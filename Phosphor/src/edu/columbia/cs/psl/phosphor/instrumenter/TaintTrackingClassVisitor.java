package edu.columbia.cs.psl.phosphor.instrumenter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.acl.Owner;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.MethodDescriptor;
import edu.columbia.cs.psl.phosphor.SelectiveInstrumentationManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.phosphor.runtime.NativeHelper;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.Tainted;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

/**
 * CV responsibilities: Add a field to classes to track each instance's taint
 * Add a method for each primitive returning method to return the taint of that
 * return Add a field to hold temporarily the return taint of each primitive
 * 
 * @author jon
 * 
 */
public class TaintTrackingClassVisitor extends ClassVisitor {
	public static boolean IS_RUNTIME_INST = true;
	public static boolean FIELDS_ONLY = false;
	public static boolean GEN_HAS_TAINTS_METHOD = false;
	public static final boolean NATIVE_BOX_UNBOX = true;
	
	static boolean DO_OPT = false;
	static {
		if (!DO_OPT && !IS_RUNTIME_INST)
			System.err.println("WARN: OPT DISABLED");
	}

	List<FieldNode> fields;
	private boolean ignoreFrames;
	public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames,
			List<FieldNode> fields) {
		super(Opcodes.ASM5, cv);
		DO_OPT = DO_OPT && !IS_RUNTIME_INST;
		this.ignoreFrames = skipFrames;
		this.fields = fields;
	}
	
	private LinkedList<MethodNode> methodsToMakeUninstWrappersAround = new LinkedList<MethodNode>();
	private LinkedList<MethodNode> methodsToAddWrappersFor = new LinkedList<MethodNode>();
	private String className;
	private boolean isNormalClass;
	private boolean isInterface;
	private boolean addTaintField = false;
	private boolean addTaintMethod;
	private boolean isAnnotation;
	private boolean isAbstractClass;
	private boolean implementsComparable;
	private boolean implementsSerializable;
	private boolean fixLdcClass;
	private boolean isEnum;
	
	@Override
	public void visit(int version, int access, String name,
			String signature, String superName, String[] interfaces) {
		addTaintField = true;
		addTaintMethod = true;
		this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
		
		if(Instrumenter.IS_KAFFE_INST
				&& name.endsWith("java/lang/VMSystem"))
			access = access | Opcodes.ACC_PUBLIC;
		else if (Instrumenter.IS_HARMONY_INST
				&& name.endsWith("java/lang/VMMemoryManager")) {
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access | Opcodes.ACC_PUBLIC;
		}

		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			isAbstractClass = true;
		}
		if ((access & Opcodes.ACC_INTERFACE) != 0) {
			addTaintField = false;
			isInterface = true;
		}
		if ((access & Opcodes.ACC_ENUM) != 0) {
			isEnum = true;
			addTaintField = false;
		}

		if ((access & Opcodes.ACC_ANNOTATION) != 0)
			isAnnotation = true;

		if (!superName.equals("java/lang/Object")
				&& !Instrumenter.isIgnoredClass(superName)) {
			addTaintField = false;
			addTaintMethod = true;
		}
		if (name.equals("java/awt/image/BufferedImage")
				|| name.equals("java/awt/image/Image"))
			addTaintField = false;
		if (addTaintField)
			addTaintMethod = true;
		if ((superName.equals("java/lang/Object") || Instrumenter.isIgnoredClass(superName))
				&& !isInterface
				&& !isAnnotation) {
			generateEquals = true;
			generateHashCode = true;
		}
		
		isNormalClass = (access & Opcodes.ACC_ENUM) == 0
			&& (access & Opcodes.ACC_INTERFACE) == 0;
		
		if ((isEnum || name.equals("java/lang/Enum"))
				&& Configuration.WITH_ENUM_BY_VAL) {

			boolean alreadyHas = false;
			for (String s : interfaces)
				if (s.equals("java/lang/Cloneable"))
					alreadyHas = true;
			if (!alreadyHas) {
				String[] newIntfcs = new String[interfaces.length + 1];
				System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
				newIntfcs[interfaces.length] = "java/lang/Cloneable";
				interfaces = newIntfcs;
				if (signature != null)
					signature = signature + "Ljava/lang/Cloneable;";
			}
		}

		if (isNormalClass
				&& !Instrumenter.isIgnoredClass(name)
				&& !FIELDS_ONLY) {
			String[] newIntfcs = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
			newIntfcs[interfaces.length] = Type.getInternalName((Configuration.MULTI_TAINTING
						? TaintedWithObjTag.class : TaintedWithIntTag.class));
			interfaces = newIntfcs;
			if (signature != null)
				signature = signature + Type.getDescriptor((Configuration.MULTI_TAINTING
							? TaintedWithObjTag.class : TaintedWithIntTag.class));
		}

		for (String s : interfaces) {
			if (s.equals(Type.getInternalName(Comparable.class)))
				implementsComparable = true;
			else if (s.equals(Type.getInternalName(Serializable.class)))
				implementsSerializable = true;
		}

		super.visit(version, access, name, signature, superName, interfaces);
		this.visitAnnotation(Type.getDescriptor(TaintInstrumented.class), false);		
		if (Instrumenter.isIgnoredClass(superName)) {
			//Might need to override stuff.
			Class c;
			try {
				c = Class.forName(superName.replace("/", "."));
				for (Method m : c.getMethods()) {
					superMethodsToOverride.put(m.getName()+Type.getMethodDescriptor(m), m);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.className = name;
	}

	boolean generateHashCode = false;
	boolean generateEquals = false;
	boolean isProxyClass = false;

	private void collectUninstrumentedInterfaceMethods(String[] interfaces) {
		String superToCheck;
		if (interfaces != null) {
			for (String itfc : interfaces) {
				superToCheck = itfc;
				try {
					ClassNode cn = Instrumenter.classes.get(superToCheck);
					if (cn != null) {
						String[] s = new String[cn.interfaces.size()];
						s = (String[]) cn.interfaces.toArray(s);
						collectUninstrumentedInterfaceMethods(s);
						continue;
					}
					Class c = Class.forName(superToCheck.replace("/", "."), false, Instrumenter.loader);
					if (Instrumenter.isIgnoredClass(superToCheck)) {
						for (Method m : c.getDeclaredMethods()) {
							if (!Modifier.isPrivate(m.getModifiers())) {
								superMethodsToOverride.put(m.getName() + Type.getMethodDescriptor(m), m);
							}
						}
					}
					Class[] in = c.getInterfaces();
					if (in != null && in.length > 0) {
						String[] s = new String[in.length];
						for (int i = 0; i < in.length; i++) {
							s[i] = Type.getInternalName(in[i]);
						}
						collectUninstrumentedInterfaceMethods(s);
					}
				} catch (Exception ex) {
					// ex.printStackTrace();
					break;
				}
			}
		}
	}

	private HashMap<String, Method> superMethodsToOverride = new HashMap<String, Method>();
	private HashMap<MethodNode, MethodNode> forMore = new HashMap<MethodNode, MethodNode>();

	@Override
	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {
		if (name.equals("hashCode") && desc.equals("()I"))
			generateHashCode = false;
		if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z"))
			generateEquals = false;

		superMethodsToOverride.remove(name + desc);
		
		if (name.equals("compareTo"))
			implementsComparable = false;
		if (name.equals("hasAnyTaints"))
			isProxyClass = true;
		
		if (name.contains(TaintUtils.METHOD_SUFFIX))
		{
		    //Some dynamic stuff might result in there being weird stuff here
		    return new MethodVisitor(Opcodes.ASM5) {
            };
		}
		if (Configuration.WITH_SELECTIVE_INST
				&& Instrumenter.isIgnoredMethodFromOurAnalysis(className, name, desc)){
//			if (TaintUtils.DEBUG_CALLS)
//				System.out.println("Skipping instrumentation for  class: " + className + " method: " + name + " desc: " + desc);
			String newName = name;
			String newDesc = desc;
			if (!name.contains("<") && 0 == (access & Opcodes.ACC_NATIVE))
				newName = name + TaintUtils.METHOD_SUFFIX_UNINST;
			else if(name.equals("<init>")) {
				newDesc = desc.substring(0,desc.indexOf(')'))
					+Type.getDescriptor(UninstrumentedTaintSentinel.class)
					+")"
					+desc.substring(desc.indexOf(')')+1);
			}
			newDesc = TaintUtils.remapMethodDescForUninst(newDesc);
			MethodVisitor mv = super.visitMethod(access, newName, newDesc, signature, exceptions);
			mv = new UninstTaintSentinalArgFixer(mv, access, newName, newDesc, desc);
			mv = new SpecialOpcodeRemovingMV(mv, ignoreFrames, className, fixLdcClass);
			MethodVisitor _mv = mv;
			NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, newDesc, mv);
			mv = new UninstrumentedReflectionHidingMV(analyzer, className);
			mv = new UninstrumentedCompatMV(access,className,name,newDesc,signature,(String[])exceptions,mv,analyzer,ignoreFrames);
			LocalVariableManager lvs = new LocalVariableManager(access, newDesc, mv, analyzer, _mv);
			((UninstrumentedCompatMV)mv).setLocalVariableSorter(lvs);
			final PrimitiveArrayAnalyzer primArrayAnalyzer = new PrimitiveArrayAnalyzer(className, access, name, desc, signature, exceptions, null);
			lvs.setPrimitiveArrayAnalyzer(primArrayAnalyzer);
			lvs.disable();
			mv = lvs;
			final MethodVisitor cmv = mv;
			MethodNode wrapper = new MethodNode(Opcodes.ASM5,
					(isInterface ? access : access & ~Opcodes.ACC_ABSTRACT),
					name, desc, signature, exceptions) {

				public void visitEnd() {
					super.visitEnd();
					this.accept(cmv);
				};
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					//determine if this is going to be uninst, and then if we need to pre-alloc for its return :/
					if(Configuration.WITH_SELECTIVE_INST && Instrumenter.isIgnoredMethod(owner, name, desc)){
						//uninst
					}
					else
					{
						Type returnType = Type.getReturnType(desc);
						Type newReturnType = TaintUtils.getContainerReturnType(returnType);
						if(newReturnType != returnType && !(returnType.getSort() == Type.ARRAY && returnType.getDimensions() > 1))
							primArrayAnalyzer.wrapperTypesToPreAlloc.add(newReturnType);
					}
					super.visitMethodInsn(opcode, owner, name, desc, itf);
				};
			};

			if(!name.equals("<clinit>"))
				methodsToMakeUninstWrappersAround.add(wrapper);
			return wrapper;
		}

		if (Configuration.WITH_ENUM_BY_VAL
				&& className.equals("java/lang/Enum")
				&& name.equals("clone"))
			return null;
		if (TaintUtils.DEBUG_CALLS
				|| TaintUtils.DEBUG_FIELDS
				|| TaintUtils.DEBUG_FRAMES
				|| TaintUtils.DEBUG_LOCAL)
			System.out.println("Instrumenting " + name + "\n\n\n\n\n\n");

		if (Instrumenter.IS_KAFFE_INST
				&& className.equals("java/lang/VMSystem"))
			access = access | Opcodes.ACC_PUBLIC;
		else if (Instrumenter.IS_HARMONY_INST
				&& className.endsWith("java/lang/VMMemoryManager")) {
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access | Opcodes.ACC_PUBLIC;
		}
		
		String originalName = name;
		if (FIELDS_ONLY) { // || isAnnotation
			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		if (originalName.contains("$$INVIVO")) {
			name = name + "_orig";
		}

		// We will need to add shadow args for each parameter that is a primitive.
		// Because that worked so well last time.
		Type[] argTypes = Type.getArgumentTypes(desc);
		LinkedList<Type> newArgTypes = new LinkedList<Type>();
		boolean isRewrittenDesc = false;
		for (Type t : argTypes) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT) {
					if (t.getDimensions() > 1) {
						newArgTypes.add(MultiDTaintedArray.getTypeForType(t));
						isRewrittenDesc = true;
						continue;
					} else {
						newArgTypes.add(Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
						isRewrittenDesc = true;
					}
				}
			} else if (t.getSort() != Type.OBJECT) {
				isRewrittenDesc = true;
				newArgTypes.add(Type.getType(Configuration.TAINT_TAG_DESC));
			}
			newArgTypes.add(t);
		}
	
		if (Configuration.IMPLICIT_TRACKING
				&& !name.equals("<clinit>")) {
			isRewrittenDesc = true;
			newArgTypes.add(Type.getType(ControlTaintTagStack.class));
		}
		
		if (isRewrittenDesc && name.equals("<init>"))
			newArgTypes.add(Type.getType(TaintSentinel.class));
		
		//If we are rewriting the return type, also add a param to pass for pre-alloc
		Type oldReturnType = Type.getReturnType(desc);
		Type newReturnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
		if ((oldReturnType.getSort() != Type.VOID
					&& oldReturnType.getSort() != Type.OBJECT
					&& oldReturnType.getSort() != Type.ARRAY)
				|| (oldReturnType.getSort() == Type.ARRAY
					&& oldReturnType.getElementType().getSort() != Type.OBJECT
					&& oldReturnType.getDimensions() == 1)) {
			newArgTypes.add(newReturnType);
		}
		Type[] newArgs = new Type[newArgTypes.size()];
		newArgs = newArgTypes.toArray(newArgs);

		boolean requiresNoChange = !isRewrittenDesc && newReturnType.equals(Type.getReturnType(desc));
		MethodNode wrapper = new MethodNode(access, name, desc, signature, exceptions);
		if (!requiresNoChange
				&& !name.equals("<clinit>")
				&& !(name.equals("<init>") && !isRewrittenDesc))
			methodsToAddWrappersFor.add(wrapper);

		String newDesc = Type.getMethodDescriptor(newReturnType, newArgs);
		//		System.out.println("olddesc " + desc + " newdesc " + newDesc);
		if ((access & Opcodes.ACC_NATIVE) == 0 && !methodIsTooBigAlready(name, desc)) {
			//not a native method
			if (!name.contains("<") && !requiresNoChange)
				name = name + TaintUtils.METHOD_SUFFIX;
			MethodVisitor mv = super.visitMethod(access, name, newDesc, signature, exceptions);
			mv = new TaintTagFieldCastMV(mv);

			MethodVisitor rootmV = mv;
			mv = new SourceSinkTaintingMV(mv, access, className, name, newDesc, desc);
			MethodVisitor optimizer;
			optimizer = mv;

			mv = new SpecialOpcodeRemovingMV(optimizer,ignoreFrames, className, fixLdcClass);

			NeverNullArgAnalyzerAdapter analyzer =
				new NeverNullArgAnalyzerAdapter(className, access, name, newDesc, mv);
			mv = new StringTaintVerifyingMV(analyzer,
					(implementsSerializable
					 || className.startsWith("java/nio/") 
					 || className.startsWith("java/io/BUfferedInputStream")
					 || className.startsWith("sun/nio")),
					analyzer); //TODO - how do we handle directbytebuffers?

			ReflectionHidingMV reflectionMasker =
				new ReflectionHidingMV(mv, className, name, analyzer);
			PrimitiveBoxingFixer boxFixer =
				new PrimitiveBoxingFixer(access, className, name, desc,
						signature, exceptions, reflectionMasker, analyzer);
			LocalVariableManager lvs;
			TaintPassingMV tmv;
			MethodVisitor nextMV;
			{
				tmv = new TaintPassingMV(boxFixer, access, className, name,
						newDesc, signature, exceptions, desc, analyzer,rootmV);
				tmv.setFields(fields);
				TaintAdapter custom = null;
				lvs = new LocalVariableManager(access, newDesc, tmv, analyzer,mv);

				nextMV = lvs;
				if(Configuration.extensionMethodVisitor != null)
				{
					try {
						custom = Configuration.extensionMethodVisitor.getConstructor(
								Integer.TYPE, String.class,
								String.class, String.class,
								String.class, String[].class,
								MethodVisitor.class, NeverNullArgAnalyzerAdapter.class)
							.newInstance(Opcodes.ASM5, className, name, desc,
									signature, exceptions, nextMV, analyzer);
						nextMV = custom;
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
				if(custom != null)
					custom.setLocalVariableSorter(lvs);

				nextMV = new ConstantValueNullTaintGenerator(className, access, name,
						newDesc, signature, exceptions, nextMV);
			}

			MethodArgReindexer mar = new MethodArgReindexer(nextMV, access, name, newDesc, desc, wrapper);
			PrimitiveArrayAnalyzer primitiveArrayFixer =
				new PrimitiveArrayAnalyzer(className, access, name, desc,
						signature, exceptions, mar);
			NeverNullArgAnalyzerAdapter preAnalyzer =
				new NeverNullArgAnalyzerAdapter(className, access, name, desc,
						primitiveArrayFixer);

			MethodVisitor mvNext = preAnalyzer;
			if (!IS_RUNTIME_INST && TaintUtils.OPT_IGNORE_EXTRA_TAINTS)
				if (Configuration.IMPLICIT_TRACKING)
					mvNext = new ImplicitUnnecessaryTaintLoadRemover(className, access, name,
							desc, signature, exceptions, preAnalyzer);
				else
					mvNext = new UnnecessaryTaintLoadRemover(className, access, name,
							desc, signature, exceptions, preAnalyzer);
			else
				mvNext = preAnalyzer;
			primitiveArrayFixer.setAnalyzer(preAnalyzer);
			boxFixer.setLocalVariableSorter(lvs);
			tmv.setArrayAnalyzer(primitiveArrayFixer);
			tmv.setLVOffset(mar.getNewArgOffset());
			tmv.setLocalVariableSorter(lvs);
			lvs.setPrimitiveArrayAnalyzer(primitiveArrayFixer); // i'm lazy. this guy will tell the LVS what return types to prealloc
			reflectionMasker.setLvs(lvs);
			
			final MethodVisitor prev = mvNext;
			MethodNode rawMethod = new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
				@Override
				public void visitEnd() {
					super.visitEnd();
					this.accept(prev);
				}
			};
			if (!isInterface && !originalName.contains("$$INVIVO"))
				this.myMethods.add(rawMethod);
			forMore.put(wrapper,rawMethod);
			return rawMethod;
		} else {
			//this is a native method. we want here to make a $taint method that will call the original one.
			final MethodVisitor prev = super.visitMethod(access, name, desc, signature, exceptions);
			MethodNode rawMethod = new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
				@Override
				public void visitEnd() {
					super.visitEnd();
					this.accept(prev);
				}
			};
			forMore.put(wrapper, rawMethod);
			return rawMethod;
		}
	}

	private boolean methodIsTooBigAlready(String name, String desc) {
		// TODO we need to implement something to detect massive constant array loads and optimize it. for now... just this :-/
		return false;
	}

	private LinkedList<FieldNode> extraFieldsToVisit = new LinkedList<FieldNode>();
	private LinkedList<FieldNode> myFields = new LinkedList<FieldNode>();
	private LinkedList<MethodNode> myMethods = new LinkedList<MethodNode>();
	boolean hasSerialUID = false;

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		Type fieldType = Type.getType(desc);
		if (TaintUtils.getShadowTaintType(desc) != null) {
			if (TaintAdapter.canRawTaintAccess(className))
				extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD,
							TaintUtils.getShadowTaintType(desc), null, null));
			else
				extraFieldsToVisit.add(new FieldNode(access, name+TaintUtils.TAINT_FIELD,
							(fieldType.getSort() == Type.ARRAY ? "[":"") + TaintAdapter.getTagType(className).getDescriptor(),
							null, null));
		} else if (!FIELDS_ONLY
				&& fieldType.getSort() == Type.ARRAY
				&& fieldType.getElementType().getSort() != Type.OBJECT
				&& fieldType.getDimensions() > 1) {
			desc = MultiDTaintedArray.getTypeForType(fieldType).getDescriptor();
		}
		if (!hasSerialUID && name.equals("serialVersionUID"))
			hasSerialUID = true;
		if((access & Opcodes.ACC_STATIC) == 0)
			myFields.add(new FieldNode(access, name, desc, signature, value));
		return super.visitField(access, name, desc, signature, value);
	}



	@Override
	public void visitEnd() {

		if((isEnum || className.equals("java/lang/Enum"))
				&& Configuration.WITH_ENUM_BY_VAL) {
			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
					"clone",
					"()Ljava/lang/Object;",
					null,
					new String[]{"java/lang/CloneNotSupportedException"});
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
					"java/lang/Object",
					"clone",
					"()Ljava/lang/Object;",
					false);

			mv.visitInsn(Opcodes.ARETURN);
			mv.visitEnd();
			mv.visitMaxs(0, 0);			
		}

		boolean goLightOnGeneratedStuff = !Instrumenter.IS_ANDROID_INST
			&& className.equals("java/lang/Byte");
