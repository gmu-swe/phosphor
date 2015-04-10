package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LocalVariableNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.Printer;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.Textifier;
import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStoreWithIntTags;
import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStoreWithObjTags;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedMisc;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class TaintPassingMV extends TaintAdapter implements Opcodes {

	int lastArg;
	Type originalMethodReturnType;
	Type newReturnType;

	private String name;
	private boolean isStatic = true;
	private Type[] paramTypes;

	public void setArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.arrayAnalyzer = primitiveArrayFixer;
	}

	@Override
	public void visitCode() {
//		System.out.println("TPMVStart" + name);
		super.visitCode();
		if(Configuration.IMPLICIT_TRACKING)
		{
			int tmpLV  = lvs.newControlTaintLV(0);
			jumpControlTaintLVs.add(tmpLV);
			super.visitTypeInsn(NEW, Type.getInternalName(ControlTaintTagStack.class));
			super.visitInsn(DUP);
			super.visitIntInsn(BIPUSH, arrayAnalyzer.nJumps);
			if(name.equals("<clinit>"))
				super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "(I)V", false);
			else
			{
			super.visitVarInsn(ALOAD, idxOfPassedControlInfo);
			super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "(I"+Type.getDescriptor(ControlTaintTagStack.class)+")V", false);
			}
			super.visitVarInsn(ASTORE, tmpLV);

		}
		//		if (arrayAnalyzer != null) {
		//			this.bbsToAddACONST_NULLto = arrayAnalyzer.getbbsToAddACONST_NULLto();
		//			this.bbsToAddChecktypeObjectto = arrayAnalyzer.getBbsToAddChecktypeObject();
		//		}
		//		if (TaintUtils.DEBUG_FRAMES)
		//			System.out.println("Need to dup " + Arrays.toString(bbsToAddACONST_NULLto) + " nulls based onthe analyzer result");
	}

	int curLabel = -1;

	@Override
	public void visitLabel(Label label) {
		if (isIgnoreAllInstrumenting) {
			super.visitLabel(label);
			return;
		}
		//		if (curLabel >= 0 && curLabel < bbsToAddChecktypeObjectto.length && bbsToAddChecktypeObjectto[curLabel] == 1) {
		//			visitTypeInsn(CHECKCAST, "java/lang/Object");
		//			bbsToAddChecktypeObjectto[curLabel] = 0;
		//		}
		//		if (curLabel >= 0 && curLabel < bbsToAddACONST_NULLto.length && bbsToAddACONST_NULLto[curLabel] == 1) {
		//			if (analyzer.stack.size() == 0 || topStackElIsNull())
		//			{
		//				if(TaintUtils.DEBUG_FRAMES)
		//					System.out.println("Pre add extra null: " + analyzer.stack);
		//				super.visitInsn(ACONST_NULL);
		//			}
		//			bbsToAddACONST_NULLto[curLabel] = 0;
		//		}
		super.visitLabel(label);
		curLabel++;
		//		System.out.println("CurLabel" + curLabel);
	}

	private String className;
	private MethodVisitor passthruMV;
	int idxOfPassedControlInfo = 0;
	public TaintPassingMV(MethodVisitor mv, int access, String className, String name, String desc, String originalDesc, NeverNullArgAnalyzerAdapter analyzer,MethodVisitor passthruMV) {
		//		super(Opcodes.ASM4,mv,access,name,desc);
		super(Opcodes.ASM5, className, mv, analyzer);
//				System.out.println("TPMV "+ className+"."+name+desc);
		this.name = name;
		this.className = className;
		Type[] oldArgTypes = Type.getArgumentTypes(originalDesc);
		Type[] newArgTypes = Type.getArgumentTypes(desc);
		lastArg = 0;
		for (Type t : newArgTypes) {
			lastArg += t.getSize();
		}
		if ((access & Opcodes.ACC_STATIC) == 0) {
			lastArg++;//take account for arg[0]=this
			isStatic = false;
		}
		originalMethodReturnType = Type.getReturnType(originalDesc);
		newReturnType = Type.getReturnType(desc);
		paramTypes = new Type[lastArg + 1];
		int n = (isStatic ? 0 : 1);
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println("New desc is " + Arrays.toString(newArgTypes));
		for (int i = 0; i < newArgTypes.length; i++) {
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("ARG TYPE: " + newArgTypes[i]);
			paramTypes[n] = newArgTypes[i];
			if(newArgTypes[i].getDescriptor().equals(Type.getDescriptor(ControlTaintTagStack.class)))
				idxOfPassedControlInfo = n;
			n += newArgTypes[i].getSize();
		}
		this.passthruMV = passthruMV;
	}

	protected Type getLocalType(int n) {
		if (n >= analyzer.locals.size())
			return null;
		Object t = analyzer.locals.get(n);
		if (t == Opcodes.TOP || t == Opcodes.NULL)
			return null;
		return getTypeForStackType(t);
	}

	public void visitIincInsn(int var, int increment) {
		if(Configuration.IMPLICIT_TRACKING)
		{
			if (isIgnoreAllInstrumenting || isRawInsns) {
				super.visitIincInsn(var, increment);
				return;
			}
			int shadowVar = 0;
			if (var < lastArg && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
				//accessing an arg; remap it
				Type localType = paramTypes[var];
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println(Arrays.toString(paramTypes) + ",,," + var);
				if (TaintUtils.getShadowTaintType(localType.getDescriptor()) != null)
					shadowVar = var - 1;
			} else {
				shadowVar = lvs.varToShadowVar.get(var);
			}
			super.visitVarInsn(ALOAD, shadowVar);
			super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
			super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
					"("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
			super.visitVarInsn(ASTORE, shadowVar);

		}
		super.visitIincInsn(var, increment);
	}
	
	HashMap<Integer, Object> varTypes = new HashMap<Integer, Object>();
	HashSet<Integer> questionableShadows = new HashSet<Integer>();

	HashSet<Integer> boxAtNextJump = new HashSet<Integer>();

	ArrayList<Integer> jumpControlTaintLVs = new ArrayList<Integer>();
	int branchStarting;
	HashSet<Integer> forceCtrlAdd = new HashSet<Integer>();
	@SuppressWarnings("unused")
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (opcode == TaintUtils.NEVER_AUTOBOX) {
			System.out.println("Never autobox: " + var);
			varsNeverToForceBox.add(var);
			return;
		}
		
		//Following 2 special cases are notes left by the post-dominator analysis for implicit taint tracking
		if(opcode == TaintUtils.BRANCH_START)
		{
			branchStarting = var;
			return;
		}
		if(opcode == TaintUtils.BRANCH_END)
		{
			passthruMV.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
			passthruMV.visitIntInsn(BIPUSH, var);
			passthruMV.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "pop", "(I)V", false);
			return;
		}
		if (opcode == TaintUtils.ALWAYS_AUTOBOX && analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
			Type t = Type.getType((String) analyzer.locals.get(var));
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && lvs.varToShadowVar.containsKey(var)) {
				//				System.out.println("Restoring " + var + " to be boxed");
				super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
				super.visitVarInsn(ALOAD, var);
				registerTaintedArray(getTopOfStackType().getDescriptor());
				super.visitVarInsn(ASTORE, var);
			}
			return;
		}
		if (opcode == TaintUtils.ALWAYS_BOX_JUMP) {
			boxAtNextJump.add(var);
			return;
		}
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			if(opcode != TaintUtils.FORCE_CTRL_STORE)
				super.visitVarInsn(opcode, var);
			return;
		}

		int shadowVar = -1;
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println(this.name + " " + Printer.OPCODES[opcode] + " on " + var + " last arg" + lastArg +", stack: " + analyzer.stack);
//			System.out.println("Locals " + analyzer.locals);

		if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
			System.out.println(this.name + " ASTORE " + var + ", shadowvar contains " + lvs.varToShadowVar.get(var) + " oldvartype " + varTypes.get(var));
		}
		boolean boxIt = false;

		if(Configuration.IMPLICIT_TRACKING)
		{
			switch(opcode)
			{
			case ISTORE:
			case FSTORE:
				super.visitInsn(SWAP);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				if(!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(SWAP);
				break;
			case DSTORE:
			case LSTORE:
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				if(!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				break;
			case ASTORE:
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
				break;
			case TaintUtils.FORCE_CTRL_STORE:
				forceCtrlAdd.add(var);
				return;
			}
		}

		if (varsNeverToForceBox.contains(var)) {
			boxIt = false;
			//			System.out.println("actually, not Boxing " + var);
		}
		if (var == 0 && !isStatic) {
			//accessing "this" so no-op, die here so we never have to worry about uninitialized this later on.
			super.visitVarInsn(opcode, var);
			return;
		} else if (var < lastArg && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
			//accessing an arg; remap it
			Type localType = paramTypes[var];
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println(Arrays.toString(paramTypes) + ",,," + var);
			if (TaintUtils.getShadowTaintType(localType.getDescriptor()) != null)
				shadowVar = var - 1;
		} else {
			//not accessing an arg
			
			Object oldVarType = varTypes.get(var);
			if (lvs.varToShadowVar.containsKey(var))
				shadowVar = lvs.varToShadowVar.get(var);
//						System.out.println(name+" "+Printer.OPCODES[opcode] + " "+var + " old " + oldVarType + " shadow " + shadowVar + " Last " + lastArg);
//						System.out.println(Arrays.toString(paramTypes));
			if (oldVarType != null) {
				//In this case, we already have a shadow for this. Make sure that it's the right kind of shadow though.
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println(name + Textifier.OPCODES[opcode] + " " + var + " old type is " + varTypes.get(var) + " shadow is " + shadowVar);
				//First: If we thought this was NULL before, but it's not NULL now (but instead another type), then update that.
				if (opcode == ALOAD && oldVarType == Opcodes.NULL && analyzer.locals.get(var) instanceof String) {
					varTypes.put(var, analyzer.locals.get(var));
				}
				if ((oldVarType == Opcodes.NULL || oldVarType instanceof String) && opcode != ASTORE && opcode != ALOAD) {
					//Went from a TYPE to a primitive.
					if (opcode == ISTORE || opcode == FSTORE || opcode == DSTORE || opcode == LSTORE)
						varTypes.put(var, getTopOfStackObject());
					else
						varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
					if (shadowVar > -1) {
						while(shadowVar >= analyzer.locals.size())
						{
							analyzer.locals.add(Opcodes.TOP);
						}
						analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
						lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_DESC));
					}
				} else if (oldVarType instanceof Integer && oldVarType != Opcodes.NULL && (opcode == ASTORE || opcode == ALOAD)) {
					//Went from primitive to TYPE
					if (opcode == ASTORE)
						varTypes.put(var, getTopOfStackObject());
					else
						varTypes.put(var, "Lunidentified;");

					if (shadowVar > -1){
						while(shadowVar >= analyzer.locals.size())
						{
							analyzer.locals.add(Opcodes.TOP);
						}
//						if(shadowVar == 9)
//							System.err.println("Setting the local type for " + shadowVar + analyzer.stack);
						if (opcode == ASTORE) {
							String shadow = TaintUtils.getShadowTaintType(getTopOfStackType().getDescriptor());
							if (shadow == null) {
								shadow = Configuration.TAINT_TAG_ARRAYDESC;
								analyzer.locals.set(shadowVar, Opcodes.TOP);
							} else if (shadow.equals(Configuration.TAINT_TAG_DESC))
								analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
							else
								analyzer.locals.set(shadowVar, shadow);
							lvs.remapLocal(shadowVar, Type.getType(shadow));
						} else {
							lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
							analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_ARRAYDESC);
						}
					}
				}

				if (opcode == ASTORE && !topOfStackIsNull() && !oldVarType.equals(getTopOfStackObject())) {
					varTypes.put(var, getTopOfStackObject());
				}
			}
			if (shadowVar < 0) {
				//We don't have a shadowvar for this yet. Do we need one?
				if (opcode == ALOAD) {
					if (analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
						Type localType = Type.getObjectType((String) analyzer.locals.get(var));
						if (localType.getSort() == Type.ARRAY && localType.getElementType().getSort() != Type.OBJECT && localType.getDimensions() == 1) {
							lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC),var));
							varTypes.put(var, Opcodes.NULL);
							shadowVar = lvs.varToShadowVar.get(var);
							if (shadowVar == analyzer.locals.size())
								analyzer.locals.add(Opcodes.NULL);
						}
					}
				} else if (opcode == ASTORE) {
					if (topOfStackIsNull()) {
//						System.out.println("Storing null. maybe its a primitive int array? at " + var);
//						if(analyzer.locals.size() > var)
//						System.out.println(analyzer.locals.get(var));
						lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC),var));
						varTypes.put(var, Opcodes.NULL);
						shadowVar = lvs.varToShadowVar.get(var);
						lvs.varsToRemove.put(var,shadowVar);
					} else {
						Type onStack = getTopOfStackType();
						if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
							//That's easy.
							lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(TaintUtils.getShadowTaintType(onStack.getDescriptor())),var));
							varTypes.put(var, getTopOfStackObject());
							shadowVar = lvs.varToShadowVar.get(var);
						}
					}
				} else {
//					if (TaintUtils.DEBUG_LOCAL)
					lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_DESC),var));
					varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
					shadowVar = lvs.varToShadowVar.get(var);
