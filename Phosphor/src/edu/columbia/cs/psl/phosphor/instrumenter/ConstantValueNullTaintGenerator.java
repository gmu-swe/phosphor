package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.AnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FieldInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.InsnList;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.InsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.IntInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.TypeInsnNode;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class ConstantValueNullTaintGenerator extends MethodVisitor implements Opcodes {
	public ConstantValueNullTaintGenerator(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5,access, name, desc, signature, exceptions) {
			@Override
			public void visitEnd() {
				final MethodNode uninstrumented = new MethodNode(api, access, name, desc, signature, exceptions.toArray(new String[4]));
				uninstrumented.instructions = new InsnList();
				AbstractInsnNode i = instructions.getFirst();
				if (i != null) {
					while (i.getNext() != null) {
						uninstrumented.instructions.add(i);
						i = i.getNext();
					}
					uninstrumented.instructions.add(i);
				}
				this.accept(new MethodNode(api, access, name, desc, signature, exceptions.toArray(new String[4])) {

					boolean hasNonConstantOps = false;

					boolean dontLoadTaint = false;

					@Override
					public void visitInsn(int opcode) {
						if (opcode == TaintUtils.DONT_LOAD_TAINT || opcode == TaintUtils.IGNORE_EVERYTHING) {
							dontLoadTaint = !dontLoadTaint;
//							System.out.println(name);
//							System.out.println("A skip");
							super.visitInsn(opcode);
							return;
						}
						if (dontLoadTaint) {
//							System.out.println("222A skip");
							super.visitInsn(opcode);
							return;
						}
						switch (opcode) {
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
							super.visitInsn(TaintUtils.RAW_INSN);
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitInsn(opcode);
							super.visitInsn(TaintUtils.RAW_INSN);
							return;
						default:
							super.visitInsn(opcode);
						}
					}

					@Override
					public void visitIntInsn(int opcode, int operand) {
						if (dontLoadTaint) {
							super.visitIntInsn(opcode, operand);
							return;
						}
						switch (opcode) {
						case Opcodes.BIPUSH:
						case Opcodes.SIPUSH:
							super.visitInsn(TaintUtils.RAW_INSN);
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitIntInsn(opcode, operand);
							super.visitInsn(TaintUtils.RAW_INSN);
							break;
						case Opcodes.NEWARRAY:
							super.visitIntInsn(opcode, operand);
							break;
						default:
							super.visitIntInsn(opcode, operand);
						}
					}

					@Override
					public void visitLdcInsn(Object cst) {
						if (dontLoadTaint) {
							super.visitLdcInsn(cst);
							return;
						}
						super.visitInsn(TaintUtils.RAW_INSN);
						if (cst instanceof Integer) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Byte) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Character) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Short) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Boolean) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Float) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Long) {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof Double) {
//							System.out.println("CVNT"+name+"LDC " + cst);
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitLdcInsn(cst);
						} else if (cst instanceof String) {
							super.visitLdcInsn(cst);
						} else {
							super.visitLdcInsn(cst);
						}
						super.visitInsn(TaintUtils.RAW_INSN);
					}

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
						Type[] args = Type.getArgumentTypes(desc);
						for (Type t : args) {
							if (!(t.getSort() == Type.OBJECT || (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT))) {
								hasNonConstantOps = true;
							}
						}
						super.visitMethodInsn(opcode, owner, name, desc,itfc);
					}

					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						super.visitFieldInsn(opcode, owner, name, desc);
						if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
							hasNonConstantOps = true;
						} else if (opcode == Opcodes.GETSTATIC && owner != className) {
							hasNonConstantOps = true;
						} else {
							//							Type field = Type.getType(desc);
							//							if(field.getSort() != Type.OBJECT && field.getSort() != Type.ARRAY)
							//								hasNonConstantOps = true;
						}
					}

					@Override
					public void visitEnd() {
						AbstractInsnNode insn = this.instructions.getFirst();
						if (hasNonConstantOps && this.instructions.size() > 30000) {
//							System.out.println("Bailing on " + className + "." + name + "cuz it's already got " + this.instructions.size());
							uninstrumented.instructions.insertBefore(uninstrumented.instructions.getFirst(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
							uninstrumented.instructions.add(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
							uninstrumented.accept(cmv);
							return;
						}
						if (!hasNonConstantOps) {
//																					System.out.println("Possible candidate for removing all constant registrations: " + this.name);
							int nInsn = this.instructions.size();
//																					System.out.println(nInsn);
//																					System.out.println(uninstrumented.instructions.size());
							if (nInsn > 30000 || (Configuration.IMPLICIT_TRACKING && nInsn > 23000)) {
//								System.out.println("Removing constant load ops: " + className + "." + this.name);
								uninstrumented.instructions.insertBefore(uninstrumented.instructions.getFirst(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
								uninstrumented.instructions.add(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
								insn = uninstrumented.instructions.getFirst();
//								HashMap<String, Type> accessedMultiDArrays = new HashMap<String, Type>();
								boolean isRaw = false;
								AnalyzerAdapter an = new AnalyzerAdapter(className, access, name, desc, null);

								while (insn != null) {
									switch (insn.getOpcode()) {
									case TaintUtils.RAW_INSN:
										isRaw = !isRaw;
										break;
									case Opcodes.MULTIANEWARRAY:
										MultiANewArrayInsnNode main = (MultiANewArrayInsnNode) insn;

										Type arrayType = Type.getType(main.desc);
										Type origType = Type.getType(main.desc);
										boolean needToHackDims = false;
										if (arrayType.getElementType().getSort() != Type.OBJECT) {
											if (main.dims == arrayType.getDimensions()) {
												needToHackDims = true;
											}
											arrayType = MultiDTaintedArray.getTypeForType(arrayType);
											//Type.getType(MultiDTaintedArray.getClassForComponentType(arrayType.getElementType().getSort()));
											main.desc = arrayType.getInternalName();
										}
										if (needToHackDims) {
											if (main.dims == 2) {
												uninstrumented.instructions.insertBefore(insn, new InsnNode(SWAP));
											} else if (main.dims == 3) {
												uninstrumented.instructions.insertBefore(insn, new InsnNode(DUP_X2));
												uninstrumented.instructions.insertBefore(insn, new InsnNode(POP));
											} else {
												throw new IllegalArgumentException();
											}
											//Stack has Capacity repeated dims times
											main.dims--;
											//NB that this is backwards
											uninstrumented.instructions.insert(insn, new MethodInsnNode(INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)), "initLastDim",
													"([Ljava/lang/Object;I)V",false));
											uninstrumented.instructions.insert(insn, new IntInsnNode(BIPUSH, origType.getSort()));
											uninstrumented.instructions.insert(insn, new InsnNode(DUP));

										}
										break;
									case Opcodes.ANEWARRAY:
										TypeInsnNode tin = (TypeInsnNode) insn;
										Type t = Type.getType(tin.desc);
										if (t.getElementType().getDescriptor().length() == 1) {
											//e.g. [I for a 2 D array -> MultiDTaintedIntArray
											tin.desc = MultiDTaintedArray.getTypeForType(t).getInternalName();
										}
										break;
									case Opcodes.GETSTATIC:
									case Opcodes.GETFIELD:
										FieldInsnNode fin = (FieldInsnNode) insn;
										t = Type.getType(fin.desc);
										if (!isRaw && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
											//											uninstrumented.instructions.insert(fin, new TypeInsnNode(Opcodes.CHECKCAST, t.getDescriptor()));
											//											uninstrumented.instructions.insert(fin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxRaw",
											//													"(Ljava/lang/Object;)Ljava/lang/Object;"));
											fin.desc = MultiDTaintedArray.getTypeForType(Type.getType(fin.desc)).getDescriptor();
										}
										break;
									case Opcodes.PUTSTATIC:
									case Opcodes.PUTFIELD:
										fin = (FieldInsnNode) insn;
										t = Type.getType(fin.desc);
										switch (t.getSort()) {
										case Type.INT:
										case Type.BOOLEAN:
										case Type.BYTE:
										case Type.CHAR:
										case Type.SHORT:
										case Type.FLOAT:
											uninstrumented.instructions.insertBefore(insn, new InsnNode(Configuration.NULL_TAINT_LOAD_OPCODE));
											uninstrumented.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, fin.owner, fin.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC));
											break;
										case Type.LONG:
										case Type.DOUBLE:
											uninstrumented.instructions.insertBefore(insn, new InsnNode(Configuration.NULL_TAINT_LOAD_OPCODE));
											uninstrumented.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, fin.owner, fin.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC));
											break;
										case Type.ARRAY:
											switch (t.getElementType().getSort()) {
											case Type.INT:
											case Type.BOOLEAN:
											case Type.BYTE:
											case Type.CHAR:
											case Type.DOUBLE:
											case Type.FLOAT:
											case Type.LONG:
											case Type.SHORT:
//												String taintDesc = t.getDescriptor().substring(0, t.getDescriptor().length() - 1) + "I";
												if (t.getDimensions() > 1) {
													uninstrumented.instructions.insertBefore(fin, new IntInsnNode(Opcodes.BIPUSH, t.getSort()));
													uninstrumented.instructions.insertBefore(fin, new IntInsnNode(Opcodes.BIPUSH, t.getDimensions()));
													uninstrumented.instructions.insertBefore(fin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)),
															"initWithEmptyTaints", "([Ljava/lang/Object;II)[Ljava/lang/Object;",false));
													uninstrumented.instructions.insertBefore(fin, new TypeInsnNode(Opcodes.CHECKCAST, t.getDescriptor()));

													fin.desc = MultiDTaintedArray.getTypeForType(Type.getType(fin.desc)).getDescriptor();
												} else {
													uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
													//Initialize a new 1D array of the right length
													uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
													uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.ARRAYLENGTH));
													if(!Configuration.MULTI_TAINTING)
														uninstrumented.instructions.insertBefore(insn, new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
													else
														uninstrumented.instructions.insertBefore(insn, new TypeInsnNode(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME));
													//													uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
													uninstrumented.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, fin.owner, fin.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC));
												}
												//												uninstrumented.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class),
												//														"registerAllConstantsArray", "(Ljava/lang/Object;Ljava/lang/Object;)V"));
												break;
											case Type.OBJECT:
												//												uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
												//												uninstrumented.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "registerAllConstants",
												//														"(Ljava/lang/Object;)V"));
											}

											break;
										}

										break;
									case Opcodes.AASTORE:
										if (an.stack.get(an.stack.size() - 1) instanceof String) {
											Type storeType = Type.getObjectType((String) an.stack.get(an.stack.size() - 1));
											if (storeType.getSort() == Type.ARRAY && storeType.getElementType().getSort() != Type.OBJECT) {
												uninstrumented.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)),
														"boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;",false));
												uninstrumented.instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, MultiDTaintedArray.getTypeForType(storeType).getInternalName()));
											}
										}
										break;
									case Opcodes.ARETURN:
										//WOOOOOAHHHH we are assuming that we can *only* be putstatic'ing on objects or arrays, so always 1 word
										//										uninstrumented.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
										//										uninstrumented.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "registerAllConstants",
										//												"(Ljava/lang/Object;)V"));
									default:
										break;
									}
									if (insn.getOpcode() < 200)
										insn.accept(an);
									insn = insn.getNext();
								}
								uninstrumented.accept(cmv);
								return;
							}

						} else {
							boolean isIgnore = false;
//							System.err.println("ZZZZZ"+name);
							while (insn != null) {

								switch (insn.getOpcode()) {
								case TaintUtils.IGNORE_EVERYTHING:
									isIgnore = !isIgnore;
									break;
								case Opcodes.INVOKEVIRTUAL:
								case Opcodes.INVOKESTATIC:
								case Opcodes.INVOKEINTERFACE:
								case Opcodes.INVOKESPECIAL:
									if (isIgnore)
										break;
									/*
									 * New optimization: If we are going to pop
									 * the primitve return of a method call,
									 * thendon't both unboxing it.
									 */

									MethodInsnNode min = (MethodInsnNode) insn;
									if (Instrumenter.isIgnoredClass(min.owner))
										break;
									Type ret = Type.getReturnType(min.desc);
									if (ret.getSort() != Type.VOID && ret.getSort() != Type.OBJECT && ret.getSort() != Type.ARRAY) {
										if (ret.getSize() == 1 && insn.getNext() != null && insn.getNext().getType() == AbstractInsnNode.INSN && insn.getNext().getOpcode() == Opcodes.POP) {
											//											System.out.println(name +desc+ "pop" + min.owner + min.name + min.desc);
											//											System.out.println("Next is " + insn.getNext().getOpcode() + "--" + insn.getNext().getType());
											instructions.insertBefore(insn, new InsnNode(TaintUtils.NO_TAINT_UNBOX));
										} else if (ret.getSize() == 2 && insn.getNext() != null && insn.getNext().getType() == AbstractInsnNode.INSN && insn.getNext().getOpcode() == Opcodes.POP2) {
											//											System.out.println("pop2");

											instructions.insertBefore(insn, new InsnNode(TaintUtils.NO_TAINT_UNBOX));
											instructions.remove(insn.getNext());
											instructions.insert(insn, new InsnNode(Opcodes.POP));

										}
									}
									break;
								case Opcodes.AASTORE:
								case Opcodes.IASTORE:
								case Opcodes.LASTORE:
								case Opcodes.FASTORE:
								case Opcodes.DASTORE:
								case Opcodes.BASTORE:
								case Opcodes.CASTORE:
								case Opcodes.SASTORE:
									if (isIgnore)
										break;
									/**
									 * value ??value-taint -- NEVER for arrays
									 * here, ONLY on primitives, and at that,
									 * ONLY if the primitive came directly from
									 * a BIPUSH or LDC index index-taint
									 * arrayref
									 */
									AbstractInsnNode previous = insn.getPrevious();
									if (previous.getOpcode() == TaintUtils.RAW_INSN) {
										/*
										 * the plan, that makes sense in my head
										 * now is that at this point, we know
										 * that we are storing a constant into
										 * an array, so we want to leave the
										 * taint on the constant, but remove the
										 * taint settings on the index, if it's
										 * also constant. we know the distance
										 * back in instructions to get to the
										 * index load, because we know that the
										 * value is constant, and therefore has
										 * the number of instructions needed
										 * specified above, guarded by the insn
										 * RAW_INSN - so maybe just wind back
										 * until we see a second RAW_INSN
										 */
										//Skip back 3 insns for primitive, 2 for string
										//										if(previous.getType() == AbstractInsnNode.LDC_INSN && ((LdcInsnNode)previous).cst instanceof String)
										//											previous = previous.getPrevious();
										previous = previous.getPrevious();
										//																				System.out.println("1Now prev is " + previous.getOpcode());
										while (previous.getOpcode() != TaintUtils.RAW_INSN)
											previous = previous.getPrevious();
										//																				System.out.println("2Now prev is " + previous.getOpcode());
										previous = previous.getPrevious();

										while (previous.getType() == AbstractInsnNode.LINE || previous.getType() == AbstractInsnNode.LABEL)
											previous = previous.getPrevious(); //haha linebreaks suck
										//																				System.out.println("3Now prev is " + previous.getOpcode() + " ---- " + previous.getType());
										if (previous.getOpcode() == TaintUtils.RAW_INSN) {
											previous = previous.getPrevious();
											while (previous.getOpcode() != TaintUtils.RAW_INSN)
												previous = previous.getPrevious();
											previous = previous.getNext();
//											AbstractInsnNode theIndex = previous;
											//											System.out.println("insn is " + insn.getOpcode());
											//IDX_TAINT IDX VAL XASTORE (insn)
											//											System.out.println("prev removed is  " + previous.getOpcode());
											this.instructions.remove(previous);
											//											previous = previous.getNext();

											//											System.out.println("prev is  " + previous.getOpcode());

											//											while (previous.getOpcode() != TaintUtils.RAW_INSN) {
											//												AbstractInsnNode tmp = previous.getNext();
											//												//												System.out.println("Popping insn " + previous.getOpcode());
											//												this.instructions.remove(previous);
											//												previous = tmp;
											//											}
											this.instructions.insertBefore(insn, new InsnNode(TaintUtils.NO_TAINT_STORE_INSN));
											//											System.out.println("double-constant array store found in " + this.name + " op is " + theIndex.getOpcode());
										}
										//										else
										//											System.out.println("Prev before a-store:" + previous.getOpcode() + " in " + this.name);
									}
									break;
								case Opcodes.LALOAD:
								case Opcodes.DALOAD:
								case Opcodes.IALOAD:
								case Opcodes.FALOAD:
								case Opcodes.BALOAD:
								case Opcodes.CALOAD:
								case Opcodes.SALOAD:
								case Opcodes.AALOAD:
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
								case Opcodes.FADD:
								case Opcodes.FREM:
								case Opcodes.FSUB:
								case Opcodes.FMUL:
								case Opcodes.FDIV:
								case Opcodes.DADD:
								case Opcodes.DSUB:
								case Opcodes.DMUL:
								case Opcodes.DDIV:
								case Opcodes.DREM:
								case Opcodes.LSUB:
								case Opcodes.LMUL:
								case Opcodes.LADD:
								case Opcodes.LDIV:
								case Opcodes.LREM:
								case Opcodes.LAND:
								case Opcodes.LOR:
								case Opcodes.LXOR:
//									previous = insn.getPrevious();
//									while (previous.getType() == AbstractInsnNode.LINE || previous.getType() == AbstractInsnNode.LABEL)
//										previous = previous.getPrevious(); //haha linebreaks suck
//									if (previous.getOpcode() == TaintUtils.RAW_INSN) {
//										this.instructions.remove(previous.getPrevious().getPrevious());
//										this.instructions.insertBefore(insn, new InsnNode(TaintUtils.NO_TAINT_STORE_INSN));
//									}
									break;
								}
								insn = insn.getNext();
							}
						}

						this.accept(cmv);
					}
				});
			}
		});
		//		super(Opcodes.ASM4,cmv);
	}
}
