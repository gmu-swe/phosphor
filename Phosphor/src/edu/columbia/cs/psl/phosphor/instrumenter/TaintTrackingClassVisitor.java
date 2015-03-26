package edu.columbia.cs.psl.phosphor.instrumenter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.FieldVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.GeneratorAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.AnnotationNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FieldNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LabelNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LocalVariableNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.runtime.NativeHelper;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.Tainted;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

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
	
	private boolean ignoreFrames;
	public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames) {
		super(Opcodes.ASM5, cv);
		DO_OPT = DO_OPT && !IS_RUNTIME_INST;
		this.ignoreFrames = skipFrames;
	}
	
	public boolean isIgnoreFrames() {
		return ignoreFrames;
	}

	private LinkedList<MethodNode> methodsToAddWrappersFor = new LinkedList<MethodNode>();
	private String className;
	private boolean isNormalClass;
	private boolean isInterface;
	private boolean addTaintMethod;
	private boolean isAnnotation;

	private boolean isAbstractClass;

	private boolean implementsComparable;

	private boolean implementsSerializable;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		addTaintField = true;
		addTaintMethod = true;
		if(Instrumenter.IS_KAFFE_INST && name.endsWith("java/lang/VMSystem"))
			access = access | Opcodes.ACC_PUBLIC;
		else if(Instrumenter.IS_HARMONY_INST && name.endsWith("java/lang/VMMemoryManager"))
		{
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
		if ((access & Opcodes.ACC_ENUM) != 0)
			addTaintField = false;

		if ((access & Opcodes.ACC_ANNOTATION) != 0)
			isAnnotation = true;

		if (!superName.equals("java/lang/Object") && !Instrumenter.isIgnoredClass(superName)) {
			addTaintField = false;
			addTaintMethod = true;
		}
		if (name.equals("java/awt/image/BufferedImage") || name.equals("java/awt/image/Image"))
			addTaintField = false;
		if (addTaintField)
			addTaintMethod = true;
		if (superName.equals("java/lang/Object") && !isInterface && !isAnnotation) {
			generateEquals = true;
			generateHashCode = true;
		}
		isNormalClass = (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_INTERFACE) == 0;

		if (isNormalClass && !Instrumenter.isIgnoredClass(name) && !FIELDS_ONLY) {
			String[] newIntfcs = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
			newIntfcs[interfaces.length] = Type.getInternalName(Tainted.class);
			interfaces = newIntfcs;
			if (signature != null)
				signature = signature + Type.getDescriptor(Tainted.class);
		}
		this.visitAnnotation(Type.getDescriptor(TaintInstrumented.class), false);

		//		System.out.println("V " + version);
		for (String s : interfaces) {
			if (s.equals(Type.getInternalName(Comparable.class)))
				implementsComparable = true;
			else if (s.equals(Type.getInternalName(Serializable.class)))
				implementsSerializable = true;
		}
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}

	boolean generateHashCode = false;
	boolean generateEquals = false;
	boolean isProxyClass = false;

	HashMap<MethodNode, MethodNode> forMore = new HashMap<MethodNode, MethodNode>();
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (TaintUtils.DEBUG_CALLS || TaintUtils.DEBUG_FIELDS || TaintUtils.DEBUG_FRAMES || TaintUtils.DEBUG_LOCAL)
			System.out.println("Instrumenting " + name + "\n\n\n\n\n\n");
		if(Instrumenter.IS_KAFFE_INST && className.equals("java/lang/VMSystem"))
			access = access | Opcodes.ACC_PUBLIC;
		else if(Instrumenter.IS_HARMONY_INST && className.endsWith("java/lang/VMMemoryManager"))
		{
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access | Opcodes.ACC_PUBLIC;
		}
		if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z"))
			generateEquals = false;
		if (name.equals("hashCode") && desc.equals("()I"))
			generateHashCode = false;

		String originalName = name;
		if (isAnnotation || FIELDS_ONLY) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		if (originalName.contains("$$INVIVO")) {
			name = name + "_orig";
		}
		if (name.equals("compareTo"))
			implementsComparable = false;

		if (name.equals("hasAnyTaints"))
			isProxyClass = true;

		//We will need to add shadow args for each parameter that is a primitive. Because that worked so well last time.
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
						newArgTypes.add(Type.getType(t.getDescriptor().substring(0, t.getDescriptor().length() - 1) + "I"));
						isRewrittenDesc = true;
					}
				}
			} else if (t.getSort() != Type.OBJECT) {
				isRewrittenDesc = true;
				newArgTypes.add(Type.INT_TYPE);
			}
			newArgTypes.add(t);
		}
		if (isRewrittenDesc && name.equals("<init>"))
			newArgTypes.add(Type.getType(TaintSentinel.class));
		//If we are rewriting the return type, also add a param to pass for pre-alloc
		Type oldReturnType = Type.getReturnType(desc);
		Type newReturnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
		if((oldReturnType.getSort() != Type.VOID && oldReturnType.getSort() != Type.OBJECT && oldReturnType.getSort() != Type.ARRAY) || (oldReturnType.getSort() == Type.ARRAY  && oldReturnType.getElementType().getSort() != Type.OBJECT && oldReturnType.getDimensions() == 1))
		{
			newArgTypes.add(newReturnType);
		}
		Type[] newArgs = new Type[newArgTypes.size()];
		newArgs = newArgTypes.toArray(newArgs);

		boolean requiresNoChange = !isRewrittenDesc && newReturnType.equals(Type.getReturnType(desc));
		MethodNode wrapper = new MethodNode(access, name, desc, signature, exceptions);
		if (!requiresNoChange && !name.equals("<clinit>") && !(name.equals("<init>") && !isRewrittenDesc))
			methodsToAddWrappersFor.add(wrapper);

		String newDesc = Type.getMethodDescriptor(newReturnType, newArgs);

		//		System.out.println("olddesc " + desc + " newdesc " + newDesc);
		if ((access & Opcodes.ACC_NATIVE) == 0 && !methodIsTooBigAlready(name, desc)) {
			//not a native method
			if (!name.contains("<") && !requiresNoChange)
				name = name + TaintUtils.METHOD_SUFFIX;
//			if(className.equals("sun/misc/URLClassPath$JarLoader"))
//				System.out.println("\t\t:"+name+newDesc);
			MethodVisitor mv = super.visitMethod(access, name, newDesc, signature, exceptions);
			mv = new SourceSinkTaintingMV(mv, access, className, name, newDesc, desc);
			//			mv = new CheckMethodAdapter(mv);
			mv = new SpecialOpcodeRemovingMV(mv,ignoreFrames, className);

//			mv = reflectionMasker;
			//			PropertyDebug debug = new PropertyDebug(Opcodes.ASM4, mv, access, name, newDesc,className);
			MethodVisitor optimizer;
			optimizer = mv;
//			if (DO_OPT)
//				optimizer = new PopOptimizingMV(mv, access, className, name, newDesc, signature, exceptions);
			mv = new SpecialOpcodeRemovingMV(optimizer,ignoreFrames, className);
//			optimizer = new PopOptimizingMV(mv, access,className, name, newDesc, signature, exceptions);

			NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, newDesc, mv);
			mv = new StringTaintVerifyingMV(analyzer,(implementsSerializable || className.startsWith("java/nio/") || className.startsWith("java/io/BUfferedInputStream") || className.startsWith("sun/nio")),analyzer); //TODO - how do we handle directbytebuffers?

			ReflectionHidingMV reflectionMasker = new ReflectionHidingMV(mv, className,analyzer);

			PrimitiveBoxingFixer boxFixer = new PrimitiveBoxingFixer(Opcodes.ASM5, className, reflectionMasker, analyzer);
			LocalVariableManager lvs;
			TaintPassingMV tmv;
			MethodVisitor nextMV;
			{
				ImplicitTaintRemoverMV implicitCleanup = new ImplicitTaintRemoverMV(Opcodes.ASM5, className, boxFixer, analyzer);
				tmv = new TaintPassingMV(implicitCleanup, access, className, name, newDesc, desc, analyzer);
				lvs = new LocalVariableManager(access, newDesc, tmv, analyzer,mv);
				nextMV = new ConstantValueNullTaintGenerator(className, access, name, newDesc, signature, exceptions, lvs);
			}

			MethodArgReindexer mar = new MethodArgReindexer(nextMV, access, name, newDesc, desc, wrapper);
			PrimitiveArrayAnalyzer primitiveArrayFixer = new PrimitiveArrayAnalyzer(className, access, name, desc, signature, exceptions, mar);
			NeverNullArgAnalyzerAdapter preAnalyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc, primitiveArrayFixer);

			MethodVisitor mvNext;
			if (!IS_RUNTIME_INST && TaintUtils.OPT_IGNORE_EXTRA_TAINTS)
				mvNext = new UnnecessaryTaintLoadRemover(className, access, name, desc, signature, exceptions, preAnalyzer);
			else
				mvNext = preAnalyzer;
			primitiveArrayFixer.setAnalyzer(preAnalyzer);
			boxFixer.setLocalVariableSorter(lvs);
			tmv.setArrayAnalyzer(primitiveArrayFixer);
			tmv.setLVOffset(mar.getNewArgOffset());
			tmv.setLocalVariableSorter(lvs);
			lvs.setPrimitiveArrayAnalyzer(primitiveArrayFixer); // i'm lazy. this guy will tell the LVS what return types to prealloc
			reflectionMasker.setLvs(lvs);
			
			//			if(IS_RUNTIME_INST)
			//			{
			//				return mvNext;
			//			}
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
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return mv;
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
			extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(desc), null, null));
		} else if (!FIELDS_ONLY && fieldType.getSort() == Type.ARRAY && fieldType.getElementType().getSort() != Type.OBJECT && fieldType.getDimensions() > 1) {
			desc = MultiDTaintedArray.getTypeForType(fieldType).getDescriptor();
		}
		if (!hasSerialUID && name.equals("serialVersionUID"))
			hasSerialUID = true;
		if((access & Opcodes.ACC_STATIC) == 0)
			myFields.add(new FieldNode(access, name, desc, signature, value));
		return super.visitField(access, name, desc, signature, value);
	}

	boolean addTaintField = false;

	@Override
	public void visitEnd() {

		boolean goLightOnGeneratedStuff = !Instrumenter.IS_ANDROID_INST && className.equals("java/lang/Byte");
		if (isAnnotation) {
			super.visitEnd();
			return;
		}
		if (!hasSerialUID && !isInterface && !goLightOnGeneratedStuff) {
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "serialVersionUIDINVIVO_PC_TAINT", "I", null, 0);
		}
		//Add a field to track the instance's taint
		if (addTaintField && !goLightOnGeneratedStuff) {
			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD, "I", null, 0);
			if(GEN_HAS_TAINTS_METHOD){
			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.HAS_TAINT_FIELD, "Z", null, 0);
			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z", null, 0);
			}
		}
		if(this.className.equals("java/lang/reflect/Method"))
			super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD+"marked", "Z", null, false);
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
		if ((isAbstractClass || isInterface) && implementsComparable && !goLightOnGeneratedStuff) {
			//Need to add this to interfaces so that we can call it on the interface
			super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "compareTo$$INVIVO_PC", "(Ljava/lang/Object;"+Type.getDescriptor(TaintedInt.class)+")" + Type.getDescriptor(TaintedInt.class), null, null);
		}

		if (generateEquals && !goLightOnGeneratedStuff) {
			methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "equals", "(Ljava/lang/Object;)Z", null, null));
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
			mv.visitMaxs(0, 0);
			mv.visitEnd();
			mv.visitLocalVariable("this", "L"+className+";", null, start, end, 0);
			mv.visitLocalVariable("other", "Ljava/lang/Object;", null, start, end, 1);
		}
		if (generateHashCode && !goLightOnGeneratedStuff) {
			methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "hashCode", "()I", null, null));
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
			mv.visitMaxs(0, 0);
			mv.visitEnd();
			mv.visitLocalVariable("this", "L"+className+";", null, start, end, 0);

		}
		if (addTaintMethod) {
			if (isInterface) {
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "get" + TaintUtils.TAINT_FIELD, "()I", null, null);
				if(GEN_HAS_TAINTS_METHOD)
					super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "hasAnyTaints", "()Z", null, null);
				super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "set" + TaintUtils.TAINT_FIELD, "(I)V", null, null);
			} else {
				MethodVisitor mv;
				mv = super.visitMethod(Opcodes.ACC_PUBLIC, "get" + TaintUtils.TAINT_FIELD, "()I", null, null);
				mv.visitCode();
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.TAINT_FIELD, "I");
				mv.visitInsn(Opcodes.IRETURN);
				mv.visitMaxs(0, 0);
				mv.visitEnd();

				mv = super.visitMethod(Opcodes.ACC_PUBLIC, "set" + TaintUtils.TAINT_FIELD, "(I)V", null, null);
				mv.visitCode();
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ILOAD, 1);
				mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.TAINT_FIELD, "I");
				if(className.equals("java/lang/String"))
				{
					//Also overwrite the taint tag of all of the chars behind this string
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className, "value"+TaintUtils.TAINT_FIELD, "[I");
					mv.visitVarInsn(Opcodes.ILOAD, 1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "([II)V", false);
				}
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(0, 0);
				mv.visitEnd();

				if (!this.isProxyClass && GEN_HAS_TAINTS_METHOD) {
					mv = super.visitMethod(Opcodes.ACC_PUBLIC, "hasAnyTaints", "()Z", null, null);
					mv.visitCode();
					Label keepGoing1 = new Label();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.HAS_TAINT_FIELD, "Z");
					mv.visitJumpInsn(Opcodes.IFEQ, keepGoing1);
					mv.visitInsn(Opcodes.ICONST_1);
					mv.visitInsn(Opcodes.IRETURN);
					mv.visitLabel(keepGoing1);
					//TODO if the istaitnsearchingfield is 1, then return 0.
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");
					Label keepGoing = new Label();
					mv.visitJumpInsn(Opcodes.IFEQ, keepGoing);
					mv.visitInsn(Opcodes.ICONST_0);
					mv.visitInsn(Opcodes.IRETURN);
					mv.visitLabel(keepGoing);
					if (myFields.size() > 0) {
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitInsn(Opcodes.ICONST_1);
						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");

						Label hasTaint = new Label();
						for (FieldNode fn : myFields) {
							Type fieldDesc = Type.getType(fn.desc);
							if (TaintUtils.getShadowTaintType(fn.desc) != null) {
								if (fieldDesc.getSort() == Type.ARRAY) {
									mv.visitVarInsn(Opcodes.ALOAD, 0);
									mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(fn.desc));
									if (fieldDesc.getDimensions() == 1) {
										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([I)Z",false);
									} else if (fieldDesc.getDimensions() == 2) {
										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([[I)Z",false);
									} else if (fieldDesc.getDimensions() == 3) {
										mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "arrayHasTaints", "([[[I)Z",false);
									} else {
										//bail and say that it has a taint i guess
										mv.visitInsn(Opcodes.POP);
										mv.visitInsn(Opcodes.ICONST_1);
									}
									mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
								} else {
									mv.visitVarInsn(Opcodes.ALOAD, 0);
									mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name + TaintUtils.TAINT_FIELD, "I");
									mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
								}
							} else if (!Instrumenter.isIgnoredClass(fieldDesc.getInternalName()) && GEN_HAS_TAINTS_METHOD) {
								int op = Opcodes.INVOKEVIRTUAL;
								if (Instrumenter.isInterface(fieldDesc.getInternalName()))
									op = Opcodes.INVOKEINTERFACE;
								mv.visitVarInsn(Opcodes.ALOAD, 0);
								mv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
								mv.visitMethodInsn(op, fieldDesc.getInternalName(), "hasAnyTaints", "()Z",false);
								mv.visitJumpInsn(Opcodes.IFNE, hasTaint);
							} else {
								//TODO XXX MUST FETCH THE TAINT SOMEHOW FOR IGNORED CLASSES FOR THIS TO BE SOUND
							}
						}

						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitInsn(Opcodes.ICONST_0);
						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");

						mv.visitInsn(Opcodes.ICONST_0);
						mv.visitInsn(Opcodes.IRETURN);

						mv.visitLabel(hasTaint);
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitInsn(Opcodes.ICONST_0);
						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.IS_TAINT_SEATCHING_FIELD, "Z");

						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitInsn(Opcodes.ICONST_1);
						mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.HAS_TAINT_FIELD, "Z");
						mv.visitInsn(Opcodes.ICONST_1);
						mv.visitInsn(Opcodes.IRETURN);
					} else {
						mv.visitInsn(Opcodes.ICONST_0);
						mv.visitInsn(Opcodes.IRETURN);
					}
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				}
			}
		}
		

		if(TaintUtils.MULTI_TAINT)
			generateStrLdcWrapper();
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
						exceptions = m.exceptions.toArray(exceptions);
						MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
						
						//TODO maybe re-enable this
						if(fullMethod != null)
						{
							if(fullMethod.visibleAnnotations != null)
								for(AnnotationNode an : fullMethod.visibleAnnotations)
								{
									an.accept(mv.visitAnnotation(an.desc, true));
								}
							if(fullMethod.invisibleAnnotations != null)
								for(AnnotationNode an : fullMethod.invisibleAnnotations)
								{
									an.accept(mv.visitAnnotation(an.desc, false));
								}
////							if(fullMethod.visibleParameterAnnotations != null)
////								for(List<AnnotationNode> an : fullMethod.visibleParameterAnnotations)
////								{
////									an.accept(mv.visitParameterAnnotation(an., desc, visible));
////								}
						}
						NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, m.access, m.name, m.desc, mv);
						LocalVariableManager lvs = new LocalVariableManager(m.access, m.desc, an, an, mv);
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
										newDesc += t.getDescriptor().substring(0, t.getDescriptor().length() - 1) + "I";
										ga.visitVarInsn(Opcodes.ALOAD, idx);
										TaintAdapter.createNewTaintArray(t.getDescriptor(), an, lvs, lvs);
										loaded = true;
									} else {
										newDesc += MultiDTaintedArray.getTypeForType(t).getDescriptor();
										needToBoxMultiD = true;
									}
								}
							} else if (t.getSort() != Type.OBJECT) {
								newDesc += "I";
								ga.visitInsn(Opcodes.ICONST_0);
							}
							if (!loaded)
								ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
							if(NATIVE_BOX_UNBOX && t.getSort() == Type.OBJECT && Instrumenter.isCollection(t.getInternalName()))
							{
								////  public final static ensureIsBoxed(Ljava/util/Collection;)Ljava/util/Collection;
								ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NativeHelper.class), "ensureIsBoxed", "(Ljava/util/Collection;)Ljava/util/Collection;",false);
								ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
							}
							if (!needToBoxMultiD)
								newDesc += t.getDescriptor();
							else {
//								Label isNull = new Label();
								Label isDone = new Label();
								ga.visitInsn(Opcodes.DUP);
								ga.visitJumpInsn(Opcodes.IFNULL, isDone);
								ga.visitIntInsn(Opcodes.BIPUSH, t.getElementType().getSort());
								ga.visitIntInsn(Opcodes.BIPUSH, t.getDimensions());
								ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "initWithEmptyTaints", "([Ljava/lang/Object;II)Ljava/lang/Object;",false);
								FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
								fn.stack.set(fn.stack.size() -1,"java/lang/Object");
								ga.visitLabel(isDone);
								fn.accept(lvs);		
								ga.visitTypeInsn(Opcodes.CHECKCAST, MultiDTaintedArray.getTypeForType(t).getDescriptor());

							}
							idx += t.getSize();
						}
						
						if (m.name.equals("<init>")) {
							newDesc += Type.getDescriptor(TaintSentinel.class);
							ga.visitInsn(Opcodes.ACONST_NULL);
						}
						if(needToPrealloc)
						{
							newDesc += newReturn.getDescriptor();
							an.visitVarInsn(Opcodes.ALOAD, lvs.getPreAllocedReturnTypeVar(newReturn));
						}
						newDesc += ")" + newReturn.getDescriptor();

						int opcode;
						if ((m.access & Opcodes.ACC_STATIC) == 0) {
							if ((m.access & Opcodes.ACC_PRIVATE) != 0 || m.name.equals("<init>"))
								opcode = Opcodes.INVOKESPECIAL;
							else
								opcode = Opcodes.INVOKEVIRTUAL;
						} else
							opcode = Opcodes.INVOKESTATIC;
						if (m.name.equals("<init>")) {
							ga.visitMethodInsn(Opcodes.INVOKESPECIAL, className, m.name, newDesc,false);
						} else
							ga.visitMethodInsn(opcode, className, m.name + TaintUtils.METHOD_SUFFIX, newDesc,false);

						//unbox collections
						idx =0;
						if ((m.access & Opcodes.ACC_STATIC) == 0) {
							idx++;
						}

						for (Type t : argTypes) {
							if(NATIVE_BOX_UNBOX && t.getSort() == Type.OBJECT && Instrumenter.isCollection(t.getInternalName()))
							{
								////  public final static ensureIsBoxed(Ljava/util/Collection;)Ljava/util/Collection;
								ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
								ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NativeHelper.class), "ensureIsUnBoxed", "(Ljava/util/Collection;)Ljava/util/Collection;",false);
								ga.visitInsn(Opcodes.POP);
							}
							idx += t.getSize();
						}
						if (origReturn != newReturn) {
							String taintType = TaintUtils.getShadowTaintType(origReturn.getDescriptor());
							if (taintType != null) {
								//							ga.visitInsn(Opcodes.DUP);
								//							String taintTypeRaw = "I";
								//							if (origReturn.getSort() == Type.ARRAY)
								//								taintTypeRaw = "[I";
								//							ga.visitFieldInsn(Opcodes.GETFIELD, newReturn.getInternalName(), "taint", taintTypeRaw);
								//							ga.visitInsn(Opcodes.SWAP);
								ga.visitFieldInsn(Opcodes.GETFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
							} else {
								//Need to convert from [[WrapperForCArray to [[[C

								Label isDone = new Label();
								ga.visitInsn(Opcodes.DUP);
								ga.visitJumpInsn(Opcodes.IFNULL, isDone);
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions()-1);
								ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxVal", "(Ljava/lang/Object;II)Ljava/lang/Object;",false);
								FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
								fn.stack.set(fn.stack.size() -1,"java/lang/Object");
								ga.visitLabel(isDone);
								fn.accept(lvs);
								ga.visitTypeInsn(Opcodes.CHECKCAST, origReturn.getInternalName());

							}
						}
						Label endLabel = new Label();
						ga.visitLabel(endLabel);
						ga.returnValue();
						ga.visitMaxs(0, 0);