//		if (isAnnotation) {
//			super.visitEnd();
//			return;
//		}
		if (!hasSerialUID && !isInterface && !goLightOnGeneratedStuff) {
			if(!Configuration.MULTI_TAINTING)
				super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
						"serialVersionUIDPHOSPHOR_TAG",
						Configuration.TAINT_TAG_DESC,
						null,
						0);
			else
				super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
						"serialVersionUIDPHOSPHOR_TAG",
						Configuration.TAINT_TAG_DESC,
						null,
						null);
		}

		//Add a field to track the instance's taint
		if (addTaintField && !goLightOnGeneratedStuff) {
			if(!Configuration.MULTI_TAINTING)
				super.visitField(Opcodes.ACC_PUBLIC,
						TaintUtils.TAINT_FIELD,
						"I", null, 0);
			else
				super.visitField(Opcodes.ACC_PUBLIC,
						TaintUtils.TAINT_FIELD,
						TaintAdapter.getTagType(className).getDescriptor(), null, null);				
//			if(GEN_HAS_TAINTS_METHOD){
//			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.HAS_TAINT_FIELD, "Z", null, 0);
//			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z", null, 0);
//			}
		}

		if(this.className.equals("java/lang/reflect/Method"))
			super.visitField(Opcodes.ACC_PUBLIC,
					TaintUtils.TAINT_FIELD + "marked",
					"Z", null, 0);

		for (FieldNode fn : extraFieldsToVisit) {
			if (className.equals("java/lang/Byte") && !fn.name.startsWith("value"))
				continue;
			if (isNormalClass) {
				fn.access = fn.access & ~Opcodes.ACC_FINAL;
				fn.access = fn.access & ~Opcodes.ACC_PRIVATE;
				fn.access = fn.access & ~Opcodes.ACC_PROTECTED;
				fn.access = fn.access | Opcodes.ACC_PUBLIC;
			}
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				if (fn.desc.equals("I"))
					super.visitField(fn.access, fn.name, fn.desc, fn.signature, 0);
				else
					super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
			} else
				super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
		}
		if(FIELDS_ONLY)
			return;
		if ((isAbstractClass || isInterface)
				&& implementsComparable
				&& !goLightOnGeneratedStuff) {
			// Need to add this to interfaces so that we can call it on the interface
			if(Configuration.IMPLICIT_TRACKING)
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"compareTo$$PHOSPHORTAGGED",
						"(Ljava/lang/Object;"+ Type.getDescriptor(ControlTaintTagStack.class)
							+ Configuration.TAINTED_INT_DESC + ")" + Configuration.TAINTED_INT_DESC,
						null, null);
			else
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"compareTo$$PHOSPHORTAGGED",
						"(Ljava/lang/Object;" + Configuration.TAINTED_INT_DESC + ")"
							+ Configuration.TAINTED_INT_DESC,
						null, null);
			if (Configuration.GENERATE_UNINST_STUBS) {
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"compareTo$$PHOSPHORUNTAGGED",
						"(Ljava/lang/Object;)I",
						null, null);
			}
		}

		if (generateEquals && !goLightOnGeneratedStuff) {
			superMethodsToOverride.remove("equals(Ljava/lang/Object;)Z");
			methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE,
						"equals", "(Ljava/lang/Object;)Z", null, null));

			MethodVisitor mv;
			mv = super.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
			mv.visitCode();
			Label start = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z",false);
			mv.visitLabel(end);
			mv.visitInsn(Opcodes.IRETURN);
			mv.visitLocalVariable("this", "L"+className+";", null, start, end, 0);
			mv.visitLocalVariable("other", "Ljava/lang/Object;", null, start, end, 1);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}
		if (generateHashCode && !goLightOnGeneratedStuff) {
			superMethodsToOverride.remove("hashCode()I");
			methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE,
						"hashCode", "()I", null, null));
			
			MethodVisitor mv;
			mv = super.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);
			mv.visitCode();
			Label start = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "hashCode", "()I",false);
			mv.visitLabel(end);
			mv.visitInsn(Opcodes.IRETURN);
			mv.visitLocalVariable("this", "L"+className+";", null, start, end, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		if (addTaintMethod) {
			if (isInterface) {
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"get" + TaintUtils.TAINT_FIELD,
						"()" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I"),
						null, null);
				if(GEN_HAS_TAINTS_METHOD)
					super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
							"hasAnyTaints", "()Z", null, null);
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"set" + TaintUtils.TAINT_FIELD,
						"(" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I") + ")V",
						null, null);
			} else {
				MethodVisitor mv;
				if (!Configuration.MULTI_TAINTING) {
					mv = super.visitMethod(Opcodes.ACC_PUBLIC,
							"get" + TaintUtils.TAINT_FIELD,
							"()" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I"),
							null, null);
					mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className,
							TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					mv.visitInsn(Opcodes.IRETURN);
					mv.visitMaxs(0, 0);
					mv.visitEnd();

					mv = super.visitMethod(Opcodes.ACC_PUBLIC,
							"set" + TaintUtils.TAINT_FIELD,
							"(" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I") + ")V",
							null, null);
					mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitVarInsn(Opcodes.ILOAD, 1);
					
					if (Configuration.MULTI_TAINTING)
						mv.visitTypeInsn(Opcodes.CHECKCAST,
								Configuration.TAINT_TAG_INTERNAL_NAME);
					mv.visitFieldInsn(Opcodes.PUTFIELD, className,
							TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					
					if (className.equals("java/lang/String")) {
						//Also overwrite the taint tag of all of the chars behind this string
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitFieldInsn(Opcodes.GETFIELD, className,
								"value" + TaintUtils.TAINT_FIELD,
								Configuration.TAINT_TAG_ARRAYDESC);
						mv.visitVarInsn(Opcodes.ILOAD, 1);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName(TaintChecker.class),
								"setTaints", "([II)V", false);
					}
					mv.visitInsn(Opcodes.RETURN);
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				} else {
					mv = super.visitMethod(Opcodes.ACC_PUBLIC,
							"get" + TaintUtils.TAINT_FIELD,
							"()" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I"),
							null, null);
					mv = new TaintTagFieldCastMV(mv);
					mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					mv.visitInsn(Opcodes.ARETURN);
					mv.visitMaxs(0, 0);
					mv.visitEnd();

					mv = super.visitMethod(Opcodes.ACC_PUBLIC,
							"set" + TaintUtils.TAINT_FIELD,
							"(" + (Configuration.MULTI_TAINTING ? "Ljava/lang/Object;" : "I") + ")V",
							null, null);
					mv = new TaintTagFieldCastMV(mv);
					mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitVarInsn(Opcodes.ALOAD, 1);
					mv.visitTypeInsn(Opcodes.CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					mv.visitFieldInsn(Opcodes.PUTFIELD, className,
							TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

					if (className.equals("java/lang/String")) {
						//Also overwrite the taint tag of all of the chars behind this string
						mv.visitVarInsn(Opcodes.ALOAD, 0);
//						mv.visitFieldInsn(Opcodes.GETFIELD, className, "value" + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
						mv.visitVarInsn(Opcodes.ALOAD, 1);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName(TaintChecker.class),
								"setTaints",
								"(Ljava/lang/String;Ljava/lang/Object;)V",
								false);
					}
					mv.visitInsn(Opcodes.RETURN);
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				}
//				if (!this.isProxyClass && GEN_HAS_TAINTS_METHOD) {
//					mv = super.visitMethod(Opcodes.ACC_PUBLIC, "hasAnyTaints", "()Z", null, null);
//					mv.visitCode();
//					Label keepGoing1 = new Label();
//					mv.visitVarInsn(Opcodes.ALOAD, 0);
//					mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.HAS_TAINT_FIELD, "Z");
//					mv.visitJumpInsn(Opcodes.IFEQ, keepGoing1);
//					mv.visitInsn(Opcodes.ICONST_1);
//					mv.visitInsn(Opcodes.IRETURN);
//					mv.visitLabel(keepGoing1);
//					//TODO if the istaitnsearchingfield is 1, then return 0.
//					mv.visitVarInsn(Opcodes.ALOAD, 0);
//					mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");
//					Label keepGoing = new Label();
//					mv.visitJumpInsn(Opcodes.IFEQ, keepGoing);
//					mv.visitInsn(Opcodes.ICONST_0);
//					mv.visitInsn(Opcodes.IRETURN);
//					mv.visitLabel(keepGoing);
//					if (myFields.size() > 0) {
//						mv.visitVarInsn(Opcodes.ALOAD, 0);
//						mv.visitInsn(Opcodes.ICONST_1);
//						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");
//
//						Label hasTaint = new Label();
//						for (FieldNode fn : myFields) {
//							Type fieldDesc = Type.getType(fn.desc);
//							if (TaintUtils.getShadowTaintType(fn.desc) != null) {
//								if (fieldDesc.getSort() == Type.ARRAY) {
//									mv.visitVarInsn(Opcodes.ALOAD, 0);
//									mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(fn.desc));
//									if (fieldDesc.getDimensions() == 1) {
//										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([I)Z",false);
//									} else if (fieldDesc.getDimensions() == 2) {
//										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([[I)Z",false);
//									} else if (fieldDesc.getDimensions() == 3) {
//										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([[[I)Z",false);
//									} else {
//										//bail and say that it has a taint i guess
//										mv.visitInsn(Opcodes.POP);
//										mv.visitInsn(Opcodes.ICONST_1);
//									}
//									mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
//								} else {
//									mv.visitVarInsn(Opcodes.ALOAD, 0);
//									mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name + TaintUtils.TAINT_FIELD, "I");
//									mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
//								}
//							} else if (!Instrumenter.isIgnoredClass(fieldDesc.getInternalName()) && GEN_HAS_TAINTS_METHOD) {
//								int op = Opcodes.INVOKEVIRTUAL;
//								if (Instrumenter.isInterface(fieldDesc.getInternalName()))
//									op = Opcodes.INVOKEINTERFACE;
//								mv.visitVarInsn(Opcodes.ALOAD, 0);
//								mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//								mv.visitMethodInsn(op, fieldDesc.getInternalName(), "hasAnyTaints", "()Z",false);
//								mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
//							} else {
//								//TODO XXX MUST FETCH THE TAINT SOMEHOW FOR IGNORED CLASSES FOR THIS TO BE SOUND
//							}
//						}
//
//						mv.visitVarInsn(Opcodes.ALOAD, 0);
//						mv.visitInsn(Opcodes.ICONST_0);
//						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");
//
//						mv.visitInsn(Opcodes.ICONST_0);
//						mv.visitInsn(Opcodes.IRETURN);
//
//						mv.visitLabel(hasTaint);
//						mv.visitVarInsn(Opcodes.ALOAD, 0);
//						mv.visitInsn(Opcodes.ICONST_0);
//						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");
//
//						mv.visitVarInsn(Opcodes.ALOAD, 0);
//						mv.visitInsn(Opcodes.ICONST_1);
//						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.HAS_TAINT_FIELD, "Z");
//						mv.visitInsn(Opcodes.ICONST_1);
//						mv.visitInsn(Opcodes.IRETURN);
//					} else {
//						mv.visitInsn(Opcodes.ICONST_0);
//						mv.visitInsn(Opcodes.IRETURN);
//					}
//					mv.visitMaxs(0, 0);
//					mv.visitEnd();
//				}
			}
		}
		