//					System.out.println("creating shadow for " + var + " - " + shadowVar);
					if (opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD) {
						if (shadowVar == analyzer.locals.size())
							analyzer.locals.add(Configuration.TAINT_TAG_STACK_TYPE);
					}
				}
			}

			if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
				System.out.println("ASTORE " + var + ", shadowvar contains " + lvs.varToShadowVar.get(var));
			}
			if (shadowVar > -1 && TaintUtils.DEBUG_LOCAL) {
				System.out.println("using shadow " + shadowVar + "for " + var + (boxIt ? " boxit" : " notboxit"));
				System.out.println("LVS: " + analyzer.locals);
			}
		}
		if (shadowVar >= 0 && !boxIt) {
			switch (opcode) {
			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
//				System.out.println("PRE LOAD" + var + analyzer.stack + "; " + analyzer.locals);
				super.visitVarInsn((!Configuration.MULTI_TAINTING ? ILOAD: ALOAD), shadowVar);
				super.visitVarInsn(opcode, var);
//				System.out.println("POST LOAD" + var + analyzer.stack);
				return;
			case Opcodes.ALOAD:
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("PRE ALOAD " + var);
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("Locals: " + analyzer.locals);
				Type localType = null;
				if (var >= analyzer.locals.size()) {
					System.err.println(analyzer.locals);
					System.err.println(className);
					IllegalStateException ex = new IllegalStateException("Trying to load an arg (" + var + ") past end of analyzer locals");
					throw ex;
				}
				//				System.out.println("Pre ALOAD " + var + "localtype " + localType);

				if (nextLoadIsNotTainted) {
					nextLoadIsNotTainted = false;
					localType = Type.getType(Object.class);
				} else if (nextLoadIsTainted) {
					nextLoadIsTainted = false;
					localType = Type.getType(Configuration.TAINT_TAG_ARRAYDESC);
				} else {
					if (analyzer.locals.get(var) == Opcodes.NULL) {
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("Ignoring shadow " + shadowVar + " on ALOAD " + var + " because var is null");
						super.visitVarInsn(opcode, var);
						return;
					}
					if (analyzer.locals.get(var) instanceof Integer) {
						System.out.println(className + "." + name);
						System.out.println("ALOAD " + var + " but found " + analyzer.locals.get(var));
						System.out.println(analyzer.locals);
						throw new IllegalArgumentException();
					}
					if (analyzer.locals.get(var) instanceof Label) {
						//this var is uninitilaized obj. def not an array or anythign we care about.
						super.visitVarInsn(opcode, var);
						return;
					}
					localType = Type.getType((String) analyzer.locals.get(var));
				}
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("Pre ALOAD " + var + "localtype " + localType);
				if (localType.getSort() == Type.ARRAY && localType.getDimensions() == 1) {
					switch (localType.getElementType().getSort()) {
					case Type.ARRAY:
					case Type.OBJECT:
						super.visitVarInsn(opcode, var);
						return;
					default:
						super.visitVarInsn(ALOAD, shadowVar);
						super.visitVarInsn(opcode, var);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ALOAD " + var);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("Locals: " + analyzer.locals);
						return;
					}
				} else {
					super.visitVarInsn(opcode, var);
					return;
				}
			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
				super.visitVarInsn(opcode, var);
				super.visitVarInsn((!Configuration.MULTI_TAINTING ? ISTORE : ASTORE), shadowVar);
				return;
			case Opcodes.ASTORE:
				Object stackEl = analyzer.stack.get(analyzer.stack.size() - 1);
				if (stackEl == Opcodes.NULL) {
					super.visitInsn(ACONST_NULL);
					super.visitVarInsn(ASTORE, shadowVar);
					super.visitVarInsn(opcode, var);
					if (TaintUtils.DEBUG_LOCAL)
						System.out.println("stack top was null, now POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
					return;
				}
				if (!(stackEl instanceof String)) {
					super.visitVarInsn(opcode, var);
					IllegalStateException ex = new IllegalStateException("Doing ASTORE but top of stack isn't a type, it's " + stackEl);
					ex.printStackTrace();
					return;
				}
				Type stackType = Type.getType((String) stackEl);

				//				if (TaintUtils.DEBUG_LOCAL)
				//					System.out.println("ASTORE " + var + ": stack is " + analyzer.stack + " StackType is " + stackType + " lvs " + analyzer.locals);
				if (stackType.getSort() == Type.ARRAY && stackType.getDimensions() == 1) {
					switch (stackType.getElementType().getSort()) {
					case Type.ARRAY:
						super.visitVarInsn(opcode, var);
						super.visitVarInsn(ASTORE, shadowVar);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					case Type.OBJECT:
						super.visitVarInsn(opcode, var);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					default:
						super.visitVarInsn(opcode, var);
						super.visitVarInsn(ASTORE, shadowVar);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					}
				}
				//				System.out.println("Ignoring shadow, because top of stack is " + stackType);
				super.visitVarInsn(opcode, var);
				return;
			case Opcodes.RET:
				break;
			}
		} else {
			if (opcode == ASTORE && getTopOfStackType().getSort() == Type.ARRAY && getTopOfStackType().getElementType().getSort() != Type.OBJECT) {
				//Storing a primitive array to a local of type Object - need to box it.
//				System.out.println("ASTORE " + var + " -> " + getTopOfStackType() + " but no shadow to be found");
				registerTaintedArray(getTopOfStackType().getDescriptor());

				super.visitVarInsn(opcode, var);
			} else
				super.visitVarInsn(opcode, var);
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("(no shadow) POST " + opcode + " " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
		}
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (isIgnoreAllInstrumenting) {
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
				
		Type ownerType = Type.getObjectType(owner);
		Type descType = Type.getType(desc);
		if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
			desc = MultiDTaintedArray.getTypeForType(descType).getInternalName();
		}
		
		if(Configuration.IMPLICIT_TRACKING)
		{
			switch(opcode)
			{
			case PUTFIELD:
			case PUTSTATIC:
				if (descType.getSize() == 1) {
					if (descType.getSort() == Type.OBJECT) {
						super.visitInsn(DUP);
						super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);						
					} else if( descType.getSort() != Type.ARRAY){
						super.visitInsn(SWAP);
						super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
						if(!Configuration.MULTI_TAINTING)
							super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
						else
						{
							super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
						}
						super.visitInsn(SWAP);
					}
				} else {
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
					if(!Configuration.MULTI_TAINTING)
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
					else
					{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
					}
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
				break;
			case ASTORE:
				break;
			}
		}
		if (ignoreLoadingNextTaint) {
			switch (opcode) {
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
				if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {

					Type newType = MultiDTaintedArray.getTypeForType(descType);
					if (newType.getSort() == Type.ARRAY) {
						super.visitFieldInsn(opcode, owner, name, desc);
					} else {
						super.visitFieldInsn(opcode, owner, name, desc);

						Label isNull = new Label();
						Label isDone = new Label();

						FrameNode fn = getCurrentFrameNode();
						super.visitInsn(DUP);
						if(!ignoreLoadingNextTaint && !isIgnoreAllInstrumenting)
						super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
						super.visitJumpInsn(IFNULL, isNull);
						if(!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
						super.visitInsn(TaintUtils.IGNORE_EVERYTHING);

						//		System.out.println("unbox: " + onStack + " type passed is " + type);
						super.visitFieldInsn(GETFIELD, newType.getInternalName(), "val", descType.getDescriptor());
						super.visitJumpInsn(GOTO, isDone);
						super.visitLabel(isNull);
						fn.stack.set(fn.stack.size()-1, descType.getDescriptor());
						fn.accept(this);
						super.visitTypeInsn(CHECKCAST, descType.getDescriptor());
						super.visitLabel(isDone);
						fn.accept(this);
					}
				} else {
					super.visitFieldInsn(opcode, owner, name, desc);
				}
				break;
			default:
				throw new IllegalArgumentException("can't do taintless putfield for now");

			}
			return;
		}

		boolean isIgnoredTaint = Instrumenter.isIgnoredClass(owner);

		switch (opcode) {
		case Opcodes.GETSTATIC:
			String shadowType = TaintUtils.getShadowTaintType(desc);
			if (shadowType != null) {
				if (isIgnoredTaint) {
					super.visitFieldInsn(opcode, owner, name, desc);
					if(desc.startsWith("["))
						retrieveTopOfStackTaintArray();
					else
						super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
					super.visitInsn(SWAP);
				} else {
					super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					super.visitFieldInsn(opcode, owner, name, desc);

				}
			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.GETFIELD:
			shadowType = TaintUtils.getShadowTaintType(desc);
			if (shadowType != null) {
				if (isIgnoredTaint) {
					super.visitFieldInsn(opcode, owner, name, desc);
					if(desc.startsWith("["))
						retrieveTopOfStackTaintArray();
					else
						super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
					super.visitInsn(SWAP);
				} else {
					super.visitInsn(DUP);
					super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					super.visitInsn(SWAP);
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.PUTSTATIC:
			if (getTopOfStackType().getSort() == Type.OBJECT && descType.getSort() == Type.ARRAY && descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT)
				retrieveTaintedArray(desc);
			shadowType = TaintUtils.getShadowTaintType(desc);
			if (shadowType != null && topStackElIsNull()) //Putting NULL to an array type
				super.visitInsn(ACONST_NULL);
			Type onStack = getTopOfStackType();
			if (shadowType == null && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
				registerTaintedArray(desc);
				super.visitFieldInsn(opcode, owner, name, desc);
			} else {
				super.visitFieldInsn(opcode, owner, name, desc);
				if (shadowType != null) {
					if (isIgnoredTaint)
						super.visitInsn(POP);
					else
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
				}
			}
			break;
		case Opcodes.PUTFIELD:
			//get an extra copy of the field owner
			//			System.out.println("PUTFIELD " + owner+"."+name+desc + analyzer.stack);
			if (getTopOfStackType().getSort() == Type.OBJECT && descType.getSort() == Type.ARRAY && descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT)
				retrieveTaintedArray(desc);
			shadowType = TaintUtils.getShadowTaintType(desc);

			if (shadowType != null) {
				if (Type.getType(desc).getSize() == 2) {

					// R T VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2); // VV R T
					super.visitInsn(SWAP);// VV T R
					super.visitInsn(DUP_X1); // VV R T R
					super.visitInsn(SWAP);// VV R R T
					if (isIgnoredTaint)
						super.visitInsn(POP2);
					else
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					super.visitInsn(DUP_X2);//VV R
					super.visitInsn(POP);// R VV R
					super.visitFieldInsn(opcode, owner, name, desc);// R VV
				} else {
					//Are we storing ACONST_NULL to a primitive array field? If so, there won't be a taint!
					if (Type.getType(desc).getSort() == Type.ARRAY && Type.getType(desc).getElementType().getSort() != Type.OBJECT && analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.NULL) {
						super.visitInsn(POP);
						super.visitInsn(DUP);
						super.visitInsn(ACONST_NULL);
						super.visitFieldInsn(opcode, owner, name, desc);
						super.visitInsn(ACONST_NULL);
						if (isIgnoredTaint)
							super.visitInsn(POP2);
						else
							super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					} else {
						super.visitInsn(DUP2_X1);
						super.visitInsn(POP2);
						super.visitInsn(DUP_X2);
						super.visitInsn(SWAP);
						super.visitFieldInsn(opcode, owner, name, desc);
						if (isIgnoredTaint)
							super.visitInsn(POP2);
						else
							super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					}
				}
			} else {
				onStack = getTopOfStackType();
				if (shadowType == null && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
					registerTaintedArray(onStack.getDescriptor());
					super.visitFieldInsn(opcode, owner, name, desc);
				} else {
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			}

			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	HashSet<Integer> varsNeverToForceBox = new HashSet<Integer>();
	HashSet<Integer> varsAlwaysToForceBox = new HashSet<Integer>();

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitIntInsn(opcode, operand);
			return;
		}

		switch (opcode) {
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			super.visitIntInsn(opcode, operand);
			break;
		case Opcodes.NEWARRAY:

			/*
			 * T L L T L [I L [I [I [I L [I [I L-1 L [I [I L-1 T L [I [I L [I [A
			 */
			super.visitInsn(SWAP);//We should just drop the taint for the size of the new array
			super.visitInsn(POP);
			super.visitInsn(DUP);
//			super.visitInsn(ICONST_1);
//			super.visitInsn(IADD);
			if(!Configuration.MULTI_TAINTING)
				super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			else
				super.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
			//Our fancy new hack where we store the taint for a primitive array in the last element of the (oversized) taint array.
			//Generate a new array taint, store it back in it.
//			super.visitInsn(DUP);
//			super.visitInsn(DUP);
//			super.visitInsn(ARRAYLENGTH);
//			super.visitInsn(ICONST_M1);
//			super.visitInsn(IADD);
//			super.visitInsn(ICONST_0);
//			super.visitInsn(IASTORE);

			super.visitInsn(SWAP);
			super.visitIntInsn(opcode, operand);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (isIgnoreAllInstrumenting) {
			super.visitMultiANewArrayInsn(desc, dims);
			return;
		}
		Type arrayType = Type.getType(desc);
		Type origType = Type.getType(desc);
		boolean needToHackDims = false;
		if (arrayType.getElementType().getSort() != Type.OBJECT) {
			if (dims == arrayType.getDimensions()) {
				needToHackDims = true;
			}
			arrayType = MultiDTaintedArray.getTypeForType(arrayType);
			//Type.getType(MultiDTaintedArray.getClassForComponentType(arrayType.getElementType().getSort()));
			desc = arrayType.getInternalName();
		}
		//Stack has T,Capacity repeated dims times
		super.visitIntInsn(BIPUSH, dims);
		super.visitIntInsn(NEWARRAY, Opcodes.T_INT);
		int tempArrayIdx = lvs.getTmpLV();
		super.visitInsn(DUP); //or aload?	V A
		super.visitVarInsn(ASTORE, tempArrayIdx);
		for (int i = 0; i < dims; i++) {
			super.visitInsn(SWAP); //			A V
			super.visitIntInsn(BIPUSH, i); //	A V I
			super.visitInsn(SWAP); //			A I V
			super.visitInsn(IASTORE);
			super.visitInsn(POP);
			super.visitVarInsn(ALOAD, tempArrayIdx);
		}
		if (needToHackDims)
			dims--;
		//Top of stack is array with dimensions, no taints.
		for (int i = 0; i < dims; i++) {
			super.visitInsn(DUP);
			super.visitIntInsn(BIPUSH, dims - i - 1 + (needToHackDims ? 1 : 0));
			super.visitInsn(IALOAD);
			super.visitInsn(SWAP);
		}
		super.visitInsn(POP);
		if (dims == 1) {
			//It's possible that we dropped down to a 1D object type array
			super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
		} else
			super.visitMultiANewArrayInsn(desc, dims);
		if (needToHackDims) {
			super.visitInsn(DUP);
			super.visitVarInsn(ALOAD, tempArrayIdx);
			super.visitIntInsn(BIPUSH, 0);
			super.visitInsn(IALOAD);

			super.visitIntInsn(BIPUSH, origType.getElementType().getSort());
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)), "initLastDim", "([Ljava/lang/Object;II)V",false);
		}
		lvs.freeTmpLV(tempArrayIdx);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (isIgnoreAllInstrumenting) {
			super.visitTypeInsn(opcode, type);
			return;
		}
		switch (opcode) {
		case Opcodes.ANEWARRAY:
			super.visitInsn(SWAP);//We should just drop the taint for the size of the new array
			super.visitInsn(POP);
			Type t = Type.getType(type);
			if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
				//e.g. [I for a 2 D array -> MultiDTaintedIntArray
				type = MultiDTaintedArray.getTypeForType(t).getInternalName();
				//TODO - initialize this?????
			}
			super.visitTypeInsn(opcode, type);
			break;
		case Opcodes.NEW:
			super.visitTypeInsn(opcode, type);
			break;
		case Opcodes.CHECKCAST:

			t = Type.getType(type);

			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				if (t.getDimensions() > 1) {
					//Hahaha you thought you could cast to a primitive multi dimensional array!

					super.visitTypeInsn(opcode, MultiDTaintedArray.getTypeForType(Type.getType(type)).getDescriptor());
					return;
				} else {
					//what is on the top of the stack that we are checkcast'ing?
					Object o = analyzer.stack.get(analyzer.stack.size() - 1);
					if (o instanceof String) {
						Type zz = getTypeForStackType(o);
						if (zz.getSort() == Type.ARRAY && zz.getElementType().getSort() != Type.OBJECT) {
							super.visitTypeInsn(opcode, type);
							break;
						}
					}
					//cast of Object[] or Object to char[] or int[] etc.
					if(o == Opcodes.NULL)
					{
						super.visitInsn(ACONST_NULL);
						super.visitTypeInsn(CHECKCAST,Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
						super.visitInsn(SWAP);
					}
					else
					retrieveTaintedArray(type);
					super.visitTypeInsn(opcode, type);
				}
				//			} else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT) {
				//				Object o = analyzer.stack.get(analyzer.stack.size() - 1);
				//				if (o instanceof String) {
				//					Type zz = getTypeForStackType(o);
				//					if (zz.getSort() == Type.OBJECT) {
				//						//casting object to Object[]
				//						super.visitTypeInsn(opcode, type);
				//						break;
				//					}
				//				}
				//				if (topStackElCarriesTaints()) {
				//					//Casting array to non-array type
				//
				//					//Register the taint array for later.
				//					registerTaintedArray(t.getDescriptor());
				//				}
			} else {

				//What if we are casting an array to Object?
				if (topStackElCarriesTaints()) {
					//Casting array to non-array type

					//Register the taint array for later.
					registerTaintedArray(getTopOfStackType().getDescriptor());
				}
				super.visitTypeInsn(opcode, type);
			}
			break;
		case Opcodes.INSTANCEOF:
			t = Type.getType(type);

			boolean doIOR = false;
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				if (t.getDimensions() > 1) {
					type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
				} else if (!topStackElCarriesTaints()) {
					doIOR = true;
					//Maybe we have it boxed on the stack, maybe we don't - how do we know? Who cares, just check both...
					super.visitInsn(DUP);
					super.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.getClassForComponentType(t.getElementType().getSort())));
					super.visitInsn(SWAP);
				}
			}
			if (ignoreLoadingNextTaint) {
				super.visitTypeInsn(opcode, type);
				if (doIOR)
					super.visitInsn(IOR);
				return;
			}
			{
				if (topStackElCarriesTaints()) {
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
				super.visitTypeInsn(opcode, type);
				if (doIOR)
					super.visitInsn(IOR);
				super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
				super.visitInsn(SWAP);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Pre: A Post: TA A
	 * 
	 * @param type
	 */
	protected void retrieveTaintedArray(String type) {
		//A
		Label isNull = new Label();
		Label isDone = new Label();

		FrameNode fn = getCurrentFrameNode();

		super.visitInsn(DUP);
		if(!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitJumpInsn(IFNULL, isNull);
		if(!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);

		//		System.out.println("unbox: " + onStack + " type passed is " + type);

		Class boxType = MultiDTaintedArray.getClassForComponentType(Type.getType(type).getElementType().getSort());
		super.visitTypeInsn(CHECKCAST, Type.getInternalName(boxType));

		super.visitInsn(DUP);

		Type arrayDesc = Type.getType(type);
		//		System.out.println("Get tainted array from " + arrayDesc);
		//A A
		if(!Configuration.MULTI_TAINTING)
		super.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "[I");
		else
		{
			super.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "[Ljava/lang/Object;");
			super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
		}
		//A TA
		super.visitInsn(SWAP);
		super.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "val", type);
		FrameNode fn2 = getCurrentFrameNode();
		super.visitJumpInsn(GOTO, isDone);
		super.visitLabel(isNull);
		fn.accept(this);
		super.visitInsn(ACONST_NULL);
		if (arrayDesc.getElementType().getSort() == Type.OBJECT)
			super.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
		else
			super.visitTypeInsn(CHECKCAST, Type.getType(TaintUtils.getShadowTaintType(arrayDesc.getDescriptor())).getInternalName());
		super.visitInsn(SWAP);
		super.visitTypeInsn(CHECKCAST, type);
		super.visitLabel(isDone);
		fn2.accept(this);
	}

	/**
	 * 
	 * @param descOfDest
	 */
	protected void registerTaintedArray(String descOfDest) {
		Type onStack = Type.getType(descOfDest);//getTopOfStackType();
		//TA A
		Type wrapperType = Type.getType(MultiDTaintedArray.getClassForComponentType(onStack.getElementType().getSort()));
		//		System.out.println("zzzNEW " + wrapperType);
		Label isNull = new Label();
		FrameNode fn = getCurrentFrameNode();

		if (!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitInsn(DUP);
		super.visitJumpInsn(IFNULL, isNull);
		if (!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitTypeInsn(NEW, wrapperType.getInternalName());
		//TA A N
		super.visitInsn(DUP_X2);
		super.visitInsn(DUP_X2);
		super.visitInsn(POP);
		//N N TA A 
		if (!Configuration.MULTI_TAINTING)
			super.visitMethodInsn(INVOKESPECIAL, wrapperType.getInternalName(), "<init>", "([I" + onStack.getDescriptor() + ")V", false);
		else
			super.visitMethodInsn(INVOKESPECIAL, wrapperType.getInternalName(), "<init>", "([Ljava/lang/Object;" + onStack.getDescriptor() + ")V", false);
		Label isDone = new Label();
		FrameNode fn2 = getCurrentFrameNode();
		super.visitJumpInsn(GOTO, isDone);
		super.visitLabel(isNull);
		fn.accept(this);
		super.visitInsn(POP);
		super.visitLabel(isDone);
		fn2.stack.set(fn2.stack.size()-1, "java/lang/Object");
		fn2.accept(this);

		//A
	}

	protected void unwrapTaintedInt() {
		super.visitInsn(DUP);
		getTaintFieldOfBoxedType(Configuration.TAINTED_INT_INTERNAL_NAME);
		super.swap();
		super.visitFieldInsn(GETFIELD, Configuration.TAINTED_INT_INTERNAL_NAME, "val", "I");
	}

	/**
	 * Will insert a NULL after the nth element from the top of the stack
	 * 
	 * @param n
	 */
	void insertNullAt(int n) {
		switch (n) {
		case 0:
			super.visitInsn(ACONST_NULL);
			break;
		case 1:
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			break;
		case 2:
			super.visitInsn(ACONST_NULL);
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			break;
		default:
			LocalVariableNode[] d = storeToLocals(n);
			super.visitInsn(ACONST_NULL);
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
	}

	/**
	 * Pop at n means pop the nth element down from the top (pop the top is n=0)
	 * 
	 * @param n
	 */
	void popAt(int n) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP)
				super.visitInsn(POP2);
			else
				super.visitInsn(POP);
			break;
		case 1:
			top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				Object second = analyzer.stack.get(analyzer.stack.size() - 3);
				//				System.out.println("Second is " + second);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					super.visitInsn(POP2);
				} else {
					//V VV
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				}
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					super.visitInsn(POP2);
				} else {
					//V V
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n);

			//			System.out.println("POST load the top " + n + ":" + analyzer.stack);

			super.visitInsn(POP);
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	/**
	 * Store at n means pop the nth element down from the top and store it to
	 * our arraystore (pop the top is n=0)
	 * 
	 * @param n
	 */
	void storeTaintArrayAt(int n, String descAtDest) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP)
				super.visitInsn(POP2);
			else
				super.visitInsn(POP);
			throw new IllegalStateException("Not supposed to ever pop the top like this");
			//			break;
		case 1:
			top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				throw new IllegalStateException("Not supposed to ever pop the top like this");
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					throw new IllegalStateException("Not supposed to ever pop the top like this");
				} else {
					//V V
					registerTaintedArray(descAtDest);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n - 1);

			//						System.out.println("POST load the top " + n + ":" + analyzer.stack);
			registerTaintedArray(descAtDest);
			for (int i = n - 2; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	void unboxTaintArrayAt(int n, String descAtDest) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " unbox AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			retrieveTaintedArray(descAtDest);
		case 1:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				throw new IllegalStateException("Not supposed to ever pop the top like this");
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					throw new IllegalStateException("Not supposed to ever pop the top like this");
				} else {
					//V
					retrieveTaintedArray(descAtDest);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n - 1);

			//						System.out.println("POST load the top " + n + ":" + analyzer.stack);
			retrieveTaintedArray(descAtDest);
			for (int i = n - 2; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
//		System.out.println("INVOKEDYNAMIC" +name+ desc);
//		System.out.println(analyzer.stack);
		desc = TaintUtils.remapMethodDesc(desc);
		if(Configuration.IMPLICIT_TRACKING)
		{
			super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
		}
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}
	static final boolean isInternalTaintingMethod(String owner)
	{
		return owner.equals(Type.getInternalName(Tainter.class)) || owner.equals(Type.getInternalName(MultiTainter.class));
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		boolean isPreAllocedReturnType = TaintUtils.isPreAllocReturnType(desc);
		if (Instrumenter.isClassWithHashmapTag(owner) && name.equals("valueOf")) {
			Type[] args = Type.getArgumentTypes(desc);
			if (args[0].getSort() != Type.OBJECT) {
				if(!Configuration.MULTI_TAINTING)
				{
				owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
				desc = "(I" + desc.substring(1);
				}
				else
				{
					owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
					desc = "(Ljava/lang/Object;" + desc.substring(1);
				}
				super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
				return;
			}
		} else if (Instrumenter.isClassWithHashmapTag(owner) && (name.equals("byteValue") || name.equals("booleanValue") || name.equals("charValue") || name.equals("shortValue"))) {
			Type returnType = Type.getReturnType(desc);
			Type boxedReturn = TaintUtils.getContainerReturnType(returnType);
			desc = "(L" + owner + ";)" + boxedReturn.getDescriptor();
			if(!Configuration.MULTI_TAINTING)
				owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
			else
				owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
			super.visitInsn(DUP);
			getTaintFieldOfBoxedType(boxedReturn.getInternalName());
			super.visitInsn(SWAP);
			super.visitFieldInsn(GETFIELD, boxedReturn.getInternalName(), "val", returnType.getDescriptor());
			return;
		}

		Type ownerType = Type.getObjectType(owner);
		if (opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
			//			System.out.println("got to change the owner on primitive array call from " +owner+" to " + MultiDTaintedArray.getTypeForType(ownerType));
			owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
		}
		//		Type ownerType = Type.getType(owner + ";");
		boolean isCallToPrimitiveArrayClone = opcode == INVOKEVIRTUAL && desc.equals("()Ljava/lang/Object;") && name.equals("clone") && getTopOfStackType().getSort() == Type.ARRAY
				&& getTopOfStackType().getElementType().getSort() != Type.OBJECT;
		//When you call primitive array clone, we should first clone the taint array, then register that taint array to the cloned object after calling clone
		Type primitiveArrayType = null;
		if (isCallToPrimitiveArrayClone) {
			//			System.out.println("Call to primitive array clone: " + analyzer.stack + " " + owner);
			primitiveArrayType = getTopOfStackType();
			//TA A
			super.visitInsn(SWAP);
			super.visitMethodInsn(opcode, "java/lang/Object", "clone", "()Ljava/lang/Object;",false);
			super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
			//A TA'
			super.visitInsn(SWAP);
			//todo
		}
		if ((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager"))&& name.equals("arraycopy")) {
			if(Instrumenter.IS_KAFFE_INST)
				name = "arraycopyVM";
			else if(Instrumenter.IS_HARMONY_INST)
				name= "arraycopyHarmony";
			owner = Type.getInternalName(TaintUtils.class);
			//We have several scenarios here. src/dest may or may not have shadow arrays on the stack
			boolean destIsPrimitve = false;
			Type destType = getStackTypeAtOffset(4);
			destIsPrimitve = destType.getSort() != Type.OBJECT && destType.getElementType().getSort() != Type.OBJECT;
			int srcOffset = 7;
			if (destIsPrimitve)
				srcOffset++;
			//			System.out.println("Sysarracopy with " + analyzer.stack);
			Type srcType = getStackTypeAtOffset(srcOffset);
			boolean srcIsPrimitive = srcType.getSort() != Type.OBJECT && srcType.getElementType().getSort() != Type.OBJECT;
			if(!Configuration.MULTI_TAINTING)
			{
				if (srcIsPrimitive) {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
						if(Configuration.IMPLICIT_TRACKING)
							name = "arraycopyControlTrack";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;IIII)V";
					}
				} else {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
					} else {
						desc = "(Ljava/lang/Object;IILjava/lang/Object;IIII)V";

					}
				}
			}
			else
			{
				if (srcIsPrimitive) {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
						if(Configuration.IMPLICIT_TRACKING)
							name = "arraycopyControlTrack";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
					}
				} else {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";

					}
				}
			}
		}
		if (owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints") && !desc.equals("(I)V") && !owner.endsWith("Tainter")) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		//to reduce how much we need to wrap, we will only rename methods that actually have a different descriptor
		boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);
		if(isCallToPrimitiveArrayClone)
		{
			hasNewName = false;
		}
		if (((Instrumenter.isIgnoredClass(owner) || Instrumenter.isIgnoredMethod(owner, name, desc)) && !owner.startsWith("edu/columbia/cs/psl/phosphor/runtime"))
				|| (opcode == INVOKEINTERFACE && Instrumenter.isAnnotation(owner))) {
			Type[] args = Type.getArgumentTypes(desc);
			if (TaintUtils.DEBUG_CALLS) {
				System.out.println("Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);
			}
			int argsSize = 0;
			for (int i = 0; i < args.length; i++) {
				argsSize += args[args.length - i - 1].getSize();
								if (TaintUtils.DEBUG_CALLS)
				System.out.println(i + ", " + analyzer.stack.get(analyzer.stack.size() - argsSize) + " " + args[args.length - i - 1]);
				if(args[args.length - i - 1].getSort() == Type.ARRAY && args[args.length - i - 1].getElementType().getSort() != Type.OBJECT && args[args.length - i - 1].getDimensions() > 1)
				{
					if(0 ==i)
					{
						super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)), "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;",false);
					}
					else
					{
						throw new IllegalArgumentException("Can't unbox ");
					}
//					unboxTaintArrayAt(i+1, args[args.length - i - 1].getDescriptor());
				}
				else if (isPrimitiveType(args[args.length - i - 1])
						|| (args[args.length - i - 1].equals(Type.getType(Object.class)) && isPrimitiveStackType(analyzer.stack.get(analyzer.stack.size() - argsSize)))) {
					//Wooahhh let's do nothing here if it's a null on the stack
					if (isPrimitiveType(args[args.length - i - 1]) && analyzer.stack.get(analyzer.stack.size() - argsSize) == Opcodes.NULL) {

					} else
						popAt(i + 1);
				}
			}
