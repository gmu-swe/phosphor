package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.InsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.Printer;

public class UnnecessaryTaintLoadRemover extends MethodVisitor implements Opcodes {
	public UnnecessaryTaintLoadRemover(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5,access, name, desc, signature, exceptions) {
			@Override
			public void visitEnd() {
				super.visitEnd();
				NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc, null);
				List[] stacks = new List[this.instructions.size() + 1];
				int insnN = 0;
				AbstractInsnNode insn = this.instructions.getFirst();
				while (insn != null) {
					if (analyzer.stack != null)
						stacks[insnN] = new ArrayList(analyzer.stack);
					if(insn.getType() == AbstractInsnNode.METHOD_INSN)
					{
						MethodInsnNode min = ((MethodInsnNode) insn);
						if(min.name.equals("valueOf") && (min.owner.equals("java/lang/Long") || min.owner.equals("java/lang/Short") ||
								min.owner.equals("java/lang/Double") || min.owner.equals("java/lang/Float") ||
								min.owner.equals("java/lang/Integer") || min.owner.equals("java/lang/Character")
								||min.owner.equals("java/lang/Byte") || min.owner.equals("java/lang/Boolean")))
						{
							switch(insn.getPrevious().getOpcode())
							{
							case BIPUSH:
							case SIPUSH:
							case ICONST_M1:
							case ICONST_0:
							case ICONST_1:
							case ICONST_2:
							case ICONST_3:
							case ICONST_4:
							case ICONST_5:
							case LCONST_0:
							case LCONST_1:
							case FCONST_0:
							case FCONST_1:
							case FCONST_2:
							case DCONST_0:
							case DCONST_1:
							case LDC:
								//Can avoid loading this taint!
								this.instructions.insertBefore(insn.getPrevious(), new InsnNode(TaintUtils.DONT_LOAD_TAINT));
								this.instructions.insert(insn, new InsnNode(TaintUtils.DONT_LOAD_TAINT));

								break;
							}
						}
					}
					else if ((insn.getType() == AbstractInsnNode.JUMP_INSN && insn.getOpcode() != Opcodes.GOTO) || insn.getType() == AbstractInsnNode.TABLESWITCH_INSN
							|| insn.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
						boolean canAvoidTaint = false;
						if (insn.getPrevious().getType() == AbstractInsnNode.LDC_INSN)
							canAvoidTaint = true;
						switch (insn.getPrevious().getOpcode()) {
						case ALOAD:
						case ILOAD:
						case DLOAD:
						case FLOAD:
						case BIPUSH:
						case SIPUSH:
						case ICONST_M1:
						case ICONST_0:
						case ICONST_1:
						case ICONST_2:
						case ICONST_3:
						case ICONST_4:
						case ICONST_5:
						case LCONST_0:
						case LCONST_1:
						case FCONST_0:
						case FCONST_1:
						case FCONST_2:
						case DCONST_0:
						case DCONST_1:
						case GETFIELD:
						case INSTANCEOF:
							canAvoidTaint = true;
							break;
						case IALOAD:
							AbstractInsnNode firstLoad = null;
							boolean hasCalls = false;
							if (insn.getOpcode() == Opcodes.IF_ACMPEQ || insn.getOpcode() == Opcodes.IF_ACMPNE || insn.getOpcode() == Opcodes.IF_ICMPEQ || insn.getOpcode() == Opcodes.IF_ICMPGE
									|| insn.getOpcode() == Opcodes.IF_ICMPGT || insn.getOpcode() == Opcodes.IF_ICMPLE || insn.getOpcode() == Opcodes.IF_ICMPLT || insn.getOpcode() == Opcodes.IF_ICMPNE) {
								AbstractInsnNode prev = insn.getPrevious();
								int j = insnN;
								int heightToFind = analyzer.stack.size();
								while (prev != null) {
									if (prev.getType() == AbstractInsnNode.METHOD_INSN || prev.getType() == AbstractInsnNode.FRAME)
										hasCalls = true;
									switch(prev.getOpcode())
									{
									case ISTORE:
									case LSTORE:
									case FSTORE:
									case DSTORE:
									case IASTORE:
									case CASTORE:
									case BASTORE:
									case FASTORE:
									case AASTORE:
									case ASTORE:
									case LASTORE:
									case DASTORE:
									case SASTORE:
										hasCalls = true;
									}
									if (stacks[j] != null && stacks[j].size() == heightToFind - 2) {
										break;
									}
									prev = prev.getPrevious();
									j--;
								}
								if (!hasCalls && prev != null) {
									if(TaintUtils.DEBUG_OPT){
									System.out.println(insn.getPrevious() +" is prev");
									System.out.println("UTLR " + name + desc +", add after "+prev + " and " + insn);
									}
									if(prev.getOpcode() == TaintUtils.DONT_LOAD_TAINT)
									{
										this.instructions.remove(prev);
									}
									else
										this.instructions.insert(prev, new InsnNode(TaintUtils.DONT_LOAD_TAINT));
									this.instructions.insert(insn, new InsnNode(TaintUtils.DONT_LOAD_TAINT));
								}
							}
							break;
						}
						boolean isDouble = false;
						boolean isTriple = false;
						if (canAvoidTaint
								&& (insn.getOpcode() == Opcodes.IF_ACMPEQ || insn.getOpcode() == Opcodes.IF_ACMPNE || insn.getOpcode() == Opcodes.IF_ICMPEQ || insn.getOpcode() == Opcodes.IF_ICMPGE
										|| insn.getOpcode() == Opcodes.IF_ICMPGT || insn.getOpcode() == Opcodes.IF_ICMPLE || insn.getOpcode() == Opcodes.IF_ICMPLT || insn.getOpcode() == Opcodes.IF_ICMPNE)) {
							canAvoidTaint = false;
							isDouble = true;
							if (insn.getPrevious().getPrevious().getType() == AbstractInsnNode.LDC_INSN)
								canAvoidTaint = true;
							switch (insn.getPrevious().getPrevious().getOpcode()) {
							case ALOAD:
								if (insn.getPrevious().getOpcode() == GETFIELD || insn.getPrevious().getOpcode() == INSTANCEOF) {
									isDouble = false;
									canAvoidTaint = false;
									isTriple = true;
									if (insn.getPrevious().getPrevious().getPrevious().getType() == AbstractInsnNode.LDC_INSN)
										canAvoidTaint = true;
									switch (insn.getPrevious().getPrevious().getPrevious().getOpcode()) {
									case ALOAD:
									case ILOAD:
									case DLOAD:
									case FLOAD:
									case BIPUSH:
									case SIPUSH:
									case ICONST_M1:
									case ICONST_0:
									case ICONST_1:
									case ICONST_2:
									case ICONST_3:
									case ICONST_4:
									case ICONST_5:
									case LCONST_0:
									case LCONST_1:
									case FCONST_0:
									case FCONST_1:
									case FCONST_2:
									case DCONST_0:
									case DCONST_1:
									case GETFIELD:
										canAvoidTaint = true;
										break;
									}
								} else
									canAvoidTaint = true;
								break;
							case ILOAD:
							case DLOAD:
							case FLOAD:
							case BIPUSH:
							case SIPUSH:
							case ICONST_M1:
							case ICONST_0:
							case ICONST_1:
							case ICONST_2:
							case ICONST_3:
							case ICONST_4:
							case ICONST_5:
							case LCONST_0:
							case LCONST_1:
							case FCONST_0:
							case FCONST_1:
							case FCONST_2:
							case DCONST_0:
							case DCONST_1:
								canAvoidTaint = true;
								break;
							}
						}
						if (canAvoidTaint) {
							if (isDouble)
								this.instructions.insertBefore(insn.getPrevious().getPrevious(), new InsnNode(TaintUtils.DONT_LOAD_TAINT));
							else if (isTriple)
								this.instructions.insertBefore(insn.getPrevious().getPrevious().getPrevious(), new InsnNode(TaintUtils.DONT_LOAD_TAINT));
							else {
								if(TaintUtils.DEBUG_OPT)
								System.out.println("UTR " + name+"ignore before " + Printer.OPCODES[insn.getPrevious().getOpcode()]);
								this.instructions.insertBefore(insn.getPrevious(), new InsnNode(TaintUtils.DONT_LOAD_TAINT));
							}
							if(TaintUtils.DEBUG_OPT)
							System.out.println("And after " + Printer.OPCODES[insn.getOpcode()]);
							this.instructions.insert(insn, new InsnNode(TaintUtils.DONT_LOAD_TAINT));
						}
					} else if (insn.getOpcode() == Opcodes.INVOKESTATIC || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						MethodInsnNode minsn = (MethodInsnNode) insn;
						if (Instrumenter.isPure(minsn.owner, minsn.name, minsn.desc)) {
							Type[] args = Type.getArgumentTypes(minsn.desc);
							boolean onlyVarLoadsOrConstants = true;
							AbstractInsnNode firstArgInsn = insn;
							if (args.length > 0) {
								for (int i = 0; i < args.length; i++) {
									firstArgInsn = firstArgInsn.getPrevious();
									if (args[i].getSort() != Type.ARRAY && args[i].getSort() != Type.OBJECT) {
										switch (firstArgInsn.getOpcode()) {
										case SIPUSH:
										case BIPUSH:
										case ICONST_M1:
										case ICONST_0:
										case ICONST_1:
										case ICONST_2:
										case ICONST_3:
										case ICONST_4:
										case ICONST_5:
										case LCONST_0:
										case LCONST_1:
										case FCONST_0:
										case FCONST_1:
										case FCONST_2:
										case DCONST_0:
										case DCONST_1:
										case ALOAD:
										case LDC:
											break;
										case ILOAD:
										case DLOAD:
										case FLOAD:
											//TODO add a check to load the shadow for this variable, see if it's non-zero, and decide which to call!
											onlyVarLoadsOrConstants = false;
											break;
										default:
											onlyVarLoadsOrConstants = false;
										}
									} else
										onlyVarLoadsOrConstants = false;
								}
							}
//							if (onlyVarLoadsOrConstants && !minsn.name.equals("<init>")) {
//								//								System.out.println("first arg is " +firstArgInsn + " " + firstArgInsn.getOpcode());
//
//								this.instructions.insertBefore(firstArgInsn, new InsnNode(TaintUtils.IGNORE_EVERYTHING));
//								//								System.out.println("Maybe opt call to " + minsn.owner + "." + minsn.name + minsn.desc);
//
//								minsn.name = minsn.name + "$$INVIVO_UNINST";
//								Type returnT = Type.getReturnType(minsn.desc);
//								if (returnT.getSort() == Type.VOID || returnT.getSort() == Type.OBJECT || (returnT.getSort() == Type.ARRAY && returnT.getElementType().getSort() == Type.OBJECT)) {
//									//No need for a taint to come on the stack after
//									this.instructions.insert(insn, new InsnNode(TaintUtils.IGNORE_EVERYTHING));
//								} else if (insn.getNext().getOpcode() == Opcodes.POP || (returnT.getSize() == 2 && insn.getNext().getOpcode() == Opcodes.POP2)) {
//									//									System.out.println("now ending after "+insn.getNext().getOpcode());
//									this.instructions.insert(insn.getNext(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
//								} else {
//									this.instructions.insert(insn, new InsnNode(TaintUtils.GENERATETAINTANDSWAP));
//									this.instructions.insert(insn, new InsnNode(TaintUtils.IGNORE_EVERYTHING));
//								}
//							}
						}
					}
					if (insn.getOpcode() < 200)
						insnN++;

					insn.accept(analyzer);
					insn = insn.getNext();
				}
				this.accept(cmv);
			}
		});
	}
}