//		if(Configuration.MULTI_TAINTING)
//			generateStrLdcWrapper();
		if (!goLightOnGeneratedStuff)
			for (MethodNode m : methodsToAddWrappersFor) {
				if ((m.access & Opcodes.ACC_NATIVE) == 0) {
					if ((m.access & Opcodes.ACC_ABSTRACT) == 0) {
						//not native
						MethodNode fullMethod = forMore.get(m);

						Type origReturn = Type.getReturnType(m.desc);
						Type newReturn = TaintUtils.getContainerReturnType(origReturn);
						boolean needToPrealloc = TaintUtils.isPreAllocReturnType(m.desc);
						String[] exceptions = new String[m.exceptions.size()];
						exceptions = (String[]) m.exceptions.toArray(exceptions);
						MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
						mv = new TaintTagFieldCastMV(mv);

						if (fullMethod != null) {
							visitAnnotations(mv,fullMethod);
						}

						NeverNullArgAnalyzerAdapter an =
							new NeverNullArgAnalyzerAdapter(className, m.access, m.name, m.desc, mv);
						MethodVisitor soc = new SpecialOpcodeRemovingMV(an, false, className, false);
						LocalVariableManager lvs = new LocalVariableManager(m.access, m.desc, soc, an, mv);
						lvs.setPrimitiveArrayAnalyzer(new PrimitiveArrayAnalyzer(newReturn));
						GeneratorAdapter ga = new GeneratorAdapter(lvs, m.access, m.name, m.desc);
						Label startLabel = new Label();
						ga.visitCode();
						ga.visitLabel(startLabel);
						ga.visitLineNumber(0, startLabel);
						
						Type[] argTypes = Type.getArgumentTypes(m.desc);
						int idx = 0;
						if ((m.access & Opcodes.ACC_STATIC) == 0) {
							ga.visitVarInsn(Opcodes.ALOAD, 0);
							idx++;
						}
						
						String newDesc = "(";
						for (Type t : argTypes) {
							boolean loaded = false;
							boolean needToBoxMultiD = false;
							if (t.getSort() == Type.ARRAY) {
								if (t.getElementType().getSort() != Type.OBJECT) {
									if (t.getDimensions() == 1) {
										newDesc += Configuration.TAINT_TAG_ARRAYDESC;
										ga.visitVarInsn(Opcodes.ALOAD, idx);
										TaintAdapter.createNewTaintArray(t.getDescriptor(), an, lvs, lvs);
										loaded = true;
									} else {
										newDesc += MultiDTaintedArray.getTypeForType(t).getDescriptor();
										needToBoxMultiD = true;
									}
								}
							} else if (t.getSort() != Type.OBJECT) {
								newDesc += Configuration.TAINT_TAG_DESC;
								Configuration.taintTagFactory.generateEmptyTaint(ga);
							}
							if (!loaded)
								ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
							
							if (NATIVE_BOX_UNBOX
									&& t.getSort() == Type.OBJECT
									&& Instrumenter.isCollection(t.getInternalName())) {
								////  public final static ensureIsBoxed(Ljava/util/Collection;)Ljava/util/Collection;
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Type.getInternalName(NativeHelper.class),
										"ensureIsBoxed" + (Configuration.MULTI_TAINTING ? "ObjTags":""),
										"(Ljava/util/Collection;)Ljava/util/Collection;",
										false);
								ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
							}

							if (t.getDescriptor().endsWith("java/lang/Object;")) {
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Type.getInternalName(MultiDTaintedArray.class),
										"boxIfNecessary",
										"(Ljava/lang/Object;)Ljava/lang/Object;",
										false);
								ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
							}

							if (!needToBoxMultiD)
								newDesc += t.getDescriptor();
							else {
								Label isDone = new Label();
								ga.visitInsn(Opcodes.DUP);
								ga.visitJumpInsn(Opcodes.IFNULL, isDone);
								ga.visitIntInsn(Opcodes.BIPUSH, t.getElementType().getSort());
								ga.visitIntInsn(Opcodes.BIPUSH, t.getDimensions());
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Type.getInternalName((Configuration.MULTI_TAINTING
												? MultiDTaintedArrayWithObjTag.class
												: MultiDTaintedArrayWithIntTag.class)),
										"initWithEmptyTaints",
										"([Ljava/lang/Object;II)Ljava/lang/Object;",
										false);
								FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
								fn.stack.set(fn.stack.size() -1,"java/lang/Object");
								ga.visitLabel(isDone);
								TaintAdapter.acceptFn(fn, lvs);
								ga.visitTypeInsn(Opcodes.CHECKCAST,
										MultiDTaintedArray.getTypeForType(t).getDescriptor());
							}
							idx += t.getSize();
						}

						if (Configuration.IMPLICIT_TRACKING) {
							newDesc += Type.getDescriptor(ControlTaintTagStack.class);
							ga.visitTypeInsn(Opcodes.NEW,
									Type.getInternalName(ControlTaintTagStack.class));
							ga.visitInsn(Opcodes.DUP);
							ga.visitMethodInsn(Opcodes.INVOKESPECIAL,
									Type.getInternalName(ControlTaintTagStack.class),
									"<init>", "()V", false);
						}
						if (m.name.equals("<init>")) {
							newDesc += Type.getDescriptor(TaintSentinel.class);
							ga.visitInsn(Opcodes.ACONST_NULL);
						}
						if (needToPrealloc) {
							newDesc += newReturn.getDescriptor();
							an.visitVarInsn(Opcodes.ALOAD, lvs.getPreAllocedReturnTypeVar(newReturn));
						}
						newDesc += ")" + newReturn.getDescriptor();

						int opcode;
						if ((m.access & Opcodes.ACC_STATIC) == 0) {
								opcode = Opcodes.INVOKESPECIAL;
						} else
							opcode = Opcodes.INVOKESTATIC;
						if (m.name.equals("<init>")) {
							ga.visitMethodInsn(Opcodes.INVOKESPECIAL,
									className, m.name, newDesc,false);
						} else
							ga.visitMethodInsn(opcode,
									className, m.name + TaintUtils.METHOD_SUFFIX, newDesc, false);

						// unbox collections
						idx =0;
						if ((m.access & Opcodes.ACC_STATIC) == 0) {
							idx++;
						}

						for (Type t : argTypes) {
							if (NATIVE_BOX_UNBOX
									&& t.getSort() == Type.OBJECT
									&& Instrumenter.isCollection(t.getInternalName())) {
								////  public final static ensureIsBoxed(Ljava/util/Collection;)Ljava/util/Collection;
								ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Type.getInternalName(NativeHelper.class),
										"ensureIsUnBoxed" + (Configuration.MULTI_TAINTING ? "ObjTags" : ""),
										"(Ljava/util/Collection;)Ljava/util/Collection;",
										false);
								ga.visitInsn(Opcodes.POP);
							}
							idx += t.getSize();
						}
						if (origReturn != newReturn) {
							String taintType = TaintUtils.getShadowTaintType(origReturn.getDescriptor());
							if (taintType != null) {
								ga.visitFieldInsn(Opcodes.GETFIELD,
										newReturn.getInternalName(), "val", origReturn.getDescriptor());
							} else {
								//Need to convert from [[WrapperForCArray to [[[C
								Label isDone = new Label();
								ga.visitInsn(Opcodes.DUP);
								ga.visitJumpInsn(Opcodes.IFNULL, isDone);
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions() - 1);
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Type.getInternalName((Configuration.MULTI_TAINTING
												? MultiDTaintedArrayWithObjTag.class
												: MultiDTaintedArrayWithIntTag.class)),
										"unboxVal",
										"(Ljava/lang/Object;II)Ljava/lang/Object;",
										false);
								FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
								fn.stack.set(fn.stack.size() - 1, "java/lang/Object");
								ga.visitLabel(isDone);
								TaintAdapter.acceptFn(fn, lvs);
								ga.visitTypeInsn(Opcodes.CHECKCAST, origReturn.getInternalName());
							}
						}
						Label endLabel = new Label();
						ga.visitLabel(endLabel);
						ga.returnValue();

						for (Object o : m.localVariables) {
							LocalVariableNode n = (LocalVariableNode) o;
							ga.visitLocalVariable(n.name, n.desc, n.signature,
									startLabel, endLabel, n.index);
						}

						if (m.name.equals("<init>")) {

						}
						ga.visitMaxs(0, 0);
						ga.visitEnd();
					} else {
						String[] exceptions = new String[m.exceptions.size()];
						exceptions = (String[]) m.exceptions.toArray(exceptions);
						MethodNode fullMethod = forMore.get(m);

						MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
						if (fullMethod.annotationDefault != null) {
							AnnotationVisitor av = mv.visitAnnotationDefault();
							acceptAnnotationRaw(av, null, fullMethod.annotationDefault);
							av.visitEnd();
						}
						m.accept(mv);
					}
				} else {
					//generate wrapper for native method - a native wrapper
					generateNativeWrapper(m, m.name);
				}
			}

		superMethodsToOverride.remove("wait(JI)V");
		superMethodsToOverride.remove("wait(J)V");
		superMethodsToOverride.remove("wait()V");
		superMethodsToOverride.remove("notify()V");
		superMethodsToOverride.remove("notifyAll()V");

		for (Method m : superMethodsToOverride.values()) {
			int acc = Opcodes.ACC_PUBLIC;
			if (Modifier.isProtected(m.getModifiers()) && isInterface)
				continue;
			else if (Modifier.isPrivate(m.getModifiers()))
				continue;
			if (Modifier.isStatic(m.getModifiers()))
				acc = acc | Opcodes.ACC_STATIC;
			if(isInterface)
				acc = acc | Opcodes.ACC_ABSTRACT;
			else
				acc = acc &~Opcodes.ACC_ABSTRACT;
			MethodNode mn = new MethodNode(Opcodes.ASM5, acc, m.getName(),
					Type.getMethodDescriptor(m), null, null);

			generateNativeWrapper(mn, mn.name);

			if (Configuration.GENERATE_UNINST_STUBS) {
				MethodVisitor mv = super.visitMethod((isInterface ? mn.access : mn.access & ~Opcodes.ACC_ABSTRACT),
						mn.name + TaintUtils.METHOD_SUFFIX_UNINST,
						mn.desc,
						mn.signature,
						(String[]) mn.exceptions.toArray(new String[0]));
				GeneratorAdapter ga = new GeneratorAdapter(mv, mn.access, mn.name, mn.desc);
				visitAnnotations(mv, mn);
				if (!isInterface) {
					mv.visitCode();
					int opcode;
					if ((mn.access & Opcodes.ACC_STATIC) == 0) {
						ga.loadThis();
						opcode = Opcodes.INVOKESPECIAL;
					} else
						opcode = Opcodes.INVOKESTATIC;
					ga.loadArgs();
					ga.visitMethodInsn(opcode, className, mn.name, mn.desc, false);
					ga.returnValue();
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				}
			}
		}

		if(Configuration.WITH_SELECTIVE_INST) {
			//Make sure that there's a wrapper in place for each method
			for (MethodNode m : methodsToMakeUninstWrappersAround) {
				// these methods were previously renamed to be $$PHOSPHORUNTASGGED
				// first, make one that has a descriptor WITH taint tags, that calls into the uninst one
				generateNativeWrapper(m, m.name + TaintUtils.METHOD_SUFFIX_UNINST);
				// next, make one WITHOUT taint tags, and WITHOUT the suffix
				String mName = m.name;
				String mToCall = m.name;
				String descToCall = m.desc;
				boolean isInit = false;
				String mDesc = m.desc;
				if ((Opcodes.ACC_NATIVE & m.access) != 0) {
					mName += TaintUtils.METHOD_SUFFIX_UNINST;
					m.access = m.access & ~Opcodes.ACC_NATIVE;
					mDesc = TaintUtils.remapMethodDescForUninst(mDesc);
				} else if (m.name.equals("<init>")) {
					isInit = true;
					descToCall = mDesc.substring(0, m.desc.indexOf(')'))
						+ Type.getDescriptor(UninstrumentedTaintSentinel.class)
						+ ")"
						+ mDesc.substring(mDesc.indexOf(')') + 1);
					descToCall = TaintUtils.remapMethodDescForUninst(descToCall);
				} else {
					mToCall += TaintUtils.METHOD_SUFFIX_UNINST;
					descToCall = TaintUtils.remapMethodDescForUninst(descToCall);
				}

				MethodVisitor mv = super.visitMethod(m.access, mName, mDesc,
						m.signature, (String[]) m.exceptions.toArray(new String[0]));
				visitAnnotations(mv, m);

				if (!isInterface) {
					GeneratorAdapter ga = new GeneratorAdapter(mv, m.access, m.name, mDesc);
					mv.visitCode();
					int opcode;
					if ((m.access & Opcodes.ACC_STATIC) == 0) {
						ga.loadThis();
						opcode = Opcodes.INVOKESPECIAL;
					} else
						opcode = Opcodes.INVOKESTATIC;
					Type[] origArgs = Type.getArgumentTypes(m.desc);
					Type[] newArgs = Type.getArgumentTypes(descToCall);
					for (int i = 0 ; i < origArgs.length; i++) {
						ga.loadArg(i);
						if(origArgs[i].getSort() == Type.ARRAY
								&& origArgs[i].getElementType().getSort() != Type.OBJECT
								&& origArgs[i].getDimensions() > 1) {
							ga.visitMethodInsn(Opcodes.INVOKESTATIC,
									Type.getInternalName(MultiDTaintedArray.class),
									"boxIfNecessary",
									"(Ljava/lang/Object;)Ljava/lang/Object;",
									false);
							ga.visitTypeInsn(Opcodes.CHECKCAST, newArgs[i].getInternalName());
						}
					}

					if(isInit)
						ga.visitInsn(Opcodes.ACONST_NULL);
					Type retType = Type.getReturnType(m.desc);
					ga.visitMethodInsn(opcode, className, mToCall, descToCall, false);
					if (retType.getSort() == Type.ARRAY
							&& retType.getDimensions() > 1
							&& retType.getElementType().getSort() != Type.OBJECT) {
						ga.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName((Configuration.MULTI_TAINTING
										? MultiDTaintedArrayWithObjTag.class
										: MultiDTaintedArrayWithIntTag.class)),
								"unboxRaw",
								"(Ljava/lang/Object;)Ljava/lang/Object;",
								false);
						ga.checkCast(retType);
					}
					ga.returnValue();
					mv.visitMaxs(0, 0);
				}
				mv.visitEnd();
			}
		}

		if (Configuration.GENERATE_UNINST_STUBS) {
			// make a copy of each raw method, using the proper suffix. right now this only goes 
			// one level deep - and it will call back into instrumented versions
			for (Entry<MethodNode, MethodNode> am : forMore.entrySet()) {
			    MethodNode mn = am.getKey();
				if (mn.name.equals("<clinit>"))
					continue;
				String mName = mn.name;
				String mDesc = TaintUtils.remapMethodDescForUninst(mn.desc);
				if (mName.equals("<init>")) {
					mDesc = mDesc.substring(0,mDesc.indexOf(')'))
						+ Type.getDescriptor(UninstrumentedTaintSentinel.class)
						+ ")"
						+ mDesc.substring(mDesc.indexOf(')')+1);
				} else {
					mName += TaintUtils.METHOD_SUFFIX_UNINST;
				}

				MethodVisitor mv = super.visitMethod(mn.access & ~Opcodes.ACC_NATIVE,
						mName, mDesc, mn.signature,
						(String[]) mn.exceptions.toArray(new String[0]));
				MethodNode meth = am.getValue();

				if ((mn.access & Opcodes.ACC_NATIVE) != 0) {
					GeneratorAdapter ga = new GeneratorAdapter(mv, mn.access, mn.name, mn.desc);
					visitAnnotations(mv, mn);
					mv.visitCode();
					int opcode;
					if ((mn.access & Opcodes.ACC_STATIC) == 0) {
						ga.loadThis();
						opcode = Opcodes.INVOKESPECIAL;
					} else
						opcode = Opcodes.INVOKESTATIC;
					
					Type[] args = Type.getArgumentTypes(mn.desc);
					for (int i = 0; i < args.length; i++) {
						ga.loadArg(i);
						if(args[i].getSort() == Type.ARRAY
								&& args[i].getDimensions() > 1
								&& args[i].getElementType().getSort() != Type.OBJECT) {
							ga.visitMethodInsn(Opcodes.INVOKESTATIC,
									Type.getInternalName((Configuration.MULTI_TAINTING
											? MultiDTaintedArrayWithObjTag.class
											: MultiDTaintedArrayWithIntTag.class)),
									"unboxRaw",
									"(Ljava/lang/Object;)Ljava/lang/Object;",
									false);
							ga.visitTypeInsn(Opcodes.CHECKCAST, args[i].getInternalName());
						}
					}

					ga.visitMethodInsn(opcode, className, mn.name, mn.desc, false);
					ga.returnValue();
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				} else {
					mv = new SpecialOpcodeRemovingMV(mv, ignoreFrames, className, fixLdcClass);
					NeverNullArgAnalyzerAdapter analyzer =
						new NeverNullArgAnalyzerAdapter(className, mn.access, mn.name, mDesc, mv);
					mv = analyzer;
					mv = new UninstrumentedReflectionHidingMV(mv, className);
					UninstrumentedReflectionHidingMV ta = (UninstrumentedReflectionHidingMV) mv;
					mv = new UninstrumentedCompatMV(mn.access, className, mn.name,
							mn.desc, mn.signature,
							(String[]) mn.exceptions.toArray(new String[0]),mv,analyzer,ignoreFrames);
					LocalVariableManager lvs = new LocalVariableManager(mn.access, mn.desc, mv, analyzer, analyzer);
					final PrimitiveArrayAnalyzer primArrayAnalyzer =
						new PrimitiveArrayAnalyzer(className, mn.access, mn.name, mn.desc, null, null, null);
					lvs.disable();
					lvs.setPrimitiveArrayAnalyzer(primArrayAnalyzer);
					((UninstrumentedCompatMV)mv).setLocalVariableSorter(lvs);
					ta.setLvs(lvs);
					mv = lvs;
					meth.accept(new MethodVisitor(Opcodes.ASM5) {
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
							//determine if this is going to be uninst, and then if we need to pre-alloc for its return :/
							if(Configuration.WITH_SELECTIVE_INST
								&& Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc)){
								//uninst
							} else {
								Type returnType = Type.getReturnType(desc);
								Type newReturnType = TaintUtils.getContainerReturnType(returnType);
								if (newReturnType != returnType
									&& !(returnType.getSort() == Type.ARRAY
										&& returnType.getDimensions() > 1))
									primArrayAnalyzer.wrapperTypesToPreAlloc.add(newReturnType);
							}
						};
					});
					meth.accept(mv);
				}
			}
		}
		super.visitEnd();
	}

	private void visitAnnotations(MethodVisitor mv, MethodNode fullMethod) {
		if (fullMethod.annotationDefault != null) {
			AnnotationVisitor av = mv.visitAnnotationDefault();
			acceptAnnotationRaw(av, null, fullMethod.annotationDefault);
			av.visitEnd();
		}
		if (fullMethod.visibleAnnotations != null)
			for (Object o : fullMethod.visibleAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, true));
			}
		if (fullMethod.invisibleAnnotations != null)
			for (Object o : fullMethod.invisibleAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, false));
			}
		if (fullMethod.visibleLocalVariableAnnotations != null)
			for (Object o : fullMethod.visibleLocalVariableAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, true));
			}
		if (fullMethod.invisibleLocalVariableAnnotations != null)
			for (Object o : fullMethod.invisibleLocalVariableAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, false));
			}
		if (fullMethod.visibleTypeAnnotations != null)
			for (Object o : fullMethod.visibleTypeAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, true));
			}
		if (fullMethod.invisibleTypeAnnotations != null)
			for (Object o : fullMethod.invisibleTypeAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(mv.visitAnnotation(an.desc, false));
			}
		if (fullMethod.parameters != null)
			for (Object o : fullMethod.parameters) {
				ParameterNode pn = (ParameterNode) o;
				pn.accept(mv);
			}
		if (fullMethod.visibleParameterAnnotations != null)
			for (int i = 0; i < fullMethod.visibleParameterAnnotations.length; i++)
				if (fullMethod.visibleParameterAnnotations[i] != null)
					for (Object o : fullMethod.visibleParameterAnnotations[i]) {
						AnnotationNode an = (AnnotationNode) o;
						an.accept(mv.visitParameterAnnotation(i, an.desc, true));
					}
		if (fullMethod.invisibleParameterAnnotations != null)
			for (int i = 0; i < fullMethod.invisibleParameterAnnotations.length; i++)
				if (fullMethod.invisibleParameterAnnotations[i] != null)
					for (Object o : fullMethod.invisibleParameterAnnotations[i]) {
						AnnotationNode an = (AnnotationNode) o;
						an.accept(mv.visitParameterAnnotation(i, an.desc, false));
					}
	}

	static void acceptAnnotationRaw(final AnnotationVisitor av, final String name,
            final Object value) {
        if (av != null) {
            if (value instanceof String[]) {
                String[] typeconst = (String[]) value;
                av.visitEnum(name, typeconst[0], typeconst[1]);
            } else if (value instanceof AnnotationNode) {
                AnnotationNode an = (AnnotationNode) value;
                an.accept(av.visitAnnotation(name, an.desc));
            } else if (value instanceof List) {
                AnnotationVisitor v = av.visitArray(name);
                List<?> array = (List<?>) value;
                for (int j = 0; j < array.size(); ++j) {
                    acceptAnnotationRaw(v, null, array.get(j));
                }
                v.visitEnd();
            } else {
                av.visit(name, value);
            }
        }
    }

	private void generateNativeWrapper(MethodNode m, String methodNameToCall) {
		String[] exceptions = new String[m.exceptions.size()];
		exceptions = (String[]) m.exceptions.toArray(exceptions);
		Type[] argTypes = Type.getArgumentTypes(m.desc);

		boolean isPreAllocReturnType = TaintUtils.isPreAllocReturnType(m.desc);
		String newDesc = "(";
		LinkedList<LocalVariableNode> lvsToVisit = new LinkedList<LocalVariableNode>();
		LabelNode start = new LabelNode(new Label());
		LabelNode end = new LabelNode(new Label());
		for (Type t : argTypes) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT
						&& t.getDimensions() == 1) {
					newDesc += TaintUtils.getShadowTaintType(t.getDescriptor());
				}
			} else if (t.getSort() != Type.OBJECT) {
				newDesc += Configuration.TAINT_TAG_DESC;
			}
			if (t.getSort() == Type.ARRAY
					&& t.getElementType().getSort() != Type.OBJECT
					&& t.getDimensions() > 1)
				newDesc += MultiDTaintedArray.getTypeForType(t).getDescriptor();
			else
				newDesc += t.getDescriptor();
		}

		Type origReturn = Type.getReturnType(m.desc);
		Type newReturn = TaintUtils.getContainerReturnType(origReturn);
		if (Configuration.IMPLICIT_TRACKING)
			newDesc += Type.getDescriptor(ControlTaintTagStack.class);
		if (m.name.equals("<init>"))
			newDesc += Type.getDescriptor(TaintSentinel.class);
		if (isPreAllocReturnType)
			newDesc += newReturn.getDescriptor();
		newDesc += ")" + newReturn.getDescriptor();

		MethodVisitor mv;
		if (m.name.equals("<init>"))
			mv = super.visitMethod(m.access & ~Opcodes.ACC_NATIVE,
					m.name,
					newDesc, m.signature, exceptions);
		else
			mv = super.visitMethod(m.access & ~Opcodes.ACC_NATIVE,
					m.name + TaintUtils.METHOD_SUFFIX,
					newDesc, m.signature, exceptions);

		NeverNullArgAnalyzerAdapter an =
			new NeverNullArgAnalyzerAdapter(className, m.access, m.name, newDesc, mv);
		MethodVisitor soc = new SpecialOpcodeRemovingMV(an, false, className, false);
		LocalVariableManager lvs = new LocalVariableManager(m.access, newDesc, soc, an, mv);
		lvs.setPrimitiveArrayAnalyzer(new PrimitiveArrayAnalyzer(newReturn));
		GeneratorAdapter ga = new GeneratorAdapter(lvs, m.access,
				m.name + TaintUtils.METHOD_SUFFIX, newDesc);
		if (isInterface) {
			ga.visitEnd();
			return;
		}

		ga.visitCode();
		ga.visitLabel(start.getLabel());
		String descToCall = m.desc;
		boolean isUntaggedCall = false;
		if (methodNameToCall.contains("$$PHOSPHORUNTAGGED")) {
			descToCall = TaintUtils.remapMethodDescForUninst(descToCall);
			isUntaggedCall = true;
		}

		int idx = 0;
		if ((m.access & Opcodes.ACC_STATIC) == 0) {
			ga.visitVarInsn(Opcodes.ALOAD, 0);
			lvsToVisit.add(new LocalVariableNode("this",
						"L"+className+";", null, start, end, idx));
			idx++;
		}
		for (Type t : argTypes) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT
						&& t.getDimensions() == 1) {
					lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx,
								Configuration.TAINT_TAG_ARRAYDESC, null, start, end, idx));
					idx++;
				}
			} else if (t.getSort() != Type.OBJECT) {
				lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx,
							Configuration.TAINT_TAG_DESC, null, start, end, idx));
				idx++;
			}
			ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);

			lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx,
						t.getDescriptor(), null, start, end, idx));
			if (t.getDescriptor().equals("Ljava/lang/Object;")
					|| (t.getSort() == Type.ARRAY
						&& t.getElementType().getDescriptor().equals("Ljava/lang/Object;"))) {
				// Need to make sure that it's not a boxed primitive array
				ga.visitInsn(Opcodes.DUP);
				ga.visitInsn(Opcodes.DUP);
				Label isOK = new Label();
				ga.visitTypeInsn(Opcodes.INSTANCEOF,
						"[" + Type.getDescriptor((!Configuration.MULTI_TAINTING
								? MultiDTaintedArrayWithIntTag.class
								: MultiDTaintedArrayWithObjTag.class)));
				ga.visitInsn(Opcodes.SWAP);
				ga.visitTypeInsn(Opcodes.INSTANCEOF,
						Type.getInternalName((!Configuration.MULTI_TAINTING
								? MultiDTaintedArrayWithIntTag.class
								: MultiDTaintedArrayWithObjTag.class)));
				ga.visitInsn(Opcodes.IOR);
				ga.visitJumpInsn(Opcodes.IFEQ, isOK);
				if (isUntaggedCall)
					ga.visitMethodInsn(Opcodes.INVOKESTATIC,
							Type.getInternalName(MultiDTaintedArray.class),
							"unbox1D",
							"(Ljava/lang/Object;)Ljava/lang/Object;",
							false);
				else
					ga.visitMethodInsn(Opcodes.INVOKESTATIC,
							Type.getInternalName((Configuration.MULTI_TAINTING
									? MultiDTaintedArrayWithObjTag.class
									: MultiDTaintedArrayWithIntTag.class)),
							"unboxRaw",
							"(Ljava/lang/Object;)Ljava/lang/Object;",
							false);
				if(t.getSort() == Type.ARRAY)
					ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
				FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
				ga.visitLabel(isOK);
				TaintAdapter.acceptFn(fn, lvs);
			} else if (!isUntaggedCall
					&& t.getSort() == Type.ARRAY
					&& t.getDimensions() > 1
					&& t.getElementType().getSort() != Type.OBJECT) {
				//Need to unbox it!!
				ga.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName((Configuration.MULTI_TAINTING
								? MultiDTaintedArrayWithObjTag.class
								: MultiDTaintedArrayWithIntTag.class)),
						"unboxRaw",
						"(Ljava/lang/Object;)Ljava/lang/Object;",
						false);
				ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			}
			idx += t.getSize();
		}

		int opcode;
		if ((m.access & Opcodes.ACC_STATIC) == 0) {
			opcode = Opcodes.INVOKESPECIAL;
		} else
			opcode = Opcodes.INVOKESTATIC;

		if (m.name.equals("<init>")
				&& methodNameToCall.contains("$$PHOSPHORUNTAGGED")) {
			// call with uninst sentinel
			descToCall = descToCall.substring(0, descToCall.indexOf(')'))
				+ Type.getDescriptor(UninstrumentedTaintSentinel.class)
				+ ")"
				+ descToCall.substring(descToCall.indexOf(')') + 1);
			ga.visitInsn(Opcodes.ACONST_NULL);
			ga.visitMethodInsn(opcode, className, m.name, descToCall,false);
		} else
			ga.visitMethodInsn(opcode, className, methodNameToCall, descToCall,false);
		
		if (origReturn != newReturn) {
			if (origReturn.getSort() == Type.ARRAY) {
				if (origReturn.getDimensions() > 1) {
					//							System.out.println(an.stack + " > " + newReturn);
					Label isOK = new Label();
					ga.visitInsn(Opcodes.DUP);
					ga.visitJumpInsn(Opcodes.IFNULL, isOK);
					ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
					ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
					ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions());
					ga.visitMethodInsn(Opcodes.INVOKESTATIC,
							Type.getInternalName((Configuration.MULTI_TAINTING
									? MultiDTaintedArrayWithObjTag.class
									: MultiDTaintedArrayWithIntTag.class)),
							"initWithEmptyTaints",
							"([Ljava/lang/Object;II)Ljava/lang/Object;",
							false);
					FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
					fn.stack.set(fn.stack.size() - 1, "java/lang/Object");
					ga.visitLabel(isOK);
					TaintAdapter.acceptFn(fn, lvs);
					ga.visitTypeInsn(Opcodes.CHECKCAST, newReturn.getDescriptor());
				} else {
					TaintAdapter.createNewTaintArray(origReturn.getDescriptor(), an, lvs, lvs);
					int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					ga.visitInsn(Opcodes.SWAP);
					ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(),
							"val", origReturn.getDescriptor());
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					ga.visitInsn(Opcodes.SWAP);
					if (!Configuration.MULTI_TAINTING)
						ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(),
								"taint", "[I");
					else
						ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(),
								"taint", "[Ljava/lang/Object;");
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
				}
			} else {
				//TODO here's where we store to the pre-alloc'ed container
				if (origReturn.getSize() == 1) {
					int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					ga.visitInsn(Opcodes.SWAP);
					ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(),
							"val", origReturn.getDescriptor());
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					Configuration.taintTagFactory.generateEmptyTaint(ga);
					idx = 0;
					if ((m.access & Opcodes.ACC_STATIC) == 0) {
						idx++;
					}

					for (Type t : argTypes) {
						if (t.getSort() == Type.ARRAY) {
							if (t.getElementType().getSort() != Type.OBJECT
									&& t.getDimensions() == 1) {
								idx++;
							}
						} else if (t.getSort() != Type.OBJECT) {
							ga.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
							if (Configuration.MULTI_TAINTING) {
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Configuration.MULTI_TAINT_HANDLER_CLASS,
										"combineTags",
										"(" + Configuration.TAINT_TAG_DESC
											+ Configuration.TAINT_TAG_DESC
											+ ")"
											+ Configuration.TAINT_TAG_DESC,
										false);
							} else {
								ga.visitInsn(Opcodes.IOR);
							}
							idx++;
						}
						idx += t.getSize();
					}

					if (!Configuration.MULTI_TAINTING)
						ga.visitFieldInsn(Opcodes.PUTFIELD,
								newReturn.getInternalName(), "taint", "I");
					else
						ga.visitFieldInsn(Opcodes.PUTFIELD,
								newReturn.getInternalName(), "taint", "Ljava/lang/Object;");
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
				} else {
					int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					ga.visitInsn(Opcodes.DUP_X2);
					ga.visitInsn(Opcodes.POP);
					ga.visitFieldInsn(Opcodes.PUTFIELD,
							newReturn.getInternalName(), "val", origReturn.getDescriptor());
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
					Configuration.taintTagFactory.generateEmptyTaint(ga);
					idx = 0;
					if ((m.access & Opcodes.ACC_STATIC) == 0) {
						idx++;
					}

					// IOR all of the primitive args in too
					for (Type t : argTypes) {
						if (t.getSort() == Type.ARRAY) {
							if (t.getElementType().getSort() != Type.OBJECT
									&& t.getDimensions() == 1) {
								idx++;
							}
						} else if (t.getSort() != Type.OBJECT) {
							ga.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
							if (Configuration.MULTI_TAINTING) {
								ga.visitMethodInsn(Opcodes.INVOKESTATIC,
										Configuration.MULTI_TAINT_HANDLER_CLASS,
										"combineTags",
										"(" + Configuration.TAINT_TAG_DESC
											+ Configuration.TAINT_TAG_DESC
											+ ")"
											+ Configuration.TAINT_TAG_DESC,
										false);
							} else {
								ga.visitInsn(Opcodes.IOR);
							}
							idx++;
						}
						idx += t.getSize();
					}

					if (!Configuration.MULTI_TAINTING)
						ga.visitFieldInsn(Opcodes.PUTFIELD,
								newReturn.getInternalName(), "taint", "I");
					else
						ga.visitFieldInsn(Opcodes.PUTFIELD,
								newReturn.getInternalName(), "taint", "Ljava/lang/Object;");
					an.visitVarInsn(Opcodes.ALOAD, retIdx);
				}
			}
		} else if (origReturn.getSort() != Type.VOID
				&& (origReturn.getDescriptor().equals("Ljava/lang/Object;")
					|| origReturn.getDescriptor().equals("[Ljava/lang/Object;"))) {
			// Check to see if the top of the stack is a primitive array, adn if so, box it.
			if (!isUntaggedCall) {
				ga.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName((Configuration.MULTI_TAINTING
								? MultiDTaintedArrayWithObjTag.class
								: MultiDTaintedArrayWithIntTag.class)),
						"boxIfNecessary",
						"(Ljava/lang/Object;)Ljava/lang/Object;",
						false);
				if (origReturn.getSort() == Type.ARRAY)
					ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
			}
		}
		ga.visitLabel(end.getLabel());
		ga.returnValue();
		if (isPreAllocReturnType) {
			lvsToVisit.add(new LocalVariableNode("phosphorReturnHolder",
						newReturn.getDescriptor(), null, start, end,
						lvs.getPreAllocedReturnTypeVar(newReturn)));
		}
		for(LocalVariableNode n : lvsToVisit)
			n.accept(ga);		
		ga.visitMaxs(0, 0);
		ga.visitEnd();
	}

	private void generateStrLdcWrapper() {
		if (!isNormalClass)
			return;
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
				TaintUtils.STR_LDC_WRAPPER,
				"(Ljava/lang/String;)Ljava/lang/String;",
				null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0); //S
		mv.visitInsn(Opcodes.DUP); //S S
		mv.visitInsn(Opcodes.DUP2);
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				"java/lang/String", "length", "()I", false);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IADD);
		if(!Configuration.MULTI_TAINTING)
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
		else
			mv.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
		mv.visitFieldInsn(Opcodes.PUTFIELD, "java/lang/String",
				"value" + TaintUtils.TAINT_FIELD,
				Configuration.TAINT_TAG_ARRAYDESC);
		mv.visitInsn(Opcodes.POP);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