//			System.out.println("After modifying, Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);

			boolean isCalledOnAPrimitiveArrayType = false;
			if (opcode == INVOKEVIRTUAL) {
				Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("CALLEE IS " + callee);
				if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT)
					isCalledOnAPrimitiveArrayType = true;
			}

			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			if (isCallToPrimitiveArrayClone) {
				//Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
				//TA A
				super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
				registerTaintedArray(primitiveArrayType.getDescriptor());
			} else if (isCalledOnAPrimitiveArrayType) {
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("Post invoke stack: " + analyzer.stack);
				if (Type.getReturnType(desc).getSort() == Type.VOID) {
					super.visitInsn(POP);
				} else if (analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
					//this is so dumb that it's an array type.
					super.visitInsn(SWAP);
					super.visitInsn(POP); //This is the case that we are calling a method on a primitive array type so need to pop the taint
				}
			}

			Type returnType = Type.getReturnType(desc);
			if (isPrimitiveType(returnType)) {
				if (returnType.getSort() == Type.ARRAY) {
					generateEmptyTaintArray(returnType.getDescriptor());
				} else if (returnType.getSize() == 2) {
					generateUnconstrainedTaint(0);
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				} else {
					generateUnconstrainedTaint(0);
					super.visitInsn(SWAP);
				}
			}
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
			return;
		}
		String newDesc = TaintUtils.remapMethodDesc(desc);
		if(Configuration.IMPLICIT_TRACKING)
		{
			if(isInternalTaintingMethod(owner) || owner.startsWith("[")){
				newDesc = newDesc.replace(Type.getDescriptor(ControlTaintTagStack.class), "");
			}
			else
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
			if(owner.startsWith("["))
				hasNewName = false;
		}
		//		boolean pushNull = false;
		if (name.equals("<init>") && !newDesc.equals(desc)) {
			//Add the taint sentinel to the desc
			super.visitInsn(ACONST_NULL);
			newDesc = newDesc.substring(0, newDesc.indexOf(")")) + Type.getDescriptor(TaintSentinel.class) + ")" + Type.getReturnType(newDesc).getDescriptor();
		}
		if (isPreAllocedReturnType) {
//			System.out.println("\t\tAdding stuff for " + owner + "." + name + newDesc);
			Type t = Type.getReturnType(newDesc);
			newDesc = newDesc.substring(0, newDesc.indexOf(")")) + t.getDescriptor() + ")" + t.getDescriptor();
			super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(t));
