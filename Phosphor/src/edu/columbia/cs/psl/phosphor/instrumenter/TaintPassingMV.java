package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LocalVariableNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.Printer;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.Textifier;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStore;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.TaintedDouble;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloat;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedLong;

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

	public TaintPassingMV(MethodVisitor mv, MethodVisitor nonInstrumentingMV, int access, String className, String name, String desc, String originalDesc, NeverNullArgAnalyzerAdapter analyzer) {
		//		super(Opcodes.ASM4,mv,access,name,desc);
		super(Opcodes.ASM5, className, mv, analyzer, nonInstrumentingMV);
		//		System.out.println("TPMV "+ className+"."+name+desc);
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
			n += newArgTypes[i].getSize();
		}
	}

	protected Type getLocalType(int n) {
		if (n >= analyzer.locals.size())
			return null;
		Object t = analyzer.locals.get(n);
		if (t == Opcodes.TOP || t == Opcodes.NULL)
			return null;
		return getTypeForStackType(t);
	}

	HashMap<Integer, Integer> varToShadowVar = new HashMap<Integer, Integer>();
	HashMap<Integer, Object> varTypes = new HashMap<Integer, Object>();
	HashSet<Integer> questionableShadows = new HashSet<Integer>();

	HashSet<Integer> boxAtNextJump = new HashSet<Integer>();

	@SuppressWarnings("unused")
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (opcode == TaintUtils.NEVER_AUTOBOX) {
			System.out.println("Never autobox: " + var);
			varsNeverToForceBox.add(var);
			return;
		}
		if (opcode == TaintUtils.ALWAYS_AUTOBOX && analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
			Type t = Type.getType((String) analyzer.locals.get(var));
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && varToShadowVar.containsKey(var)) {

				super.visitVarInsn(ALOAD, varToShadowVar.get(var));
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
			super.visitVarInsn(opcode, var);
			return;
		}

		int shadowVar = -1;
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println(this.name + " " + Printer.OPCODES[opcode] + " on " + var + " last arg" + lastArg +", stack: " + analyzer.stack);

		if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
			System.out.println(this.name + " ASTORE " + var + ", shadowvar contains " + varToShadowVar.get(var) + " oldvartype " + varTypes.get(var));
		}
		boolean boxIt = false;

		//		if((opcode == ALOAD) && var < analyzer.frameLocals.size() && analyzer.frameLocals.get(var) != null && (analyzer.frameLocals.get(var).equals("java/lang/Object") || analyzer.frameLocals.get(var).equals("[Ljava/lang/Object;")))
		//			boxIt = true;
		//		if(!boxIt && (opcode == ALOAD) && var < analyzer.locals.size() && analyzer.locals.get(var) != null && (analyzer.locals.get(var).equals("java/lang/Object") || analyzer.locals.get(var).equals("[Ljava/lang/Object;")))
		//			boxIt = true;

		if (varsNeverToForceBox.contains(var)) {
			boxIt = false;
			//			System.out.println("actually, not Boxing " + var);
		}
		if (var == 0 && !isStatic) {
			//accessing "this" so no-op, die here so we never have to worry about uninitialized this later on.
			super.visitVarInsn(opcode, var);
			return;
		} else if (var < lastArg) {
			//accessing an arg; remap it
			Type localType = paramTypes[var];
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println(Arrays.toString(paramTypes) + ",,," + var);
			if (TaintUtils.getShadowTaintType(localType.getDescriptor()) != null)
				shadowVar = var - 1;
		} else {
			//not accessing an arg

			Object oldVarType = varTypes.get(var);
			if (varToShadowVar.containsKey(var))
				shadowVar = varToShadowVar.get(var);
//						System.out.println(Printer.OPCODES[opcode] + " "+var + " old " + oldVarType + " shadow " + shadowVar);

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
						varTypes.put(var, Opcodes.INTEGER);
					if (shadowVar > -1) {
						analyzer.locals.set(shadowVar, Opcodes.INTEGER);
						lvs.remapLocal(shadowVar, Type.getType("I"));
					}
				} else if (oldVarType instanceof Integer && oldVarType != Opcodes.NULL && (opcode == ASTORE || opcode == ALOAD)) {
					//Went from primitive to TYPE
					if (opcode == ASTORE)
						varTypes.put(var, getTopOfStackObject());
					else
						varTypes.put(var, "Lunidentified;");

					if (shadowVar > -1)
						if (opcode == ASTORE) {
							String shadow = TaintUtils.getShadowTaintType(getTopOfStackType().getDescriptor());
							if (shadow == null) {
								shadow = "[I";
								analyzer.locals.set(shadowVar, "[I");
							} else if (shadow.equals("I"))
								analyzer.locals.set(shadowVar, Opcodes.INTEGER);
							else
								analyzer.locals.set(shadowVar, shadow);
							lvs.remapLocal(shadowVar, Type.getType(shadow));
						} else {
							lvs.remapLocal(shadowVar, Type.getType("[I"));
							analyzer.locals.set(shadowVar, "[I");
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
							varToShadowVar.put(var, lvs.newLocal(Type.getType("[I")));
							varTypes.put(var, Opcodes.NULL);
							shadowVar = varToShadowVar.get(var);
							if (shadowVar == analyzer.locals.size())
								analyzer.locals.add(Opcodes.NULL);
						}
					}
				} else if (opcode == ASTORE) {
					if (topOfStackIsNull()) {
						varToShadowVar.put(var, lvs.newLocal(Type.getType("[I")));
						varTypes.put(var, Opcodes.NULL);
						shadowVar = varToShadowVar.get(var);
					} else {
						Type onStack = getTopOfStackType();
						if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
							//That's easy.
							varToShadowVar.put(var, lvs.newLocal(Type.getType(TaintUtils.getShadowTaintType(onStack.getDescriptor()))));
							varTypes.put(var, getTopOfStackObject());
							shadowVar = varToShadowVar.get(var);
						}
					}
				} else {
//					if (TaintUtils.DEBUG_LOCAL)
					varToShadowVar.put(var, lvs.newLocal(Type.INT_TYPE));
					varTypes.put(var, Opcodes.INTEGER);
					shadowVar = varToShadowVar.get(var);
//					System.out.println("creating shadow for " + var + " - " + shadowVar);
					if (opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD) {
						if (shadowVar == analyzer.locals.size())
							analyzer.locals.add(Opcodes.INTEGER);
					}
				}
			}

			if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
				System.out.println("ASTORE " + var + ", shadowvar contains " + varToShadowVar.get(var));
			}
			if (shadowVar > -1 && TaintUtils.DEBUG_LOCAL) {
				System.out.println("using shadow " + shadowVar + "for " + var + (boxIt ? " boxit" : " notboxit"));
				System.out.println("LVS: " + analyzer.locals);
			}
		}
		if (opcode == Opcodes.ALOAD) {
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("ALOAD " + var + " onto stack " + analyzer.stack);
		}
		if (opcode == Opcodes.ILOAD) {
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("ILOAD " + var + " onto stack " + analyzer.stack);
		}
		if (shadowVar >= 0 && !boxIt) {
			switch (opcode) {
			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
//				System.out.println("PRE LOAD" + var + analyzer.stack + "; " + analyzer.locals);
				super.visitVarInsn(ILOAD, shadowVar);
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
					localType = Type.getType("[I");
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
				if (localType.getSort() == Type.ARRAY) {
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
				super.visitVarInsn(ISTORE, shadowVar);
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

		if (ignoreLoadingNextTaint) {
			switch (opcode) {
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
				super.visitFieldInsn(opcode, owner, name, desc);
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
						super.visitInsn(ICONST_0);
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
						super.visitInsn(ICONST_0);
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
//						System.out.println("PUTFIELD " + owner+"."+name+desc +" "+ analyzer.stack);
//						new Exception().printStackTrace();
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
			super.visitInsn(ICONST_1);
			super.visitInsn(IADD);
			super.visitIntInsn(opcode, Opcodes.T_INT);

			//Our fancy new hack where we store the taint for a primitive array in the last element of the (oversized) taint array.
			//Generate a new array taint, store it back in it.
			super.visitInsn(DUP);
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);
			super.visitInsn(ICONST_M1);
			super.visitInsn(IADD);
			super.visitInsn(ICONST_0);
			super.visitInsn(IASTORE);

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
		//Top of stack is array with dimensions, no taints.
		for (int i = 0; i < dims; i++) {
			super.visitInsn(DUP);
			super.visitIntInsn(BIPUSH, dims - i - 1);
			super.visitInsn(IALOAD);
			super.visitInsn(SWAP);
		}
		super.visitInsn(POP);
		if (dims == 1) {
			//It's possible that we dropped down to a 1D object type array
			super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
		} else
			super.visitMultiANewArrayInsn(desc, dims);

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
			
			super.visitTypeInsn(opcode, type);
			break;
		case Opcodes.NEW:
			super.visitTypeInsn(opcode, type);
			break;
		case Opcodes.CHECKCAST:

			t = Type.getType(type);
//			System.out.println("Pre cc "+type+": " + analyzer.stack);
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {

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
						super.visitTypeInsn(CHECKCAST,"[I");
						super.visitInsn(SWAP);
					}
					else
						retrieveTaintedArray(type);
					super.visitTypeInsn(opcode, type);
				
			} else {

				//What if we are casting an array to Object?
				if (topStackElCarriesTaints()) {
					//Casting array to non-array type

					//Register the taint array for later.
					registerTaintedArray(getTopOfStackType().getDescriptor());
				}
				super.visitTypeInsn(opcode, type);
			}
//			System.out.println("post cc " + analyzer.stack);
			break;
		case Opcodes.INSTANCEOF:
			t = Type.getType(type);



			if (ignoreLoadingNextTaint) {
				super.visitTypeInsn(opcode, type);

				return;
			}
			{
				if (topStackElCarriesTaints()) {
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
				super.visitTypeInsn(opcode, type);
				super.visitInsn(ICONST_0);
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
		if(getTopOfStackObject() == Opcodes.NULL)
		{
//			super.visitInsn(ACONST_NULL);
		}
		else{
		super.visitInsn(DUP);
		super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ArrayHelper.class), "getTags", "(Ljava/lang/Object;)[I", false);
		super.visitInsn(SWAP);
		}
	}

	/**
	 * 
	 * @param descOfDest
	 */
	protected void registerTaintedArray(String descOfDest) {
		Type onStack = Type.getType(descOfDest);//getTopOfStackType();
		if (onStack.getSort() == Type.ARRAY && onStack.getDimensions() == 1 && onStack.getElementType().getSort() != Type.OBJECT) {
//			System.out.println("Pre register tainted array stack is " + analyzer.stack);
			super.visitInsn(DUP_X1);
			super.visitInsn(SWAP);
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ArrayHelper.class), "setTags", "(Ljava/lang/Object;[I)V", false);
		}
	}

	protected void unwrapTaintedInt() {
		super.visitInsn(DUP);
		super.visitFieldInsn(GETFIELD, Type.getInternalName(TaintedInt.class), "taint", "I");
		super.swap();
		super.visitFieldInsn(GETFIELD, Type.getInternalName(TaintedInt.class), "val", "I");
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
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		boolean isPreAllocedReturnType = TaintUtils.isPreAllocReturnType(desc);
		if (Instrumenter.isClassWithHashmapTag(owner) && name.equals("valueOf")) {
			Type[] args = Type.getArgumentTypes(desc);
			if (args[0].getSort() != Type.OBJECT) {
				owner = Type.getInternalName(BoxedPrimitiveStore.class);
				desc = "(I" + desc.substring(1);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
				return;
			}
		} else if (Instrumenter.isClassWithHashmapTag(owner) && (name.equals("byteValue") || name.equals("booleanValue") || name.equals("charValue") || name.equals("shortValue"))) {
			Type returnType = Type.getReturnType(desc);
			Type boxedReturn = TaintUtils.getContainerReturnType(returnType);
			desc = "(L" + owner + ";)" + boxedReturn.getDescriptor();
			owner = Type.getInternalName(BoxedPrimitiveStore.class);

			super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
			super.visitInsn(DUP);
			super.visitFieldInsn(GETFIELD, boxedReturn.getInternalName(), "taint", "I");
			super.visitInsn(SWAP);
			super.visitFieldInsn(GETFIELD, boxedReturn.getInternalName(), "val", returnType.getDescriptor());
			return;
		}

		Type ownerType = Type.getObjectType(owner);

		//		Type ownerType = Type.getType(owner + ";");
		boolean isCallToPrimitiveArrayClone = opcode == INVOKEVIRTUAL && desc.equals("()Ljava/lang/Object;") && name.equals("clone") && getTopOfStackType().getSort() == Type.ARRAY
				&& getTopOfStackType().getElementType().getSort() != Type.OBJECT && getTopOfStackType().getDimensions() == 1;
		//When you call primitive array clone, we should first clone the taint array, then register that taint array to the cloned object after calling clone
		Type primitiveArrayType = null;
		if (isCallToPrimitiveArrayClone) {
			primitiveArrayType = getTopOfStackType();
			//TA A
			super.visitInsn(SWAP);
			super.visitMethodInsn(opcode, "java/lang/Object", "clone", "()Ljava/lang/Object;",false);
			super.visitTypeInsn(CHECKCAST, "[I");
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
			destIsPrimitve = destType.getSort() == Type.ARRAY && destType.getElementType().getSort() != Type.OBJECT && destType.getDimensions() == 1;
			int srcOffset = 7;
			if (destIsPrimitve)
				srcOffset++;
//						System.out.println("Sysarracopy with " + analyzer.stack);
			Type srcType = getStackTypeAtOffset(srcOffset);
			boolean srcIsPrimitive = srcType.getSort() == Type.ARRAY && srcType.getElementType().getSort() != Type.OBJECT && srcType.getDimensions() == 1;
			if (srcIsPrimitive) {
				if (destIsPrimitve) {
					desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
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
		if (owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints") && !desc.equals("(I)V") && !owner.endsWith("Tainter")) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		//to reduce how much we need to wrap, we will only rename methods that actually have a different descriptor
		boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);

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
				if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT && callee.getDimensions() == 1)
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

		//								System.out.println("12dw23 Calling "+owner+"."+name+newDesc + "with " + analyzer.stack);
		for (Type t : argsInReverse) {
			if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
				i++;

			//			if(ignoreNext)
			//				System.out.println("ignore next i");
			Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
			//			System.out.println(name+ " ONStack: " + onStack + " and t = " + t);

			if (!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				//				System.out.println("t " + t + " and " + analyzer.stack.get(analyzer.stack.size() - i) + " j23oij4oi23");

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
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
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
			if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT && callee.getDimensions() == 1)
				isCalledOnAPrimitiveArrayType = true;
		}

		super.visitMethodInsn(opcode, owner, name, newDesc, itfc);
		//		System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//		System.out.println("Now: " + analyzer.stack);
		if (isCallToPrimitiveArrayClone) {
			//Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
			//TA A
			//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
//			super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
//						System.out.println(owner + name + newDesc + ": " + analyzer.stack);
			registerTaintedArray(primitiveArrayType.getDescriptor());
//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
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
			if(Type.getReturnType(desc).getSort() == Type.ARRAY)
			{
				retrieveTaintedArray(Type.getReturnType(desc).getDescriptor());
			}
			else {
				super.visitInsn(DUP);
				String taintTypeRaw = "I";
				if (Type.getReturnType(desc).getSort() == Type.ARRAY)
					taintTypeRaw = "[I";
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintTypeRaw);

				super.visitInsn(SWAP);
				super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
			}
		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if (TaintUtils.DEBUG_FRAMES)
			System.out.println("TMV sees frame: " + Arrays.toString(local) + ", stack " + Arrays.toString(stack));
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

	@Override
	public void visitInsn(int opcode) {
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
				super.visitInsn(ICONST_0);
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
			return;
		} else if (isIgnoreAllInstrumenting || isRawInsns) {
			super.visitInsn(opcode);
			return;
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
					super.visitInsn(IALOAD);
					//V V T
					super.visitInsn(DUP_X2);
					// T V V T
					super.visitInsn(POP);
				} else {
					//TA I V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					//V TA I
					super.visitInsn(IALOAD);
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
			if (t.getDimensions() == 2 && t.getElementType().getSort() != Type.OBJECT) {
				//				System.out.println("it's a multi array in disguise!!!");
				super.visitInsn(opcode);
				try {
					retrieveTaintedArray("null");
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
			//						System.out.println("AASTORE of " + arrayType + " ONTO ...");

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
				registerTaintedArray(t.getDescriptor());
				//We don't know if we're storing it in a multi-d array of this type, or of type Object[]. If it's
				//type Object[] then we will not be storing the taint! Let's find out what the array beneath us is.
				
			}
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitInsn(POP);
				super.visitInsn(opcode);
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
				if (opcode == DASTORE || opcode == LASTORE)
					super.visitVarInsn(valStoreOpcode, tmp1);
				else
					super.visitVarInsn(valStoreOpcode, tmp1);
				tmp2 = lvs.getTmpLV(getTopOfStackType());
				super.visitInsn(TaintUtils.IS_TMP_STORE);
				super.visitVarInsn(ISTORE, tmp2);
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
				super.visitVarInsn(ILOAD, tmp2);
				super.visitInsn(IASTORE);
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
					super.visitVarInsn(ISTORE, taint);
					//VV T
					super.visitInsn(DUP_X2);
					//T VV T
					super.visitInsn(POP);
					//TVV
					super.visitInsn(DUP2);
					//TVVVV
					super.visitVarInsn(ILOAD, taint);
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
					super.visitVarInsn(ISTORE, second);
					if (isPrimitiveStackType(secondOnStack)) {
						//Also need to dup the second one.
						super.visitInsn(DUP2);
					} else
						super.visitInsn(DUP);
					super.visitVarInsn(ILOAD, second);
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
				} else if (secondHas0Taint() && TaintUtils.OPT_CONSTANT_ARITHMETIC) {
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
					super.visitInsn(IOR);
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
				} else if (secondHas0Taint() && TaintUtils.OPT_CONSTANT_ARITHMETIC) {
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
					super.visitInsn(IOR);
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
					super.visitInsn(IOR);
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
						super.visitInsn(IOR);
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
				super.visitInsn(IOR);
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
					super.visitInsn(IOR);
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
		case Opcodes.LCMP:
			{
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
				super.visitInsn(IOR);
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
				super.visitInsn(IOR);
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
				super.visitInsn(IOR);
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
				super.visitInsn(IOR);
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
				super.visitInsn(IOR);
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
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", "I");
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
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", "I");
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(ARETURN);
			break;
		case Opcodes.ARETURN:
			Type onStack = getTopOfStackType();
			if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
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

				if (arrType.getElementType().getSort() != Type.OBJECT && arrType.getDimensions() == 1) {
					//TA A
					super.visitInsn(SWAP);
					super.visitInsn(POP);
					//A
				}
				super.visitInsn(opcode);
				super.visitInsn(ICONST_0);
				super.visitInsn(SWAP);


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
		if (isIgnoreAllInstrumenting) {
			super.visitJumpInsn(opcode, label);
			return;
		}

		if (boxAtNextJump.size() > 0) {
			Label origDest = label;
			Label newDest = new Label();
			Label origFalseLoc = new Label();

			super.visitJumpInsn(opcode, newDest);
			nonInstrumentingMV.visitJumpInsn(GOTO, origFalseLoc);
			//			System.out.println("taint passing mv monkeying with jump");
			super.visitLabel(newDest);
			for (Integer var : boxAtNextJump) {
				super.visitVarInsn(ALOAD, varToShadowVar.get(var));
				super.visitVarInsn(ALOAD, var);
				registerTaintedArray("[I");
//				Type onStack = Type.getObjectType((String) analyzer.locals.get(var));
//				//TA A
//				Type wrapperType = Type.getType(MultiDTaintedArray.getClassForComponentType(onStack.getElementType().getSort()));
//				//				System.out.println("Boxing " + var + " to " + wrapperType);
//
//				//				System.out.println("zzzNEW " + wrapperType);
//				Label isNull = new Label();
//				nonInstrumentingMV.visitInsn(DUP);
//				nonInstrumentingMV.visitJumpInsn(IFNULL, isNull);
//				nonInstrumentingMV.visitTypeInsn(NEW, wrapperType.getInternalName());
//				//TA A N
//				nonInstrumentingMV.visitInsn(DUP_X2);
//				nonInstrumentingMV.visitInsn(DUP_X2);
//				nonInstrumentingMV.visitInsn(POP);
//				//N N TA A 
//				nonInstrumentingMV.visitMethodInsn(INVOKESPECIAL, wrapperType.getInternalName(), "<init>", "([I" + onStack.getDescriptor() + ")V",false);
//				Label isDone = new Label();
//				nonInstrumentingMV.visitJumpInsn(GOTO, isDone);
//				nonInstrumentingMV.visitLabel(isNull);
//				nonInstrumentingMV.visitInsn(POP);
//				nonInstrumentingMV.visitLabel(isDone);
//				nonInstrumentingMV.visitVarInsn(ASTORE, var);

			}
			nonInstrumentingMV.visitJumpInsn(GOTO, origDest);
			nonInstrumentingMV.visitLabel(origFalseLoc);
			boxAtNextJump.clear();
			return;
		}
		switch (opcode) {
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPNE:
		case Opcodes.IF_ACMPEQ:
			//Logic for dealing with the taints on the stack is now in JumpLoggingMV
//			System.out.println(Textifier.OPCODES[opcode] + analyzer.stack);
			super.visitJumpInsn(opcode, label);
			break;
		case Opcodes.GOTO:
			//			if (curLabel >= 0 && curLabel < bbsToAddACONST_NULLto.length && bbsToAddACONST_NULLto[curLabel] == 2) {
			//				//There seems to be a bug where we are adding extra NULL because in the analysis pass we find that NULL is on the top
			//				//but for other reasons we've loaded an extra null already (e.g. from an LV that was null but we had type inference for)
			//				//Find out if that's the case.
			//				if(TaintUtils.DEBUG_FRAMES)
			//								System.out.println("Pre add extra null" + analyzer.stack);
			//				if (analyzer.stack.size() == 0 || topStackElIsNull())
			//					super.visitInsn(ACONST_NULL);
			//				bbsToAddACONST_NULLto[curLabel] = 0;
			//			}
			//			if (curLabel >= 0 && curLabel < bbsToAddChecktypeObjectto.length && bbsToAddChecktypeObjectto[curLabel] == 2) {
			//				visitTypeInsn(CHECKCAST, "java/lang/Object");
			//				bbsToAddChecktypeObjectto[curLabel] = 0;
			//			}
			super.visitJumpInsn(opcode, label);
			break;
		default:
			super.visitJumpInsn(opcode, label);
			throw new IllegalStateException("Unimplemented: " + opcode);
		}
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
		super.visitInsn(SWAP);
		super.visitInsn(POP);
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (isIgnoreAllInstrumenting || ignoreLoadingNextTaint) {
			super.visitLookupSwitchInsn(dflt, keys, labels);
			return;
		}
		//Need to remove taint
		super.visitInsn(SWAP);
		super.visitInsn(POP);
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	int argOffset;

	public void setLVOffset(int newArgOffset) {
		this.argOffset = newArgOffset;
	}
}
