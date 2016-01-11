package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class PartialInstrumentationMV extends TaintAdapter implements Opcodes {

	private NeverNullArgAnalyzerAdapter analyzer;
	private MethodVisitor uninstMV;
	private StringTaintVerifyingMV stringVerifyingMV;
	int lastArg;
	private boolean forceFieldChecks;
	
	public PartialInstrumentationMV(String className, int access, String methodName, String desc, MethodVisitor mv, NeverNullArgAnalyzerAdapter an, MethodVisitor uninstMV, boolean forceFieldChecks) {
		super(access, className, methodName, desc, null, null, mv, an);
		this.uninstMV = uninstMV;
		this.analyzer = an;
		if(forceFieldChecks)
			stringVerifyingMV = new StringTaintVerifyingMV(mv, true, an);
		Type[] newArgTypes = Type.getArgumentTypes(desc);
		for (Type t : newArgTypes) {
			lastArg += t.getSize();
		}
		if((access & Opcodes.ACC_STATIC) == 0)
			lastArg++;
		this.forceFieldChecks = forceFieldChecks;
	}

	LocalVariableManager lvs;

	public void setLvs(LocalVariableManager lvs) {
		this.lvs = lvs;
	}

	public boolean doTaint;

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (doTaint) {
			String taintDesc = Configuration.TAINT_TAG_DESC;
			if (desc.startsWith("["))
				taintDesc = Configuration.TAINT_TAG_ARRAYDESC;
			switch (opcode) {
			case Opcodes.GETFIELD:
				Type fieldType = Type.getType(desc);
				super.visitInsn(DUP);
				super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, taintDesc);
				super.visitInsn(SWAP);
				if(stringVerifyingMV != null && TaintUtils.isPrimitiveArrayType(fieldType))
				{
					stringVerifyingMV.visitFieldInsn(opcode, owner, name, desc);
					analyzer.stackTaintedVector.set(analyzer.stack.size()-1, true);
				}
				else
				{
					analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
					super.visitFieldInsn(opcode, owner, name, desc);
				}
				break;
			case Opcodes.GETSTATIC:
				super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, taintDesc);
				analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
				super.visitFieldInsn(opcode, owner, name, desc);
				break;
			default:
				super.visitFieldInsn(opcode, owner, name, desc);
//				throw new IllegalArgumentException();
			}
			doTaint = false;
		} else
			super.visitFieldInsn(opcode, owner, name, desc);

	}

	void generateTaintArray() {
//		System.out.println("Pre generate taint array, stack tags are " + analyzer.stackTaintedVector);
//		System.out.println(analyzer.stack);
		FrameNode fn = getCurrentFrameNode();
//		removeTaintArraysFromStack(fn);
		analyzer.visitInsn(DUP);
		Label ok = new Label();
		Label isnull = new Label();
		analyzer.visitJumpInsn(IFNULL, isnull);
		analyzer.visitTypeInsn(NEW, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
		analyzer.visitInsn(DUP);
		analyzer.visitMethodInsn(INVOKESPECIAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "<init>", "()V", false);
		FrameNode fn2 = getCurrentFrameNode();
//		System.out.println("NMIDPT"+analyzer.stack);
//		System.out.println(analyzer.stackTaintedVector);
//		System.out.println(fn2.stack);
		analyzer.visitJumpInsn(Opcodes.GOTO, ok);
		analyzer.visitLabel(isnull);
		fn.accept(analyzer);
		analyzer.visitInsn(Opcodes.ACONST_NULL);
		analyzer.visitLabel(ok);
//		fn.stack = new LinkedList(fn.stack);
//		fn.stack.add(Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
//		System.out.println("FN stack is " + fn.stack);
		fn2.accept(analyzer);
//		System.out.println("New stack is " + analyzer.stack);
//		System.out.println("New stack is " + analyzer.stackTaintedVector);
		analyzer.visitInsn(SWAP);
//		System.out.println("POst genreate array " + analyzer.stack + ", " + analyzer.stackTaintedVector);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
//		if(opcode < 200)
//		System.out.println("PIMV" + Printer.OPCODES[opcode] +var);
//		System.out.println(analyzer.stack);
//		System.out.println(analyzer.locals);
//		if(opcode == Opcodes.ILOAD)
//			System.out.println("ILOAD " + var + " - " + analyzer.locals.get(var));
		if (doTaint) {
//			System.out.println("Dotaint on  var" + Printer.OPCODES[opcode] +var);
//			System.out.println(analyzer.stack);
//			System.out.println(analyzer.stackTaintedVector);
			Type shadowType = Type.getType(Configuration.TAINT_TAG_DESC);
			if (opcode == Opcodes.ALOAD || opcode == Opcodes.ASTORE)
				shadowType = Type.getType(Configuration.TAINT_TAG_ARRAYDESC);
			if (!lvs.varToShadowVar.containsKey(var))
			{
				if(var < lastArg)
				{
					lvs.varToShadowVar.put(var, var-1);
				}
				else
					lvs.varToShadowVar.put(var, lvs.newShadowLV(shadowType, var));
//				analyzer.locals.add(shadowType.getInternalName());
			}
			int shadow = lvs.varToShadowVar.get(var);
//			System.out.println("Shadow: " + shadow);
			switch (opcode) {
			case ALOAD:
				if (isPrimitiveStackType(analyzer.locals.get(var)) || analyzer.locals.get(var) == Opcodes.NULL) {
//					if (var <= lvs.lastArg) {
//						if (Configuration.SINGLE_TAG_PER_ARRAY) {
//							analyzer.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
//							analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
//							analyzer.visitVarInsn(Opcodes.ALOAD, var);
//						} else {
//							analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
//							analyzer.visitVarInsn(Opcodes.ALOAD, var);
//							generateTaintArray();
//						}
//					} else {
						super.visitVarInsn(Opcodes.ALOAD, shadow);
						analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
						super.visitVarInsn(Opcodes.ALOAD, var);
//					}
				} else
					super.visitVarInsn(opcode, var);
				break;
			case ILOAD:
			case FLOAD:
			case LLOAD:
			case DLOAD:
				super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, shadow);
				super.visitVarInsn(Opcodes.ALOAD, var);
				break;
			case ASTORE:
				super.visitVarInsn(Opcodes.ASTORE, var);
				super.visitVarInsn(Opcodes.ASTORE, shadow);
				break;
			case ISTORE:
			case FSTORE:
			case LSTORE:
			case DSTORE:
				super.visitVarInsn(opcode, var);
				super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, shadow);
				break;
			default:
				throw new IllegalArgumentException();
			}
			doTaint = false;
		} else {
			super.visitVarInsn(opcode, var);
		}
	}
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		super.visitFrame(type, nLocal, local, nStack, stack);
	}

	public void retrieveTaintedArray(String type) {
		//A
		Label isNull = new Label();
		Label isDone = new Label();

		FrameNode fn = getCurrentFrameNode();

//		System.out.println("RTA " + analyzer.stack);
//		System.out.println(".. and " + analyzer.stackTaintedVector);
		analyzer.visitInsn(DUP);
		analyzer.visitJumpInsn(IFNULL, isNull);

//				System.out.println("unbox: " + getTopOfStackType() + " type passed is " + type);

		Class boxType = MultiDTaintedArray.getClassForComponentType(Type.getType(type).getElementType().getSort());
		analyzer.visitTypeInsn(CHECKCAST, Type.getInternalName(boxType));

		analyzer.visitInsn(DUP);

		Type arrayDesc = Type.getType(type);
		//		System.out.println("Get tainted array from " + arrayDesc);
		//A A
		analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", Configuration.TAINT_TAG_ARRAYDESC);
		//A TA
		analyzer.visitInsn(SWAP);
		analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
		analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "val", type);
		