//			System.out.println("n: " + lvs.getPreAllocedReturnTypeVar(t));
//			System.out.println("Analyzer lcoal is: " + analyzer.locals.get(lvs.getPreAllocedReturnTypeVar(t)));
		}
		Type origReturnType = Type.getReturnType(desc);
		Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("Remapped call from " + owner + "." + name + desc + " to " + owner + "." + name + newDesc);
		if (!name.contains("<") && hasNewName)
			name += TaintUtils.METHOD_SUFFIX;
		if (TaintUtils.DEBUG_CALLS) {
			System.out.println("Calling w/ stack: " + analyzer.stack);
		}

		//if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
		//for the taint for that array
		Type[] args = Type.getArgumentTypes(newDesc);
		Type[] argsInReverse = new Type[args.length];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argsInReverse[args.length - i - 1] = args[i];
			argsSize += args[i].getSize();
		}
		int i = 1;
		int n = 1;
		boolean ignoreNext = false;

//										System.out.println("12dw23 Calling "+owner+"."+name+newDesc + "with " + analyzer.stack);
		for (Type t : argsInReverse) {
			if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
				i++;

			//			if(ignoreNext)
			//				System.out.println("ignore next i");
			Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
//						System.out.println(name+ " ONStack: " + onStack + " and t = " + t);

			if (!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
//								System.out.println("t " + t + " and " + analyzer.stack.get(analyzer.stack.size() - i) + " j23oij4oi23");

				//Need to check to see if there's a null on the stack in this position
				if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.NULL) {
					if (TaintUtils.DEBUG_CALLS)
						System.err.println("Adding a null in call at " + n);
					insertNullAt(n);
				} else if (onStack.getSort() == Type.OBJECT) {
					//Unbox this
					unboxTaintArrayAt(n, t.getDescriptor());
				}
			} else if (!ignoreNext && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
				//There is an extra taint on the stack at this position
				if (TaintUtils.DEBUG_CALLS)
					System.err.println("removing taint array in call at " + n);
				storeTaintArrayAt(n, onStack.getDescriptor());
			}
			if ((t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) || (t.getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC)))
				ignoreNext = !ignoreNext;
			n++;
			i++;
		}

		//		System.out.println("Args size: " + argsSize + " nargs " + args.length);
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("No more changes: calling " + owner + "." + name + newDesc + " w/ stack: " + analyzer.stack);

		boolean isCalledOnAPrimitiveArrayType = false;
		if (opcode == INVOKEVIRTUAL) {
			if (analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null)
				System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
			Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("CALLEE IS " + callee);
			if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT)
				isCalledOnAPrimitiveArrayType = true;
		}

		super.visitMethodInsn(opcode, owner, name, newDesc, itfc);
		//		System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//		System.out.println("Now: " + analyzer.stack);
		if (isCallToPrimitiveArrayClone) {
			//Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
			//TA A
			//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
			super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
			//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
			registerTaintedArray(primitiveArrayType.getDescriptor());
		} else if (isCalledOnAPrimitiveArrayType) {
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("Post invoke stack: " + analyzer.stack);
			if (Type.getReturnType(desc).getSort() == Type.VOID) {
				super.visitInsn(POP);
			} else if (analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
				//this is so dumb that it's an array type.
				super.visitInsn(SWAP);
				super.visitInsn(POP);
			}

		}