//						int j = 0;
						for (LocalVariableNode n : m.localVariables) {
							ga.visitLocalVariable(n.name, n.desc, n.signature, startLabel, endLabel, n.index);
						}

						if (m.name.equals("<init>")) {

						}
						ga.visitEnd();
					} else {
						String[] exceptions = new String[m.exceptions.size()];
						exceptions = m.exceptions.toArray(exceptions);
						MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
						mv.visitEnd();
					}
				} else {

					//generate wrapper for native method - a native wrapper
					m.access = m.access & ~Opcodes.ACC_NATIVE;
					String[] exceptions = new String[m.exceptions.size()];
					exceptions = m.exceptions.toArray(exceptions);
					Type[] argTypes = Type.getArgumentTypes(m.desc);

					boolean isPreAllocReturnType = TaintUtils.isPreAllocReturnType(m.desc);
					String newDesc = "(";
					LinkedList<LocalVariableNode> lvsToVisit = new LinkedList<LocalVariableNode>();
					LabelNode start = new LabelNode(new Label());
					LabelNode end = new LabelNode(new Label());
					for (Type t : argTypes) {
						if (t.getSort() == Type.ARRAY) {
							if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
								newDesc += TaintUtils.getShadowTaintType(t.getDescriptor());
							}
						} else if (t.getSort() != Type.OBJECT) {
							newDesc += "I";
						}
						if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1)
							newDesc += MultiDTaintedArray.getTypeForType(t).getDescriptor();
						else
							newDesc += t.getDescriptor();
					}
					Type origReturn = Type.getReturnType(m.desc);
					Type newReturn = TaintUtils.getContainerReturnType(origReturn);
					if(isPreAllocReturnType)
						newDesc += newReturn.getDescriptor();
					newDesc += ")" + newReturn.getDescriptor();

					MethodVisitor mv = super.visitMethod(m.access, m.name + TaintUtils.METHOD_SUFFIX, newDesc, m.signature, exceptions);
					NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, m.access, m.name, newDesc, mv);
					LocalVariableManager lvs = new LocalVariableManager(m.access,newDesc, an, an, mv);
					lvs.setPrimitiveArrayAnalyzer(new PrimitiveArrayAnalyzer(newReturn));
					GeneratorAdapter ga = new GeneratorAdapter(lvs, m.access, m.name + TaintUtils.METHOD_SUFFIX, newDesc);
					ga.visitCode();
					ga.visitLabel(start.getLabel());
										
					int idx = 0;
					if ((m.access & Opcodes.ACC_STATIC) == 0) {
						ga.visitVarInsn(Opcodes.ALOAD, 0);
						lvsToVisit.add(new LocalVariableNode("this", "L"+className+";", null, start, end, idx));
						idx++;
					}
					for (Type t : argTypes) {
						if (t.getSort() == Type.ARRAY) {
							if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
								lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg"+idx, "[I", null, start, end, idx));
								idx++;
							}
						} else if (t.getSort() != Type.OBJECT) {
							lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg"+idx,"I", null, start, end, idx));
							idx++;
						}
						ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);

						lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg"+idx, t.getDescriptor(), null, start, end, idx));
						if (t.getDescriptor().equals("Ljava/lang/Object;") || (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().equals("Ljava/lang/Object;"))) {
							//Need to make sure that it's not a boxed primitive array
							ga.visitInsn(Opcodes.DUP);
							ga.visitInsn(Opcodes.DUP);
							Label isOK = new Label();
							ga.visitTypeInsn(Opcodes.INSTANCEOF, "[" + Type.getDescriptor(MultiDTaintedArray.class));
							ga.visitInsn(Opcodes.SWAP);
							ga.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(MultiDTaintedArray.class));
							ga.visitInsn(Opcodes.IOR);
							ga.visitJumpInsn(Opcodes.IFEQ, isOK);
							ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;",false);
							if(t.getSort() == Type.ARRAY)
								ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
							FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
							ga.visitLabel(isOK);
							fn.accept(lvs);
						}
						else if(t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT)
						{
							//Need to unbox it!!
							ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;",false);
							ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
						}
						idx += t.getSize();
					}

					int opcode;
					if ((m.access & Opcodes.ACC_STATIC) == 0) {
						if ((m.access & Opcodes.ACC_PRIVATE) != 0 || m.name.equals("<init>"))
							opcode = Opcodes.INVOKESPECIAL;
						else
							opcode = Opcodes.INVOKEVIRTUAL;
					} else
						opcode = Opcodes.INVOKESTATIC;
					ga.visitMethodInsn(opcode, className, m.name, m.desc,false);
					if (origReturn != newReturn) {

						if (origReturn.getSort() == Type.ARRAY) {
							if (origReturn.getDimensions() > 1) {
								//							System.out.println(an.stack + " > " + newReturn);
								Label isOK = new Label();
								ga.visitInsn(Opcodes.DUP);
								ga.visitJumpInsn(Opcodes.IFNULL, isOK);
								ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
								//							//	public static Object[] initWithEmptyTaints(Object[] ar, int componentType, int dims) {
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
								ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions());
								ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "initWithEmptyTaints", "([Ljava/lang/Object;II)Ljava/lang/Object;",false);
								FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
								fn.stack.set(fn.stack.size() -1,"java/lang/Object");
								ga.visitLabel(isOK);
								fn.accept(lvs);
								ga.visitTypeInsn(Opcodes.CHECKCAST, newReturn.getDescriptor());
							} else {
								TaintAdapter.createNewTaintArray(origReturn.getDescriptor(), an, lvs, lvs);

//								//						ga.visitInsn(Opcodes.SWAP);
//								ga.visitTypeInsn(Opcodes.NEW, newReturn.getInternalName()); //T V N
//								ga.visitInsn(Opcodes.DUP_X2); //N T V N
//								ga.visitInsn(Opcodes.DUP_X2); //N N T V N
//								ga.visitInsn(Opcodes.POP); //N N T V
//								ga.visitMethodInsn(Opcodes.INVOKESPECIAL, newReturn.getInternalName(), "<init>", "([I" + origReturn.getDescriptor() + ")V");
								int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.SWAP);
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.SWAP);
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", "[I");
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
							}
						} else {
							//TODO here's where we store to the pre-alloc'ed container
							if(origReturn.getSize() == 1)
							{
								int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.SWAP);
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.ICONST_0);
								idx = 0;
								if ((m.access & Opcodes.ACC_STATIC) == 0) {
									idx++;
								}
								for (Type t : argTypes) {
									if (t.getSort() == Type.ARRAY) {
										if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
											idx++;
										}
									} else if (t.getSort() != Type.OBJECT) {
										ga.visitVarInsn(Opcodes.ILOAD, idx);
										if(TaintUtils.MULTI_TAINT)
										{
											ga.visitMethodInsn(Opcodes.INVOKESTATIC, TaintUtils.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(II)I", false);
										}
										else
										{
											ga.visitInsn(Opcodes.IOR);
										}
										idx++;
									}
									idx += t.getSize();
								}
								
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", "I");
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
							}
							else
							{

								int retIdx = lvs.getPreAllocedReturnTypeVar(newReturn);
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.DUP_X2);
								ga.visitInsn(Opcodes.POP);
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
								ga.visitInsn(Opcodes.ICONST_0);
								idx = 0;
								if ((m.access & Opcodes.ACC_STATIC) == 0) {
									idx++;
								}

								//IOR all of the primitive args in too
								for (Type t : argTypes) {
									if (t.getSort() == Type.ARRAY) {
										if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
											idx++;
										}
									} else if (t.getSort() != Type.OBJECT) {
										ga.visitVarInsn(Opcodes.ILOAD, idx);
										if(TaintUtils.MULTI_TAINT)
										{
											ga.visitMethodInsn(Opcodes.INVOKESTATIC, TaintUtils.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(II)I", false);
										}
										else
										{
											ga.visitInsn(Opcodes.IOR);
										}
										idx++;
									}
									idx += t.getSize();
								}
								
								ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", "I");
								an.visitVarInsn(Opcodes.ALOAD, retIdx);