//		System.out.println("RTA2 " + analyzer.stack);
//		System.out.println(".. and " + analyzer.stackTaintedVector);
		FrameNode fn2 = getCurrentFrameNode();
		analyzer.visitJumpInsn(GOTO, isDone);
		analyzer.visitLabel(isNull);
		acceptFn(fn, analyzer);
		analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
		analyzer.visitInsn(ACONST_NULL);
		if (arrayDesc.getElementType().getSort() == Type.OBJECT)
			analyzer.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
		else
			analyzer.visitTypeInsn(CHECKCAST, Type.getType(TaintUtils.getShadowTaintType(arrayDesc.getDescriptor())).getInternalName());
		analyzer.visitInsn(SWAP);
		analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
		analyzer.visitTypeInsn(CHECKCAST, type);
		analyzer.visitLabel(isDone);
		acceptFn(fn2, analyzer);
		analyzer.visitInsn(NOP);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (doTaint) {
			doTaint = false;
			switch (opcode) {
			case Opcodes.CHECKCAST:
				Type t = Type.getType(type);
				if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT) {
					if (getTopOfStackObject() == Opcodes.NULL) {
						analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
						analyzer.visitTypeInsn(opcode, type);
						analyzer.visitInsn(Opcodes.ACONST_NULL);
						analyzer.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
						analyzer.visitInsn(SWAP);
					} else {
						retrieveTaintedArray(type);
					}
					return;
				} else
				{
					super.visitTypeInsn(opcode, type);
					return;
				}
			default:
				throw new IllegalArgumentException();
			}
		}
		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (doTaint) {
			switch (opcode) {
			case Opcodes.NEWARRAY:
				super.visitTypeInsn(NEW, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
				super.visitInsn(DUP);
				super.visitMethodInsn(INVOKESPECIAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "<init>", "()V", false);
				super.visitInsn(SWAP);
				analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
				super.visitIntInsn(opcode, operand);
				break;
			default:
				throw new IllegalArgumentException();
			}
		} else
			super.visitIntInsn(opcode, operand);
		doTaint = false;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//				System.out.println("Visit method: " + name+desc);
//				System.out.println(analyzer.stack);
		//		System.out.println(analyzer.stackTaintedVector);

		if (doTaint) {
			//Is it 0 args?
			boolean forceInstCall = false;
			if(Type.getArgumentTypes(desc).length == 0)
			{
				forceInstCall = true;
			}
			super.visitInsn(TaintUtils.DONT_LOAD_TAINT);
			analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
			if(forceInstCall)
				super.visitInsn(TaintUtils.NEXTLOAD_IS_TAINTED);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			super.visitInsn(TaintUtils.DONT_LOAD_TAINT);
			Type origReturnType = Type.getReturnType(desc);
			Type newReturnType = TaintUtils.getContainerReturnType(origReturnType);
			if (!forceInstCall && Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc, true)) {
				generateTaintArray();
			} else {
				if (origReturnType.getSort() == Type.ARRAY && origReturnType.getDimensions() == 1 && origReturnType.getElementType().getSort() != Type.OBJECT) {
					//unbox array
					analyzer.visitInsn(DUP);
					analyzer.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_ARRAYDESC);
					analyzer.visitInsn(SWAP);
					analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
					analyzer.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				} else if (origReturnType.getSort() != Type.ARRAY && origReturnType.getSort() != Type.OBJECT && origReturnType.getSort() != Type.VOID) {
					//unbox prim
					analyzer.visitInsn(DUP);
					analyzer.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
					analyzer.visitInsn(SWAP);
					analyzer.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
					analyzer.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				}
			}
			doTaint = false;
		} else
			super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
	boolean skip = false;

	@Override
	public void visitInsn(int opcode) {
		if(skip && opcode <= 200)
		{
			skip = false;
			super.visitInsn(opcode);
			return;
		}
		switch (opcode) {
		case TaintUtils.NEXT_INSN_TAINT_AWARE:
			doTaint = true;
			return;
		case TaintUtils.DONT_LOAD_TAINT:
//			System.out.println("skip on");
			skip=true;
			return;
		case Opcodes.ACONST_NULL:
			if (doTaint) {
				super.visitInsn(ACONST_NULL);
				super.visitInsn(TaintUtils.NEXT_INSN_TAINT_AWARE);
				super.visitInsn(ACONST_NULL);
				doTaint = false;
				return;
			} else
				break;
		case Opcodes.AALOAD:
			if (doTaint) {
				//				System.out.println("Tainted aaload");
				Type t = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
				analyzer.visitInsn(opcode);
				try {
					retrieveTaintedArray("[" + (MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", ".")))));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				;
				doTaint = false;
				return;
			} else
				break;
		case Opcodes.DUP:
			if(doTaint)
			{
				super.visitInsn(DUP2);
			}
			else
			{
				super.visitInsn(opcode);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-1, false);
			}
			return;
		case Opcodes.DUP2:
			if(doTaint)
			{
				boolean v0 = isTopOfStackTainted();
				if(v0)
				{
					boolean v1 = isStackTaintedAt(2);
					if(v1)
					{
						// 4 things on top
						LocalVariableNode[] d = storeToLocals(2);
						super.visitInsn(DUP2);
						super.visitVarInsn(ALOAD, d[0].index);
						super.visitVarInsn(ALOAD, d[1].index);
						super.visitVarInsn(ALOAD, d[0].index);
						super.visitVarInsn(ALOAD, d[1].index);
						freeLVs(d);
					}
					else
					{
						LocalVariableNode[] d = storeToLocals(2);
						super.visitInsn(DUP);
						super.visitVarInsn(ALOAD, d[0].index);
						super.visitVarInsn(ALOAD, d[1].index);
						super.visitVarInsn(ALOAD, d[0].index);
						super.visitVarInsn(ALOAD, d[1].index);
						freeLVs(d);
					}
				}
				else if(isStackTaintedAt(1))
				{
					LocalVariableNode[] d = storeToLocals(1);
					super.visitInsn(DUP2);
					super.visitVarInsn(ALOAD, d[0].index);
					super.visitVarInsn(ALOAD, d[0].index);
					freeLVs(d);
				}
			}
			else
			{
				super.visitInsn(opcode);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-1, false);
			}
			return;

		case Opcodes.DUP_X2:
//			System.out.println("Pre dupx2" + analyzer.stack);
//			System.out.println(analyzer.stackTaintedVector);
			if(doTaint)
			{
				boolean v0 = isTopOfStackTainted();
				Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
				if (v0) {
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
					boolean v1 = isStackTaintedAt(2);
					if (v1) {
						if (getStackElementSize(underThisOne) == 2) {
							//Dup the top 2 elements to be under the 3 beneath.
							DUPN_XU(2, 3);
						} else {
							//top el is 2, next is 2
							Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isStackTaintedAt(4)) {
								//Dup the top 2 under the next 4
								DUPN_XU(2, 4);
							} else {
								//Dup the top 2 under the next 3
								DUPN_XU(2, 3);
							}
						}
					} else {//top is primitive, second is not
						Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isStackTaintedAt(3)) {
							//Dup the top 2 under the next 3
							DUPN_XU(2, 3);
						} else {
							//Dup the top 2 under the next 2
							super.visitInsn(DUP2_X2);
						}
					}
				} else { //top is not primitive
					Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 2);
					if (isStackTaintedAt(1)) {
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
							if (isStackTaintedAt(3)) {
								//Dup the top 1 under the next 4
								DUPN_XU(1, 4);
							} else {
								//Dup the top 1 under the next 3
								DUPN_XU(1, 3);
							}
						}
					} else {//top is not primitive, second is not
						Object threeUnder = analyzer.stack.get(analyzer.stack.size() - 3);
						if (isStackTaintedAt(2)) {
							//Dup the top 1 under the next 3
							DUPN_XU(1, 3);
						} else {
							//Dup the top 1 under the next 2
							super.visitInsn(DUP_X2);
						}
					}
				}
			}
			else
			{
				if(isTopOfStackTainted())
				{
//					System.out.println("Top tainted dupx2");
					int tmp = lvs.getTmpLV();
					super.visitInsn(SWAP); // D I A T
					super.visitVarInsn(ASTORE, tmp); //D I A
					super.visitInsn(DUP_X2); //A D I T
					super.visitVarInsn(ALOAD, tmp);
					super.visitInsn(SWAP);
					lvs.freeTmpLV(tmp);
					analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-5, false);
				}
				else
				{
					super.visitInsn(opcode);
					analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-4, false);
				}
			}