//		System.out.println("after: " + analyzer.stack);

		if (dontUnboxTaints) {
			dontUnboxTaints = false;
			return;
		}
		String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
		if (taintType != null) {
			super.visitInsn(DUP);
			if (!Configuration.MULTI_TAINTING) {
				String taintTypeRaw = Configuration.TAINT_TAG_DESC;
				if (Type.getReturnType(desc).getSort() == Type.ARRAY)
					taintTypeRaw = Configuration.TAINT_TAG_ARRAYDESC;
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintTypeRaw);
			} else {
				String taintTypeRaw = Configuration.TAINT_TAG_INTERNAL_NAME;
				String fieldType = "Ljava/lang/Object;";
				if (Type.getReturnType(desc).getSort() == Type.ARRAY) {
					taintTypeRaw = Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME;
					fieldType = "[Ljava/lang/Object;";
				}
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", fieldType);
				super.visitTypeInsn(CHECKCAST, taintTypeRaw);
			}

			super.visitInsn(SWAP);
			if (returnType.equals(Type.getType(TaintedMisc.class))) {
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", "Ljava/lang/Object;");
				super.visitTypeInsn(CHECKCAST, origReturnType.getDescriptor());
			} else
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
//		if (TaintUtils.DEBUG_FRAMES)
//			System.out.println("TMV sees frame: "
//+type+ Arrays.toString(local) 
//+ ", stack " + Arrays.toString(stack));
//			new Exception().printStackTrace();
		super.visitFrame(type, nLocal, local, nStack, stack);

		varsNeverToForceBox = new HashSet<Integer>();
	}

	//	private int[] bbsToAddACONST_NULLto;
	//	private int[] bbsToAddChecktypeObjectto;
	private PrimitiveArrayAnalyzer arrayAnalyzer;

	boolean isTaintlessArrayStore = false;
	boolean isIgnoreAllInstrumenting = false;
	boolean isRawInsns = false;
	boolean dontUnboxTaints;

	boolean nextLoadIsTainted = false;
	boolean nextLoadIsNotTainted = false;
	boolean ignoreLoadingNextTaint = false;