//								ga.visitInsn(Opcodes.ARETURN);
							}
//							if (origReturn.getSize() == 1)
//								ga.visitInsn(Opcodes.SWAP);
//							else {
//								ga.visitInsn(Opcodes.DUP_X2);
//								ga.visitInsn(Opcodes.POP);
//							}
//							ga.visitMethodInsn(Opcodes.INVOKESTATIC, newReturn.getInternalName(), "valueOf", "(I" + origReturn.getDescriptor() + ")" + newReturn.getDescriptor());
						}
					} else if (origReturn.getSort() != Type.VOID && (origReturn.getDescriptor().equals("Ljava/lang/Object;") || origReturn.getDescriptor().equals("[Ljava/lang/Object;"))) {
						//Check to see if the top of the stack is a primitive array, adn if so, box it.
						ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;",false);
						if (origReturn.getSort() == Type.ARRAY)
							ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
					}
					ga.visitLabel(end.getLabel());
					ga.returnValue();
					ga.visitMaxs(0, 0);
					if(isPreAllocReturnType)
					{
						lvsToVisit.add(new LocalVariableNode("phosphorReturnHolder", newReturn.getDescriptor(), null, start, end, lvs.getPreAllocedReturnTypeVar(newReturn)));
					}
					ga.visitEnd();
					for(LocalVariableNode n : lvsToVisit)
						n.accept(ga);
					
				}
			}