//			System.out.println("post dupx2" + analyzer.stack);
//			System.out.println(analyzer.stackTaintedVector);
			return;
		case Opcodes.DUP2_X1:
			if(doTaint)
			{
				Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
				if (isStackTaintedAt(0)) {
					if (getStackElementSize(topOfStack) == 2) {
						//Have two-word el + 1 word taint on top
						Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isStackTaintedAt(3)) {
							//Dup the top three words to be under the 2 words beneath them
							DUPN_XU(2, 2);
						} else {
							//Dup the top three words to be under the word beneath them
							DUPN_XU(2, 1);
						}
					} else // top is 1 word, primitive
					{
						Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 3);
						if (isStackTaintedAt(2)) {
							//top is primitive, second is primitive
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isStackTaintedAt(4)) {
								// Dup the top four words to be under the 2 beneath them
								DUPN_XU(4, 2);
							} else {
								// dup the top four words to be under the 1 beneath
								DUPN_XU(4, 1);
							}
						} else {
							//top is primitive, second is not
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
							if (isStackTaintedAt(3)) {
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
					if (isStackTaintedAt(1)) {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isStackTaintedAt(4)) {
							// Dup the top 3 words to be under the 2 beneath
							DUPN_XU(3, 2);
						} else {
							// dup the top 3 words to be under the 1 beneath
							DUPN_XU(3, 1);
						}
					} else {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 3);
						if (isStackTaintedAt(2)) {
							// Dup the top 2 words to be under the 2 beneath
							super.visitInsn(DUP2_X2);
						} else {
							// dup the top 2 words to be under the 1 beneath
							super.visitInsn(DUP2_X1);
						}
					}
				}
			}
			else
			{
				super.visitInsn(opcode);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-4, false);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-5, false);
			}
			return;
		case Opcodes.DUP2_X2:
			if(doTaint)
			{
				Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
				if (isStackTaintedAt(0)) {
					if (getStackElementSize(topOfStack) == 2) {
						//Have two-word el + 1 word taint on top
						Object underThisOne = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isStackTaintedAt(3)) {
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isStackTaintedAt(5)) {
								//Dup the top three words to be under the 4 words beneath them
								DUPN_XU(2, 4);
							} else {
								//Dup the top three words to be under the 3 words beneath them
								DUPN_XU(2, 3);
							}
						} else {
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isStackTaintedAt(4)) {
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
						if (isStackTaintedAt(2)) {
							//top is primitive, second is primitive
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isStackTaintedAt(4)) {
								Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
								if (isStackTaintedAt(5)) {
									DUPN_XU(4, 4);
								} else {
									DUPN_XU(4, 3);
								}
							} else {
								Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
								if (isStackTaintedAt(5)) {
									DUPN_XU(4, 3);
								} else {
									DUPN_XU(4, 2);
								}
							}
						} else {
							//top is primitive, second is not
							Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
							if (isStackTaintedAt(3)) {
								Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
								if (isStackTaintedAt(5)) {
									DUPN_XU(3, 4);
								} else {
									DUPN_XU(3, 3);
								}

							} else {
								Object fourDown = analyzer.stack.get(analyzer.stack.size() - 5);
								if (isStackTaintedAt(4)) {
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
					if (isStackTaintedAt(1)) {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 4);
						if (isStackTaintedAt(3)) {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isStackTaintedAt(5)) {
								DUPN_XU(3, 4);
							} else {
								DUPN_XU(3, 3);
							}

						} else {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 6);
							if (isStackTaintedAt(5)) {
								DUPN_XU(3, 3);
							} else {
								DUPN_XU(3, 2);
							}
						}
					} else {
						Object threeDown = analyzer.stack.get(analyzer.stack.size() - 3);
						if (isStackTaintedAt(2)) {
							super.visitInsn(DUP2_X2);
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 5);
							if (isStackTaintedAt(4)) {
								DUPN_XU(2, 4);
							} else {
								DUPN_XU(2, 3);
							}

						} else {
							Object fourDown = analyzer.stack.get(analyzer.stack.size() - 4);
							if (isStackTaintedAt(3)) {
								DUPN_XU(2, 3);
							} else {
								super.visitInsn(DUP2_X2);
							}
						}
					}
				}
			}
			else
			{
				super.visitInsn(opcode);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-5, false);
				analyzer.stackTaintedVector.set(analyzer.stackTaintedVector.size()-6, false);
			}
			return;
		case Opcodes.POP:
			if(doTaint)
				super.visitInsn(POP2);
			else
				super.visitInsn(POP);
			return;
		case Opcodes.POP2:
			if(doTaint)
			{
				super.visitInsn(POP2);
				super.visitInsn(POP2);
			}
			else
				super.visitInsn(POP2);
			return;
		case Opcodes.SWAP:
			if(doTaint)
			{
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
			}
			else
				super.visitInsn(opcode);
			return;
		case Opcodes.DUP_X1:
			if (doTaint) {
				boolean v0 = isTopOfStackTainted();
				boolean v1 = isStackTaintedAt(1);
				if (v0) {
					if (v1) {
						// X X T V -> T V X X T V
						super.visitInsn(DUP2_X2);
					} else {
						//X T V -> T V X TV
						super.visitInsn(DUP2_X1);
					}
				} else {
					if (v1) {
						// X X V -> V X X V
						super.visitInsn(DUP_X2);
					} else {
						//X V -> V X V
						super.visitInsn(DUP_X1);
					}
				}
			}
			else
			{
				//plan is STILL do to dup_x1, but ONLY dup the top item
				boolean v0 = isTopOfStackTainted();
				boolean v1 = isStackTaintedAt(1);
				if (v0) {
					if (v1) {
						// X X T V -> V X X T V
						super.visitInsn(SWAP);
						//X X V T
						Type t1=  getTopOfStackType();
						int lv1 = lvs.getTmpLV(t1);
						super.visitVarInsn(t1.getOpcode(ISTORE), lv1);
						//X X V
						super.visitInsn(DUP_X2);
						super.visitVarInsn(t1.getOpcode(ILOAD), lv1);
						super.visitInsn(SWAP);
					} else {
						//X T V -> V X T V
						super.visitInsn(DUP_X2);
					}
				} else {
					if (v1) {
						// X X V -> V X X V
						super.visitInsn(DUP_X2);
					} else {
						//X V -> V X V
						super.visitInsn(DUP_X1);
					}
				}
			}
			return;
		default:
//			if(opcode < 200)
//				System.out.println(Printer.OPCODES[opcode] + analyzer.stackTaintedVector +", " + analyzer.stack);
			doTaint = false;
		}
		super.visitInsn(opcode);
	}
}