//	private Object getTopIfConstant() {
//		if (getTopOfStackObject() == Opcodes.TOP) {
//			return analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 2);
//		} else
//			return analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 1);
//	}

	public boolean topHas0Taint() {
		if (getTopOfStackObject() == Opcodes.TOP)
			return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 3));
		else
			return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 2));
	}

	public boolean secondHas0Taint() {
		int offset = 2;
		if (getTopOfStackObject() == Opcodes.TOP)
			offset++;
		if (getStackElementSize(offset) == Opcodes.TOP)
			offset++;
		offset++;
		return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - offset));
	}

	boolean isIgnoreEverything = false;
	@Override
	public void visitInsn(int opcode) {
//		System.out.println(name + Printer.OPCODES[opcode] + " " + analyzer.stack + " raw? " + isRawInsns);
//		System.out.println(name+Printer.OPCODES[opcode]);
		if (opcode == TaintUtils.NEXTLOAD_IS_NOT_TAINTED) {
			nextLoadIsNotTainted = true;
			return;
		}
		if (opcode == TaintUtils.NEXTLOAD_IS_TAINTED) {
			nextLoadIsTainted = true;
			return;
		}
		if (opcode == TaintUtils.GENERATETAINTANDSWAP) {
			Type onStack = getTopOfStackType();
			if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
				generateEmptyTaintArray(onStack.getDescriptor());
			} else {
				super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
				if (onStack.getSize() == 1) {
					super.visitInsn(SWAP);
				} else {
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
			}
			return;
		}
		if (opcode == TaintUtils.RAW_INSN) {
			isRawInsns = !isRawInsns;
			return;
		}
		if (opcode == TaintUtils.DONT_LOAD_TAINT) {
			if(isIgnoreEverything)
				return;
			ignoreLoadingNextTaint = !ignoreLoadingNextTaint;
			//						if(ignoreLoadingNextTaint)
			//						{
			//							System.out.println(name+"Start: ignore taints");
			//						}
			//						else
			//							System.out.println("End: ignore taints");
			super.visitInsn(opcode);
			return;
		}
		if (opcode == TaintUtils.IGNORE_EVERYTHING) {
			//						System.err.println("VisitInsn is ignoreverything! in  " + name);
			isIgnoreAllInstrumenting = !isIgnoreAllInstrumenting;
			isIgnoreEverything = !isIgnoreEverything;
			super.visitInsn(opcode);
			return;
		}
		if (opcode == TaintUtils.NO_TAINT_UNBOX) {
			dontUnboxTaints = true;
			return;
		}
		if (opcode == TaintUtils.NO_TAINT_STORE_INSN) {
			isTaintlessArrayStore = true;
			return;
		}
		if (ignoreLoadingNextTaint) {

			super.visitInsn(opcode);
			if (opcode == Opcodes.AALOAD) {
				//Type on stack:
				Object arrayType = analyzer.stack.get(analyzer.stack.size() - 1);

				Type t = getTypeForStackType(arrayType);
				if (t.getSort() == Type.OBJECT && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/multid/MultiDTainted")) {
					try {
						super.visitFieldInsn(GETFIELD, t.getInternalName(), "val", "[" + MultiDTaintedArray.getPrimitiveTypeForWrapper(t.getInternalName()).getDescriptor());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return;
		} else if (isIgnoreAllInstrumenting || isRawInsns) {
			super.visitInsn(opcode);
			return;
		}

		if(Configuration.IMPLICIT_TRACKING)
		{
			switch (opcode) {
			case IRETURN:
			case IASTORE:
			case FRETURN:
			case FASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				super.visitInsn(SWAP);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				if (!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else {
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
							"("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(SWAP);
				break;
			case DASTORE:
			case LASTORE:
			case DRETURN:
			case LRETURN:
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				if (!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else {
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
							"("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				break;
			case AASTORE:
			case ARETURN:
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);						
				break;
			}
		}
		
		switch (opcode) {
		case Opcodes.NOP:
			super.visitInsn(opcode);
			break;
		case Opcodes.ACONST_NULL:
			//			System.out.println("NULL at" + curLabel);
			//			System.out.println("NULL IN " + curLabel);
			super.visitInsn(opcode);
			break;
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			//the constant taint registration happens in constantvalueconstraintmv, because it's
			//optimized to not set the taint if the value is used immediately for an array index
			super.visitInsn(opcode);
			return;
		case Opcodes.LALOAD:
		case Opcodes.DALOAD:
		case Opcodes.IALOAD:
		case Opcodes.FALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
			String elType = null;
			switch (opcode) {
			case Opcodes.LALOAD:
				elType = "J";
				break;
			case Opcodes.DALOAD:
				elType = "D";
				break;
			case Opcodes.IALOAD:
				elType = "I";
				break;
			case Opcodes.FALOAD:
				elType = "F";
				break;
			case Opcodes.BALOAD:
				//				System.out.println("BALOAD " + analyzer.stack);
				if (analyzer.stack.get(analyzer.stack.size() - 3) instanceof Integer)
					elType = "B";
				else
					elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - 3)).getElementType().getDescriptor();
				break;
			case Opcodes.CALOAD:
				elType = "C";
				break;
			case Opcodes.SALOAD:
				elType = "S";
				break;
			}
			if (TaintUtils.DEBUG_FRAMES)
				System.out.println("PRE XALOAD " + elType + ": " + analyzer.stack + "; " + analyzer.locals);
			{
				//TA A T I
				super.visitInsn(SWAP);
				super.visitInsn(POP);
				//TA A I
				super.visitInsn(DUP_X1);
				//TA I A I
				super.visitInsn(opcode);
				//TA I V V?
				if (opcode == LALOAD || opcode == DALOAD) {
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					//V V TA I
					super.visitInsn(Configuration.TAINT_ARRAY_LOAD_OPCODE);
					//V V T
					super.visitInsn(DUP_X2);
					// T V V T
					super.visitInsn(POP);
				} else {
					//TA I V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					//V TA I
					super.visitInsn(Configuration.TAINT_ARRAY_LOAD_OPCODE);
					super.visitInsn(SWAP);
				}
			}
			break;
		case Opcodes.AALOAD:
			//need to drop the taint on the index
			//?TA A T I

			super.visitInsn(SWAP);
			super.visitInsn(POP);

			//?TA A I
			Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
			Type t = getTypeForStackType(arrayType);
			if (t.getDimensions() == 1 && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/multid/MultiDTainted")) {
				//				System.out.println("it's a multi array in disguise!!!");
				super.visitInsn(opcode);
				try {
					retrieveTaintedArray("[" + (MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", ".")))));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//				System.out.println("AALOAD " + analyzer.stack);

			} else
				super.visitInsn(opcode);
			break;
		case Opcodes.AASTORE:
//			if(className.contains("ArrayBuilder"))
//			{
//				System.out.println(className+"."+name+" - AASTORE: " + this.analyzer.stack);
//			}
			if (isTaintlessArrayStore) {
				isTaintlessArrayStore = false;
				super.visitInsn(opcode);
				return;
			}
			//need to drop the taint on the index
			// A T I V
			arrayType = analyzer.stack.get(analyzer.stack.size() - 1);
			t = getTypeForStackType(arrayType);
			Type taintArrayType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
//									System.out.println("AASTORE of " + arrayType + " ONTO ...");
//									System.out.println(analyzer.stack);
			//better look to see if we are storing a NULL into a multidemnsional array...
			if (arrayType == Opcodes.NULL) {
				Object theArray = analyzer.stack.get(analyzer.stack.size() - 4);
				t = getTypeForStackType(theArray);
				//				System.out.println(theArray);
				if (theArray != Opcodes.NULL && t.getElementType().getSort() != Type.OBJECT)
					super.visitInsn(ACONST_NULL);
			}

			if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
				if (TaintUtils.DEBUG_FRAMES)
					System.out.println("PRE-AASTORE w/ mutlid array " + t + ": " + analyzer.stack);
				//We don't know if we're storing it in a multi-d array of this type, or of type Object[]. If it's
				//type Object[] then we will not be storing the taint! Let's find out what the array beneath us is.
				if (analyzer.stack.size() >= 5) {
					Object wayBelow = analyzer.stack.get(analyzer.stack.size() - 5);
					Type wayBelowType = getTypeForStackType(wayBelow);
					if (TaintUtils.DEBUG_FRAMES)
						System.out.println("Type we are storing into: " + wayBelowType + " --- " + analyzer.stack);
					if (wayBelowType.getSort() == Type.OBJECT || wayBelowType.getElementType().getSort() == Type.OBJECT) {
						//In this case, we want to drop the taint on the index and the taint array too.
						//Also, since there's a taint, we need to wrap that up.
						//And right here, it looks like:
						//A TI I T V
						super.visitInsn(DUP2_X2);
						super.visitInsn(POP2);
						//A T V TI I
						super.visitInsn(SWAP);
						super.visitInsn(POP);
						//A T V I
						super.visitInsn(DUP_X2);
						super.visitInsn(POP);
						//A I T V
//						System.out.println("PRe register " + analyzer.stack);
						registerTaintedArray(getTopOfStackType().getDescriptor());
						//						System.out.println(analyzer.stack);
						//A I V
						super.visitInsn(AASTORE);
						if (TaintUtils.DEBUG_FRAMES)
							System.out.println("POST-AASTORE w/ mutlid array " + t + ": " + analyzer.stack);
						//						throw new IllegalStateException(analyzer.stack.toString());
						return;
					}
				}
				//this is a multi-d array. make it work, even if it's nasty.
				//				System.out.println("PRE-AASTORE w/ multid array: " + t + " : " + analyzer.stack);
				super.visitInsn(SWAP);
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				int tmpLocal = lvs.getTmpLV();
				super.visitVarInsn(ASTORE, tmpLocal);
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitInsn(POP);
				super.visitInsn(SWAP);
				super.visitInsn(DUP_X2);
				super.visitInsn(SWAP);
				super.visitInsn(AASTORE);
				super.visitVarInsn(ALOAD, tmpLocal);
				lvs.freeTmpLV(tmpLocal);
				super.visitInsn(opcode);
				if (TaintUtils.DEBUG_FRAMES)
					System.out.println("POST-AASTORE w/ mutlid array " + t + ": " + analyzer.stack);

			} else {
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitInsn(POP);
				super.visitInsn(opcode);
			}
			break;
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			//	public static void XASTORE(int[] TArray, int[] Array, int idxTaint, int idx, int valTaint, int val) {
			int valStoreOpcode;
			int valLoadOpcode;

			switch (opcode) {
			case Opcodes.LASTORE:
				valStoreOpcode = LSTORE;
				valLoadOpcode = LLOAD;
				elType = "J";
				break;
			case Opcodes.DASTORE:
				valStoreOpcode = DSTORE;
				valLoadOpcode = DLOAD;
				elType = "D";
				break;
			case Opcodes.IASTORE:
				valStoreOpcode = ISTORE;
				valLoadOpcode = ILOAD;
				elType = "I";
				break;
			case Opcodes.FASTORE:
				valStoreOpcode = FSTORE;
				valLoadOpcode = FLOAD;
				elType = "F";
				break;
			case Opcodes.BASTORE:
				if (analyzer.stack.get(analyzer.stack.size() - (isTaintlessArrayStore ? 4 : 5)) == Opcodes.INTEGER)
					elType = "B";
				else
					elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - (isTaintlessArrayStore ? 4 : 5))).getElementType().getDescriptor();
				valStoreOpcode = ISTORE;
				valLoadOpcode = ILOAD;
				break;
			case Opcodes.CASTORE:
				valLoadOpcode = ILOAD;
				valStoreOpcode = ISTORE;
				elType = "C";
				break;
			case Opcodes.SASTORE:
				valLoadOpcode = ILOAD;
				valStoreOpcode = ISTORE;
				elType = "S";
				break;
			default:
				valLoadOpcode = -1;
				valStoreOpcode = -1;
				elType = null;
			}
			if (TaintUtils.DEBUG_FRAMES) {
				System.out.println("XASTORE>>>" + elType);
				System.out.println(analyzer.stack);
			}

			 {
				//int[] TArray, X[] Array, int idxTaint, int idx, int valTaint, X val
				//TA A IT I VT V
				int tmp1, tmp2;
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				tmp1 = lvs.getTmpLV(getTopOfStackType());
				super.visitVarInsn(valStoreOpcode, tmp1);
				tmp2 = lvs.getTmpLV(getTopOfStackType());
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, tmp2);
				if (isTaintlessArrayStore) {
					isTaintlessArrayStore = false;
				} else {
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
				//tarray[] array[] idx
				super.visitInsn(DUP_X1);
				//tarray idx array idx
				super.visitVarInsn(valLoadOpcode, tmp1);
				super.visitInsn(opcode);
				super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, tmp2);
				super.visitInsn(Configuration.TAINT_ARRAY_STORE_OPCODE);
				lvs.freeTmpLV(tmp1);
				lvs.freeTmpLV(tmp2);
			}
			break;
		case Opcodes.POP:
		case Opcodes.POP2:
			Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			super.visitInsn(opcode);
			if (isPrimitiveStackType(topOfStack)) {
				super.visitInsn(Opcodes.POP);
			}
			return;
		case Opcodes.DUP:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			//			System.out.println(analyzer.stack);
			if (TaintUtils.DEBUG_FRAMES) {
				System.out.println("DUP: " + analyzer.stack);
			}
			if (isPrimitiveStackType(topOfStack)) {
				super.visitInsn(Opcodes.DUP2);
			} else
				super.visitInsn(DUP);
			break;
		case Opcodes.DUP2:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (TaintUtils.DEBUG_FRAMES)
				System.out.println("PRE dup2 " + analyzer.stack);
			if (isPrimitiveStackType(topOfStack)) {
				//Dup the 3rd element down in the stack
				if (getStackElementSize(topOfStack) == 2) {
					//This is the easiest case, there's a 2word elmenent we dup, we know there is only 1 taint
					// T V V
					super.visitInsn(DUP2_X1);
					// VV T VV
					super.visitInsn(POP2);
					// VV T
					super.visitInsn(DUP);
					//VV TT
					int taint = lvs.getTmpLV();
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, taint);
					//VV T
					super.visitInsn(DUP_X2);
					//T VV T
					super.visitInsn(POP);
					//TVV
					super.visitInsn(DUP2);
					//TVVVV
					super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, taint);
					lvs.freeTmpLV(taint);
					//TVV VVT
					super.visitInsn(DUP_X2);
					//TVV TVVT
					super.visitInsn(POP);
					//TVV TVV
					//					throw new IllegalArgumentException("Untested, proceed with care");
				} else {
					Object secondOnStack = analyzer.stack.get(analyzer.stack.size() - 3);

					/**
					 * V1 T1 V2 T2 V1 T1 V2 T2
					 */
					//We might also need to dup the second taint...
					Type topType = getTypeForStackType(topOfStack);
					Type secondType = getTypeForStackType(secondOnStack);
					if (TaintUtils.DEBUG_FRAMES)
						System.out.println("DUP2...." + topType + "," + secondType);
					int top = lvs.getTmpLV();
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					super.visitVarInsn(topType.getOpcode(ISTORE), top);
					int second = lvs.getTmpLV();
					super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, second);
					if (isPrimitiveStackType(secondOnStack)) {
						//Also need to dup the second one.
						super.visitInsn(DUP2);
					} else
						super.visitInsn(DUP);
					super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, second);
					super.visitVarInsn(topType.getOpcode(ILOAD), top);
					if (isPrimitiveStackType(secondOnStack)) {
						super.visitInsn(DUP2_X2);
					} else {
						super.visitInsn(DUP2_X1);
					}
					lvs.freeTmpLV(top);
					lvs.freeTmpLV(second);
				}
			} else
				super.visitInsn(opcode);
			if (TaintUtils.DEBUG_FRAMES)
				System.out.println("POST dup2 " + analyzer.stack);
			break;
		case Opcodes.DUP_X1:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (isPrimitiveStackType(topOfStack)) {
				//There is a 1 word element at the top of the stack, we want to dup
				//it and it's taint to go one under so that it's
				//T V X T V
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
				if (isPrimitiveStackType(underThisOne)) {
					// X X T V -> T V X X T V
					super.visitInsn(DUP2_X2);
				} else {
					//X T V -> T V X TV
					super.visitInsn(DUP2_X1);
				}
			} else {
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 2);
				if (isPrimitiveStackType(underThisOne)) {
					// X X V -> V X X V
					super.visitInsn(DUP_X2);
				} else {
					//X V -> V X V
					super.visitInsn(DUP_X1);
				}
			}
			break;
		case Opcodes.DUP_X2:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (isPrimitiveStackType(topOfStack)) {
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
				if (isPrimitiveStackType(underThisOne)) {
					if (getStackElementSize(underThisOne) == 2) {
						//Dup the top 2 elements to be under the 3 beneath.
						DUPN_XU(2, 3);
					} else {
						//top el is 2, next is 2
						Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 5);
						if (isPrimitiveStackType(threeUnder)) {
							//Dup the top 2 under the next 4
							DUPN_XU(2, 4);
						} else {
							//Dup the top 2 under the next 3
							DUPN_XU(2, 3);
						}
					}
				} else {//top is primitive, second is not
					Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 4);
					if (isPrimitiveStackType(threeUnder)) {
						//Dup the top 2 under the next 3
						DUPN_XU(2, 3);
					} else {
						//Dup the top 2 under the next 2
						super.visitInsn(DUP2_X2);
					}
				}
			} else { //top is not primitive
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 2);
				if (isPrimitiveStackType(underThisOne)) {
					if (getStackElementSize(underThisOne) == 2) {
						//Dup the top 1 element to be under the 3 beneath.
						LocalVariableNode d[] = storeToLocals(3);
						loadLV(0, d);
						loadLV(2, d);
						loadLV(1, d);
						loadLV(0, d);
						freeLVs(d);

					} else {
						Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isPrimitiveStackType(threeUnder)) {
							//Dup the top 1 under the next 4
							DUPN_XU(1, 4);
						} else {
							//Dup the top 1 under the next 3
							DUPN_XU(1, 3);
						}
					}
				} else {//top is not primitive, second is not
					Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 3);
					if (isPrimitiveStackType(threeUnder)) {
						//Dup the top 1 under the next 3
						DUPN_XU(1, 3);
					} else {
						//Dup the top 1 under the next 2
						super.visitInsn(DUP_X2);
					}
				}
			}
			break;
		case Opcodes.DUP2_X1:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (isPrimitiveStackType(topOfStack)) {
				if (getStackElementSize(topOfStack) == 2) {
					//Have two-word el + 1 word taint on top
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 4);
					if (isPrimitiveStackType(underThisOne)) {
						//Dup the top three words to be under the 2 words beneath them
						DUPN_XU(2, 2);
					} else {
						//Dup the top three words to be under the word beneath them
						DUPN_XU(2, 1);
					}
				} else // top is 1 word, primitive
				{
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
					if (isPrimitiveStackType(underThisOne)) {
						//top is primitive, second is primitive
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
						if (isPrimitiveStackType(threeDown)) {
							// Dup the top four words to be under the 2 beneath them
							DUPN_XU(4, 2);
						} else {
							// dup the top four words to be under the 1 beneath
							DUPN_XU(4, 1);
						}
					} else {
						//top is primitive, second is not
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isPrimitiveStackType(threeDown)) {
							// TV  VTV
							// Dup the top three words to be under the 2 beneath
							DUPN_XU(3, 2);
						} else {
							// dup the top three words to be under the 1 beneath
							DUPN_XU(3, 1);
						}
					}
				}
			} else {
				//top is not primitive. must be one word.
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 2);
				if (isPrimitiveStackType(underThisOne)) {
					Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
					if (isPrimitiveStackType(threeDown)) {
						// Dup the top 3 words to be under the 2 beneath
						DUPN_XU(3, 2);
					} else {
						// dup the top 3 words to be under the 1 beneath
						DUPN_XU(3, 1);
					}
				} else {
					Object threeDown = analyzer.stack.get(analyzer.stack.size() - 3);
					if (isPrimitiveStackType(threeDown)) {
						// Dup the top 2 words to be under the 2 beneath
						super.visitInsn(DUP2_X2);
					} else {
						// dup the top 2 words to be under the 1 beneath
						super.visitInsn(DUP2_X1);
					}
				}
			}
			break;
		case Opcodes.DUP2_X2:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (isPrimitiveStackType(topOfStack)) {
				if (getStackElementSize(topOfStack) == 2) {
					//Have two-word el + 1 word taint on top
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 4);
					if (isPrimitiveStackType(underThisOne)) {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 6);
						if (isPrimitiveStackType(threeDown)) {
							//Dup the top three words to be under the 4 words beneath them
							DUPN_XU(2, 4);
						} else {
							//Dup the top three words to be under the 3 words beneath them
							DUPN_XU(2, 3);
						}
					} else {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
						if (isPrimitiveStackType(threeDown)) {
							//Dup the top three words to be under the 4 words beneath them
							DUPN_XU(2, 3);
						} else {
							//Dup the top three words to be under the 2 words beneath them
							DUPN_XU(2, 2);
						}
					}
				} else // top is 1 word, primitive
				{
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
					if (isPrimitiveStackType(underThisOne)) {
						//top is primitive, second is primitive
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
						if (isPrimitiveStackType(threeDown)) {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isPrimitiveStackType(fourDown)) {
								DUPN_XU(4, 4);
							} else {
								DUPN_XU(4, 3);
							}
						} else {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isPrimitiveStackType(fourDown)) {
								DUPN_XU(4, 3);
							} else {
								DUPN_XU(4, 2);
							}
						}
					} else {
						//top is primitive, second is not
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isPrimitiveStackType(threeDown)) {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isPrimitiveStackType(fourDown)) {
								DUPN_XU(3, 4);
							} else {
								DUPN_XU(3, 3);
							}

						} else {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isPrimitiveStackType(fourDown)) {
								DUPN_XU(3, 3);
							} else {
								DUPN_XU(3, 2);
							}
						}
					}
				}
			} else {
				//top is not primitive. must be one word.
				Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 2);
				if (isPrimitiveStackType(underThisOne)) {
					Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
					if (isPrimitiveStackType(threeDown)) {
						Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
						if (isPrimitiveStackType(fourDown)) {
							DUPN_XU(3, 4);
						} else {
							DUPN_XU(3, 3);
						}

					} else {
						Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
						if (isPrimitiveStackType(fourDown)) {
							DUPN_XU(3, 3);
						} else {
							DUPN_XU(3, 2);
						}
					}
				} else {
					Object threeDown = analyzer.stack.get(analyzer.stack.size() - 3);
					if (isPrimitiveStackType(threeDown)) {
						super.visitInsn(DUP2_X2);
						Object fourDown = analyzer.stack.get(analyzer.stack.size() - 5);
						if (isPrimitiveStackType(fourDown)) {
							DUPN_XU(2, 4);
						} else {
							DUPN_XU(2, 3);
						}

					} else {
						Object fourDown = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isPrimitiveStackType(fourDown)) {
							DUPN_XU(2, 3);
						} else {
							super.visitInsn(DUP2_X2);
						}
					}
				}
			}
			break;
		case Opcodes.SWAP:
			topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
			if (isPrimitiveStackType(topOfStack)) {
				//swap needs to take into account that there's a taint under the top el
				Object secondOnStack = analyzer.stack.get(analyzer.stack.size() - 3);
				if (isPrimitiveStackType(secondOnStack)) {
					//top is primitive, second is primitive: AA BB -> BB AA
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
				} else {
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
				}
			} else {
				Object secondOnStack = analyzer.stack.get(analyzer.stack.size() - 2);
				if (isPrimitiveStackType(secondOnStack)) {
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				} else
					super.visitInsn(SWAP);
			}
			break;

		case Opcodes.FADD:
		case Opcodes.FREM:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
			 {
				if (isTaintlessArrayStore) {
					//T V V
					isTaintlessArrayStore = false;
					super.visitInsn(opcode);
					break;
				} else if (secondHas0Taint() && Configuration.OPT_CONSTANT_ARITHMETIC) {
					//0 V T V
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					//0 TV V
					super.visitInsn(SWAP);
					super.visitInsn(opcode);
					//0 T V
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				} else {
					//T V T V
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					int tmp = lvs.getTmpLV(getTopOfStackType());
					super.visitVarInsn(FSTORE, tmp);
					//T V T
					super.visitInsn(SWAP);
					//T T V
					super.visitVarInsn(FLOAD, tmp);
					//T T V V
					super.visitInsn(opcode);
					//T T V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					if(Configuration.MULTI_TAINTING)
					{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
						super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					}
					else
					{
						if (Configuration.DATAFLOW_TRACKING)
							super.visitInsn(Opcodes.IOR);
						else
							super.visitInsn(Opcodes.POP2);
				}
					super.visitInsn(SWAP);
					lvs.freeTmpLV(tmp);
				}
			}
			break;
		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.IOR:
		case Opcodes.IAND:
		case Opcodes.IXOR:
			{
				if (isTaintlessArrayStore) {
					//T V V
					isTaintlessArrayStore = false;
					super.visitInsn(opcode);
					break;
				} else if (secondHas0Taint() && Configuration.OPT_CONSTANT_ARITHMETIC) {
					//0 V T V
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					//0 TV V
					super.visitInsn(SWAP);
					super.visitInsn(opcode);
					//0 T V
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				} else {
					int tmp = lvs.getTmpLV(Type.INT_TYPE);
					//T V T V
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					super.visitVarInsn(ISTORE, tmp);
					//T V T
					super.visitInsn(SWAP);
					//T T V
					super.visitVarInsn(ILOAD, tmp);
					lvs.freeTmpLV(tmp);
					//T T V V
					super.visitInsn(opcode);
					//T T V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					if(Configuration.MULTI_TAINTING)
					{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
								"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					}
					else
					{
						if (Configuration.DATAFLOW_TRACKING)
							super.visitInsn(Opcodes.IOR);
						else
							super.visitInsn(Opcodes.POP2);
					}
					super.visitInsn(SWAP);
				}
			}
			break;
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			{
				boolean secondHas0Taint = secondHas0Taint();
				if (isTaintlessArrayStore) {
					isTaintlessArrayStore = false;
					//T VV VV
					super.visitInsn(opcode);
					break;
				} else if (TaintUtils.OPT_USE_STACK_ONLY) {
					//T VV T VV
					int tmp = lvs.getTmpLV();
					super.visitVarInsn(DSTORE, tmp);
					//T VV T
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					//T T VV
					super.visitVarInsn(DLOAD, tmp);
					lvs.freeTmpLV(tmp);
					// T T VV VV
					super.visitInsn(opcode);
					// T T VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					if(Configuration.MULTI_TAINTING)
					{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
								"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					}
					else
					{
						if (Configuration.DATAFLOW_TRACKING)
							super.visitInsn(Opcodes.IOR);
						else
							super.visitInsn(Opcodes.POP2);
					}
					// VV T
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				} else {

					//Do it with LVs where it is less opcodes.
					//T VV T VV
					int tmp = lvs.getTmpLV();
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					super.visitVarInsn(DSTORE, tmp);
					//T VV T
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					//T T VV
					int tmp2 = lvs.getTmpLV();
					super.visitInsn(TaintUtils.IS_TMP_STORE);
					super.visitVarInsn(DSTORE, tmp2);
					if (secondHas0Taint) {
						//0 T
						super.visitInsn(SWAP);
						super.visitInsn(POP);
					} else {
						//T T
						if(Configuration.MULTI_TAINTING)
						{
								super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
										"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
							
							}
						else
						{
							if (Configuration.DATAFLOW_TRACKING)
								super.visitInsn(Opcodes.IOR);
							else
								super.visitInsn(Opcodes.POP2);
						}
					}
					//T 
					super.visitVarInsn(DLOAD, tmp2);
					super.visitVarInsn(DLOAD, tmp);
					super.visitInsn(opcode);
					lvs.freeTmpLV(tmp2);
					lvs.freeTmpLV(tmp);
				}
			}
			break;
		case Opcodes.LSHL:
		case Opcodes.LUSHR:
		case Opcodes.LSHR:
			{
				//T VV T V
				int tmp = lvs.getTmpLV();
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(ISTORE, tmp);
				//T VV T
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				//T T VV
				super.visitVarInsn(ILOAD, tmp);
				lvs.freeTmpLV(tmp);
				// T T VV V
				super.visitInsn(opcode);
				// T T VV
				super.visitInsn(DUP2_X2);
				super.visitInsn(POP2);
				if(Configuration.MULTI_TAINTING)
				{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
								"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					
					}
				else
				{
					if (Configuration.DATAFLOW_TRACKING)
						super.visitInsn(Opcodes.IOR);
					else
						super.visitInsn(Opcodes.POP2);
				}
				// VV T
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			}
			break;
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LADD:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
			 {
				if (isTaintlessArrayStore) {
					isTaintlessArrayStore = false;
					//T V V
					super.visitInsn(opcode);
					break;
				} else {
					//T VV T VV
					int tmp = lvs.getTmpLV();
					super.visitVarInsn(LSTORE, tmp);
					//T VV T
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					//T T VV
					super.visitVarInsn(LLOAD, tmp);
					lvs.freeTmpLV(tmp);
					// T T VV VV
					super.visitInsn(opcode);
					// T T VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					if(Configuration.MULTI_TAINTING)
					{
							super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
									"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
						
						}
					else
					{
						if (Configuration.DATAFLOW_TRACKING)
							super.visitInsn(Opcodes.IOR);
						else
							super.visitInsn(Opcodes.POP2);
					}
					// VV T
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
			}
			break;
		case Opcodes.INEG:
		case Opcodes.FNEG:
			super.visitInsn(opcode);
			
			break;
		case Opcodes.LNEG:
		case Opcodes.DNEG:
			super.visitInsn(opcode);
			
			break;
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
			super.visitInsn(opcode);
			break;
		case Opcodes.LCMP: {
			//T VV T VV
			int tmp = lvs.getTmpLV();
			super.visitInsn(TaintUtils.IS_TMP_STORE);
			super.visitVarInsn(LSTORE, tmp);
			//T VV T
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			//T T VV
			super.visitVarInsn(LLOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV VV
			super.visitInsn(opcode);
			// T T V
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {

				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);

			} else {
				if (Configuration.DATAFLOW_TRACKING)
					super.visitInsn(Opcodes.IOR);
				else
					super.visitInsn(Opcodes.POP2);
			}
			// V T
			super.visitInsn(SWAP);
		}
			break;
		case Opcodes.DCMPL:
			{
				//T VV T VV
				int tmp = lvs.getTmpLV();
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(DSTORE, tmp);
				//T VV T
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				//T T VV
				super.visitVarInsn(DLOAD, tmp);
				lvs.freeTmpLV(tmp);
				// T T VV VV
				super.visitInsn(opcode);
				// T T V
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				if(Configuration.MULTI_TAINTING)
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					}
				else
				{
					if (Configuration.DATAFLOW_TRACKING)
						super.visitInsn(Opcodes.IOR);
					else
						super.visitInsn(Opcodes.POP2);
				}
				// V T
				super.visitInsn(SWAP);
			}
			break;
		case Opcodes.DCMPG:
			 {
				//T VV T VV
				int tmp = lvs.getTmpLV();
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(DSTORE, tmp);
				//T VV T
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				//T T VV
				super.visitVarInsn(DLOAD, tmp);
				lvs.freeTmpLV(tmp);
				// T T VV VV
				super.visitInsn(opcode);
				// T T V
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				if(Configuration.MULTI_TAINTING)
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
							"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					}
				else
				{
					if (Configuration.DATAFLOW_TRACKING)
						super.visitInsn(Opcodes.IOR);
					else
						super.visitInsn(Opcodes.POP2);
				}
				// VV T
				super.visitInsn(SWAP);
			}
			break;
		case Opcodes.FCMPL:
			{
				//T V T V
				int tmp = lvs.getTmpLV();
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(FSTORE, tmp);
				//T V T
				super.visitInsn(SWAP);
				//T T V
				super.visitVarInsn(FLOAD, tmp);
				lvs.freeTmpLV(tmp);
				//T T V V
				super.visitInsn(opcode);
				//T T V
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				if(Configuration.MULTI_TAINTING)
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
							"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
				}
				else
				{
					if (Configuration.DATAFLOW_TRACKING)
						super.visitInsn(Opcodes.IOR);
					else
						super.visitInsn(Opcodes.POP2);
				}
				super.visitInsn(SWAP);
			}
			break;
		case Opcodes.FCMPG:
			{
				//T V T V
				int tmp = lvs.getTmpLV();
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(FSTORE, tmp);
				//T V T
				super.visitInsn(SWAP);
				//T T V
				super.visitVarInsn(FLOAD, tmp);
				lvs.freeTmpLV(tmp);
				//T T V V
				super.visitInsn(opcode);
				//T T V
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				if(Configuration.MULTI_TAINTING)
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
							"("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					
				}
				else
				{
					if (Configuration.DATAFLOW_TRACKING)
						super.visitInsn(Opcodes.IOR);
					else
						super.visitInsn(Opcodes.POP2);
				}
				super.visitInsn(SWAP);
			}
			break;
		case Opcodes.DRETURN:
		case Opcodes.LRETURN:
			int retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
			
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", (!Configuration.MULTI_TAINTING ? "I":"Ljava/lang/Object;"));
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(ARETURN);
			break;
		case Opcodes.IRETURN:
		case Opcodes.FRETURN:
			//			super.visitMethodInsn(INVOKESTATIC, TaintUtils.getContainerReturnType(originalMethodReturnType).getInternalName(), "valueOf", "(I" + originalMethodReturnType.getDescriptor() + ")"
			//					+ TaintUtils.getContainerReturnType(originalMethodReturnType).getDescriptor());
			retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", (!Configuration.MULTI_TAINTING ? "I":"Ljava/lang/Object;"));
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(ARETURN);
			break;
		case Opcodes.ARETURN:
			Type onStack = getTopOfStackType();
			if (originalMethodReturnType.getSort() == Type.ARRAY) {
				if (onStack.getElementType().getSort() == Type.OBJECT && onStack.getElementType().getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
					super.visitInsn(opcode);
					return;
				} else if (originalMethodReturnType.getDimensions() > 1 && (onStack.getSort() != Type.ARRAY || onStack.getElementType().getSort() == Type.OBJECT)) {
					super.visitInsn(opcode);
					return;
				}
				switch (originalMethodReturnType.getElementType().getSort()) {
				case Type.INT:
				case Type.LONG:
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.DOUBLE:
				case Type.FLOAT:
				case Type.SHORT:
					//If we are returning and there is a NULL on the top of the stack, we need another!
					if (topStackElIsNull()) {
						super.visitInsn(ACONST_NULL);
					}
					//					//T V
					//					//					System.out.println("zzzNEW " + newReturnType);
					//					super.visitTypeInsn(NEW, newReturnType.getInternalName()); //T V N
					//					super.visitInsn(DUP_X2); //N T V N
					//					super.visitInsn(DUP_X2); //N N T V N
					//					super.visitInsn(POP); //N N T V
					//					if (originalMethodReturnType.getDimensions() > 1) {
					//						super.visitTypeInsn(CHECKCAST, "java/lang/Object"); // NNTV
					//						super.visitInsn(SWAP);//NNVT
					//						super.visitTypeInsn(CHECKCAST, "java/lang/Object");
					//						super.visitInsn(SWAP);//NNTV
					//						super.visitMethodInsn(INVOKESPECIAL, newReturnType.getInternalName(), "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
					//					} else
					//						super.visitMethodInsn(INVOKESPECIAL, newReturnType.getInternalName(), "<init>", "([I" + originalMethodReturnType.getDescriptor() + ")V");
					//					super.visitInsn(opcode);
					retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
					super.visitVarInsn(ALOAD, retIdx);
					super.visitInsn(SWAP);
					super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
					super.visitVarInsn(ALOAD, retIdx);
					super.visitInsn(SWAP);
					if(!Configuration.MULTI_TAINTING)
					super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", "[I");
					else
						super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", "[Ljava/lang/Object;");
					super.visitVarInsn(ALOAD, retIdx);
					super.visitInsn(ARETURN);
					break;
				default:
					super.visitInsn(opcode);
				}
			} else if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
				registerTaintedArray(getTopOfStackType().getDescriptor());
				super.visitInsn(opcode);
			} else
				super.visitInsn(opcode);
			break;
		case Opcodes.RETURN:
			super.visitInsn(opcode);
			break;
		case Opcodes.ARRAYLENGTH:

			Type arrType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1));
			{
				if (arrType.getElementType().getSort() != Type.OBJECT) {
					//TA A
					super.visitInsn(SWAP);
					super.visitInsn(POP);
					//A
				}
				super.visitInsn(opcode);
				super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
				super.visitInsn(SWAP);

			}
			//			System.out.println("post arraylength " + analyzer.stack);
			break;
		case Opcodes.ATHROW:
			if (TaintUtils.DEBUG_FRAMES)
				System.out.println("ATHROW " + analyzer.stack);
			super.visitInsn(opcode);
			break;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			//You can have a monitor on an array type. If it's a primitive array type, pop the taint off!
			if (topStackElCarriesTaints()) {
				super.visitInsn(SWAP);
				super.visitInsn(POP);
			}
			super.visitInsn(opcode);
			break;
		default:
			super.visitInsn(opcode);

			throw new IllegalArgumentException();
		}
	}

	private boolean topStackElCarriesTaints() {
		Object o = analyzer.stack.get(analyzer.stack.size() - 1);
		return isPrimitiveStackType(o);
	}

	private boolean topStackElIsNull() {
		Object o = analyzer.stack.get(analyzer.stack.size() - 1);
		return o == Opcodes.NULL;
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		//		System.out.println(Printer.OPCODES[opcode]);
//		System.out.println("PRE" + name + Printer.OPCODES[opcode] + " " + analyzer.stack);

		if (isIgnoreAllInstrumenting) {
			super.visitJumpInsn(opcode, label);
			return;
		}

		if (Configuration.IMPLICIT_TRACKING && !isIgnoreAllInstrumenting) {
			switch (opcode) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:

				super.visitInsn(SWAP);
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				super.visitInsn(SWAP);
				super.visitIntInsn(BIPUSH, branchStarting);
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "("+Configuration.TAINT_TAG_DESC+"I)V", false);

				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				Type typeOnStack = getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT && typeOnStack.getDimensions() == 1) {
					//O1 T1
					super.visitInsn(SWAP);
					super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
					super.visitInsn(SWAP);
					super.visitIntInsn(BIPUSH, branchStarting);
					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(Ljava/lang/Object;I)V", false);
				} else {
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
					super.visitInsn(SWAP);
					super.visitIntInsn(BIPUSH, branchStarting);
					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(Ljava/lang/Object;I)V", false);
				}

				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				//T V T V
				int tmp = lvs.getTmpLV(Type.INT_TYPE);
				//T V T V
				super.visitInsn(SWAP);
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, tmp);
				//T V V
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				//V V T  
				super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
				super.visitInsn(SWAP);
				//V V C T
				super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, tmp);
				lvs.freeTmpLV(tmp);
				//V V T T
				super.visitIntInsn(BIPUSH, branchStarting);
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "("+Configuration.TAINT_TAG_DESC+Configuration.TAINT_TAG_DESC+"I)V", false);

				break;
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ACMPEQ:
				typeOnStack = getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
				//O1 O2 (t2?)
				Type secondOnStack = getStackTypeAtOffset(1);
				if (secondOnStack.getSort() == Type.ARRAY && secondOnStack.getElementType().getSort() != Type.OBJECT) {
					//O1 O2 T2
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				}
				break;
			case Opcodes.GOTO:
				break;
			default:
				throw new IllegalStateException("Unimplemented: " + opcode);
			}
			if(opcode != Opcodes.GOTO)
			{
				for (int var : forceCtrlAdd) {
					int shadowVar = -1;
					if (analyzer.locals.size() <= var || analyzer.locals.get(var) == Opcodes.TOP)
						continue;
					if (var < lastArg && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
						//accessing an arg; remap it
						Type localType = paramTypes[var];
						if (localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
							shadowVar = var - 1;
						} else if (localType.getSort() == Type.ARRAY)
							continue;
					} else {
						if (lvs.varToShadowVar.containsKey(var)) {
							shadowVar = lvs.varToShadowVar.get(var);
							if (analyzer.locals.get(var) instanceof String && ((String) analyzer.locals.get(var)).startsWith("["))
								continue;
							if (shadowVar >= analyzer.locals.size() || analyzer.locals.get(shadowVar) instanceof Integer || ((String) analyzer.locals.get(shadowVar)).startsWith("["))
								continue;
						}
					}
					if(shadowVar >= 0)
					{
						super.visitVarInsn(ALOAD, shadowVar);
						super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
						super.visitVarInsn(ASTORE, shadowVar);
					}
					else
					{
						super.visitVarInsn(ALOAD, var);
						super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
					}
				}
				forceCtrlAdd.clear();
			}
		}
		if (boxAtNextJump.size() > 0) {
			Label origDest = label;
			Label newDest = new Label();
			Label origFalseLoc = new Label();

			super.visitJumpInsn(opcode, newDest);
			FrameNode fn = getCurrentFrameNode();
			super.visitJumpInsn(GOTO, origFalseLoc);
			//			System.out.println("taint passing mv monkeying with jump");
			super.visitLabel(newDest);
			fn.accept(this);
			for (Integer var : boxAtNextJump) {
				super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
				super.visitVarInsn(ALOAD, var);
//				System.out.println("Boxing." + analyzer.stack);
				registerTaintedArray(getTopOfStackType().getDescriptor());
				
				super.visitVarInsn(ASTORE, var);
			}
			super.visitJumpInsn(GOTO, origDest);
			super.visitLabel(origFalseLoc);
			fn.accept(this);
			boxAtNextJump.clear();
		}
		else
			super.visitJumpInsn(opcode, label); //If we aren't doing implicit tracking, let ImplicitTaintRemovingMV handle this
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitTableSwitchInsn(min, max, dflt, labels);
			return;
		}
		//Need to remove taint
		if (TaintUtils.DEBUG_FRAMES)
			System.out.println("Table switch shows: " + analyzer.stack);
		if (Configuration.IMPLICIT_TRACKING) {
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
			super.visitInsn(SWAP);
			super.visitIntInsn(BIPUSH, branchStarting);
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "("+Configuration.TAINT_TAG_DESC+"I)V", false);
		} else {
			super.visitInsn(SWAP);
			super.visitInsn(POP);
		}
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitLookupSwitchInsn(dflt, keys, labels);
			return;
		}
		//Need to remove taint
		if (Configuration.IMPLICIT_TRACKING) {
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, jumpControlTaintLVs.get(0));
			super.visitInsn(SWAP);
			super.visitIntInsn(BIPUSH, branchStarting);
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "("+Configuration.TAINT_TAG_DESC+"I)V", false);
		} else {
			super.visitInsn(SWAP);
			super.visitInsn(POP);
		}
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	int argOffset;

	public void setLVOffset(int newArgOffset) {
		this.argOffset = newArgOffset;
	}
}