//		if (!goLightOnGeneratedStuff && TaintUtils.GENERATE_FASTPATH_VERSIONS)
//			for (final MethodNode m : myMethods) {
//				final String oldDesc = m.desc;
//				if (m.name.equals("<init>")) {
//					m.desc = m.desc.substring(0, m.desc.indexOf(")")) + Type.getDescriptor(UninstrumentedTaintSentinel.class) + ")" + Type.getReturnType(m.desc).getDescriptor();
//				} else if (m.name.equals("<clinit>")) {
//					continue;
//				} else {
//					m.name = m.name.replace(TaintUtils.METHOD_SUFFIX, "") + "$$INVIVO_UNINST";
//				}
//				if ((m.access & Opcodes.ACC_ABSTRACT) != 0 && !isInterface) {
//					//Let's see what happens if we make these non-abstract, with no body, to try to fix
//					//problems with jasper usage.
//					m.access = m.access & ~Opcodes.ACC_ABSTRACT;
//					m.instructions = new InsnList();
//					Type ret = Type.getReturnType(m.desc);
//					switch (ret.getSort()) {
//					case Type.BOOLEAN:
//					case Type.BYTE:
//					case Type.CHAR:
//					case Type.SHORT:
//					case Type.INT:
//						m.instructions.add(new InsnNode(Opcodes.ICONST_0));
//						m.instructions.add(new InsnNode(Opcodes.IRETURN));
//						break;
//					case Type.DOUBLE:
//						m.instructions.add(new InsnNode(Opcodes.DCONST_0));
//						m.instructions.add(new InsnNode(Opcodes.DRETURN));
//						break;
//					case Type.FLOAT:
//						m.instructions.add(new InsnNode(Opcodes.FCONST_0));
//						m.instructions.add(new InsnNode(Opcodes.FRETURN));
//						break;
//					case Type.LONG:
//						m.instructions.add(new InsnNode(Opcodes.LCONST_0));
//						m.instructions.add(new InsnNode(Opcodes.LRETURN));
//						break;
//					case Type.ARRAY:
//					case Type.OBJECT:
//						m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
//						m.instructions.add(new InsnNode(Opcodes.ARETURN));
//						break;
//					case Type.VOID:
//						m.instructions.add(new InsnNode(Opcodes.RETURN));
//						break;
//					}
//				}
//				m.accept(new ClassVisitor(Opcodes.ASM5, this.cv) {
//					@Override
//					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//						if (name.equals("<init>")) {
//							mv = new ConstructorArgReindexer(mv, access, name, desc, oldDesc);
//						}
//						return new MethodVisitor(api, mv) {
//							@Override
//							public void visitVarInsn(int opcode, int var) {
//								super.visitVarInsn(opcode, var);
//							}
//
//							@Override
//							public void visitMethodInsn(int opcode, String owner, String name, String desc) {
//								if (!Instrumenter.isIgnoredClass(owner)) {
//									if (name.equals("<init>")) {
//										super.visitInsn(Opcodes.ACONST_NULL);
//										desc = desc.substring(0, desc.indexOf(")")) + Type.getDescriptor(UninstrumentedTaintSentinel.class) + ")" + Type.getReturnType(desc).getDescriptor();
//									} else
//										name = name + "$$INVIVO_UNINST";
//								}
//								super.visitMethodInsn(opcode, owner, name, desc);
//							}
//						};
//					}
//				});
//			}

		super.visitEnd();
	}

	private void generateStrLdcWrapper() {
		if (!isNormalClass)
			return;
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, TaintUtils.STR_LDC_WRAPPER, "(Ljava/lang/String;)Ljava/lang/String;", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0); //S
		mv.visitInsn(Opcodes.DUP); //S S
		mv.visitInsn(Opcodes.DUP2);

		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I",false);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IADD);
		mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
		mv.visitFieldInsn(Opcodes.PUTFIELD, "java/lang/String", "value" + TaintUtils.TAINT_FIELD, "[I");
		{
			mv.visitInsn(Opcodes.POP);
		}
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
