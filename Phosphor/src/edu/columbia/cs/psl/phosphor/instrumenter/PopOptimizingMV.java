package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import edu.columbia.cs.psl.phosphor.TaintUtils;

public class PopOptimizingMV extends MethodVisitor implements Opcodes {

	public PopOptimizingMV(MethodVisitor cmv, int access, String owner, String name, String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM5);
		this.mv = new PopOptimizingMN(access, owner, name, desc, signature, exceptions, cmv);
	}

	private final class PopOptimizingMN extends MethodNode {
		String owner;
		final MethodVisitor cmv;

		public PopOptimizingMN(int access, String owner, String name, String desc, String signature, String[] exceptions, MethodVisitor cmv) {
			super(Opcodes.ASM5,access, name, desc, signature, exceptions);
			this.owner = owner;
			this.cmv = cmv;
		}

		private boolean isBinaryOpcode(int opcode) {
			switch (opcode) {
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
				return true;
			}
			return false;
		}

		private boolean is1WordOpcode(AbstractInsnNode insn) {
			switch (insn.getOpcode()) {
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case ICONST_M1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case BIPUSH:
			case SIPUSH:
				return true;
			case DCONST_0:
			case DCONST_1:
				break;
			case LDC:
				if (!(((LdcInsnNode) insn).cst instanceof Double || ((LdcInsnNode) insn).cst instanceof Long))
					return true;
			}
			return false;
		}

		private boolean isStoreOp(int opcode) {
			switch (opcode) {
			case ISTORE:
			case ASTORE:
			case FSTORE:
				return true;
			case LSTORE:
			case DSTORE:

			}
			return false;
		}

		private boolean isArrayLoad(int opcode) {
			switch (opcode) {
			case IALOAD:
			case DALOAD:
			case FALOAD:
			case CALOAD:
			case LALOAD:
			case SALOAD:
				return true;
			}
			return false;
		}

		private boolean is1WordGetField(AbstractInsnNode insn) {
			//			if(insn.getOpcode() == GETFIELD && Type.getType(((FieldInsnNode)insn).desc).getSize() == 1)
			//				return true;
			return false;
		}

		private boolean is1WordLoadOp(AbstractInsnNode insn) {
			switch (insn.getOpcode()) {
			case ILOAD:
			case ALOAD:
			case FLOAD:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case ICONST_M1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case BIPUSH:
			case SIPUSH:
				return true;
			case LDC:
				if (!(((LdcInsnNode) insn).cst instanceof Double || ((LdcInsnNode) insn).cst instanceof Long))
					return true;
				break;
			case GETSTATIC:
				if (Type.getType(((FieldInsnNode) insn).desc).getSize() == 1)
					return true;
				break;
			case DLOAD:
			case LLOAD:
			case DCONST_0:
			case DCONST_1:
			}
			return false;
		}

		private int doOptPass() {
			if (TaintUtils.DEBUG_OPT)
				System.out.println("Optimizing: " + name);
			HashMap<Integer, Boolean> lvIsWrittenNotRead = new HashMap<Integer, Boolean>();
			int nChanges = 0;
			//			NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(owner, access, name, desc, null);
			AbstractInsnNode insn = this.instructions.getFirst();
			//			List[] stacksAtInsn = new List[this.instructions.size()];
			int idx = 0;
			int nPop = 0;
			while (insn != null) {
				//				stacksAtInsn[idx] = analyzer.stack;
				switch (insn.getOpcode()) {
				case Opcodes.ISTORE:
					if (!lvIsWrittenNotRead.containsKey(((VarInsnNode) insn).var)) {
						lvIsWrittenNotRead.put(((VarInsnNode) insn).var, Boolean.TRUE);
					}
					break;
				case Opcodes.ILOAD:
					lvIsWrittenNotRead.put(((VarInsnNode) insn).var, Boolean.FALSE);
					break;
				case Opcodes.IALOAD:
					if (insn.getPrevious().getOpcode() == Opcodes.POP && insn.getPrevious().getPrevious().getOpcode() == Opcodes.DUP_X2
							&& isArrayLoad(insn.getPrevious().getPrevious().getPrevious().getOpcode()) && insn.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == ILOAD
							&& is1WordGetField(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())
							&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == ALOAD) {
						AbstractInsnNode next = insn.getNext();
						AbstractInsnNode insertBefore = insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
						if (TaintUtils.DEBUG_OPT)
							System.out.println("Float up the iaload");
						this.instructions.remove(insn.getPrevious().getPrevious());
						this.instructions.remove(insn.getPrevious());
						this.instructions.remove(insn);
						this.instructions.insertBefore(insertBefore, insn);
						insn = next;
						this.instructions.insert(next, new InsnNode(SWAP));
						nChanges++;
						continue;
					}
					break;
				case Opcodes.DUP_X1:
					if (is1WordLoadOp(insn.getPrevious()) && is1WordGetField(insn.getPrevious().getPrevious()) && insn.getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
						AbstractInsnNode next = insn.getNext();
						this.instructions.insertBefore(insn.getPrevious().getPrevious().getPrevious(), insn.getPrevious().clone(null));
						this.instructions.remove(insn);
						nChanges++;
						insn = next;
						continue;
					} else if (is1WordLoadOp(insn.getPrevious()) && is1WordOpcode(insn.getPrevious().getPrevious())) {
						AbstractInsnNode next = insn.getNext();
						this.instructions.insertBefore(insn.getPrevious().getPrevious(), insn.getPrevious().clone(null));
						this.instructions.remove(insn);
						nChanges++;
						insn = next;
						continue;
					}
					break;
				case Opcodes.DUP:
					if (insn.getPrevious().getOpcode() == Opcodes.ALOAD) {
						AbstractInsnNode next = insn.getNext();
						this.instructions.insert(insn, insn.getPrevious().clone(null));
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("Dup -> ALOAD");
						nChanges++;
						insn = next;
						continue;
					}
					break;
				case TaintUtils.IS_TMP_STORE:
					AbstractInsnNode prev = insn.getPrevious();
					if (is1WordLoadOp(prev)) {
						int tmpVar = ((VarInsnNode) insn.getNext()).var;
						AbstractInsnNode next = insn.getNext().getNext();
						if (TaintUtils.DEBUG_OPT)
							System.out.println("Tmp store -> replace constant");
						this.instructions.remove(prev);
						this.instructions.remove(insn.getNext());
						this.instructions.remove(insn);
						AbstractInsnNode toReplace = next;
						boolean found = false;
						while (!found) {
							if (toReplace.getType() == AbstractInsnNode.VAR_INSN && ((VarInsnNode) toReplace).var == tmpVar) {
								this.instructions.insertBefore(toReplace, prev);
								this.instructions.remove(toReplace);
								found = true;
								break;
							}
							toReplace = toReplace.getNext();
						}
						nChanges++;
						insn = next;
						continue;
					} else if (is1WordGetField(prev) && (prev.getPrevious().getOpcode() == Opcodes.ALOAD || prev.getPrevious().getOpcode() == Opcodes.DUP)) {
						int tmpVar = ((VarInsnNode) insn.getNext()).var;
						AbstractInsnNode next = insn.getNext().getNext();
						AbstractInsnNode prevprev = prev.getPrevious();
						if (TaintUtils.DEBUG_OPT)
							System.out.println("Tmp store -> replace constant");
						this.instructions.remove(prev);
						this.instructions.remove(prevprev);
						this.instructions.remove(insn.getNext());
						this.instructions.remove(insn);
						AbstractInsnNode toReplace = next;
						boolean found = false;
						while (!found) {
							if (toReplace.getType() == AbstractInsnNode.VAR_INSN && ((VarInsnNode) toReplace).var == tmpVar) {
								this.instructions.insertBefore(toReplace, prevprev);
								this.instructions.insertBefore(toReplace, prev);
								this.instructions.remove(toReplace);
								found = true;
								break;
							}
							toReplace = toReplace.getNext();
						}
						nChanges++;
						insn = next;
						continue;
					}
					break;
				case Opcodes.SWAP:
					if (insn.getPrevious().getOpcode() == Opcodes.SWAP) {
						AbstractInsnNode tmpInsn = insn.getNext();
						this.instructions.remove(insn.getPrevious());
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("Remove double swap");
						idx++;
						nChanges++;
						insn = tmpInsn;
						continue;
					} else if (is1WordLoadOp(insn.getPrevious()) && is1WordLoadOp(insn.getPrevious().getPrevious())) {
						AbstractInsnNode orig = insn.getPrevious().getPrevious();
						this.instructions.remove(orig);
						AbstractInsnNode next = insn.getNext();
						this.instructions.insert(insn, orig);
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("swap -> reorder loads");
						insn = next;
						nChanges++;
						continue;
					} else if (is1WordGetField(insn.getPrevious()) && insn.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD
							&& insn.getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
						AbstractInsnNode val = insn.getPrevious().getPrevious().getPrevious();
						AbstractInsnNode next = insn.getNext();
						this.instructions.remove(val);
						this.instructions.insert(insn, val);
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("swap -> reorder getfield");
						insn = next;
						nChanges++;
						continue;
					} else if (is1WordGetField(insn.getPrevious()) && insn.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD && is1WordGetField(insn.getPrevious().getPrevious().getPrevious())
							&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
						prev = insn.getPrevious();
						AbstractInsnNode prev2 = prev.getPrevious();
						AbstractInsnNode prev3 = prev2.getPrevious();
						AbstractInsnNode prev4 = prev3.getPrevious();
						AbstractInsnNode next = insn.getNext();
						if (TaintUtils.DEBUG_OPT)
							System.out.println("swap -> reorder getfield2");
						this.instructions.remove(prev3);
						this.instructions.remove(prev4);
						this.instructions.insert(insn, prev3);
						this.instructions.insert(insn, prev4);
						this.instructions.remove(insn);
						insn = next;
						nChanges++;
						continue;
					} else if (isStoreOp(insn.getNext().getOpcode()) && isStoreOp(insn.getNext().getNext().getOpcode())) {
						AbstractInsnNode later = insn.getNext().getNext();
						this.instructions.remove(later);
						this.instructions.insert(insn, later);
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("swap -> reorder stores");
						insn = later;
						nChanges++;
						continue;
					}
					break;
				case Opcodes.POP:
					if (insn.getPrevious().getOpcode() == Opcodes.SWAP) {
						if (is1WordOpcode(insn.getPrevious().getPrevious()) && is1WordOpcode(insn.getPrevious().getPrevious().getPrevious())) {
							AbstractInsnNode tmp = insn.getNext();
							this.instructions.remove(insn.getPrevious().getPrevious().getPrevious());
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("pop, swap over constant");
							insn = tmp;
							nChanges++;
							continue;
						} else if (isStoreOp(insn.getPrevious().getPrevious().getOpcode()) && isStoreOp(insn.getPrevious().getPrevious().getPrevious().getOpcode())
								&& is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious())
								&& is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())) {
							if (is1WordOpcode(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())
									|| is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())) {
								AbstractInsnNode tmp = insn.getNext();
								this.instructions.remove(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
								this.instructions.remove(insn.getPrevious());
								this.instructions.remove(insn);
								if (TaintUtils.DEBUG_OPT)
									System.out.println("pop swap 2");
								insn = tmp;
								nChanges++;
								continue;
							}
						} else if (is1WordLoadOp(insn.getPrevious())) {
							//this should never have happened, weird
							AbstractInsnNode tmp = insn.getNext();
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);
							this.instructions.remove(insn);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("pop swap 3");
							insn = tmp;
							nChanges++;
							continue;
						} else if (isBinaryOpcode(insn.getPrevious().getPrevious().getOpcode()) && is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious())
								&& is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious())) {
							AbstractInsnNode tmp = insn.getNext();

							this.instructions.remove(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("pop swap 4");
							insn = tmp;
							nChanges++;
							continue;
						} else if (insn.getPrevious().getPrevious().getOpcode() == IALOAD && insn.getPrevious().getPrevious().getPrevious().getOpcode() == ILOAD
								&& is1WordGetField(insn.getPrevious().getPrevious().getPrevious().getPrevious())
								&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == ALOAD) {
							AbstractInsnNode next = insn.getNext();
							this.instructions.insertBefore(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious(), new InsnNode(POP));
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("pop swap 5");
							insn = next;
							nChanges++;
							continue;
						} else if (insn.getPrevious().getPrevious().getOpcode() == Opcodes.GETFIELD && insn.getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.SWAP
								&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.GETFIELD) {
							AbstractInsnNode next = insn.getNext();
							AbstractInsnNode insertBefore = insn.getPrevious().getPrevious().getPrevious().getPrevious();
							this.instructions.insertBefore(insertBefore, new InsnNode(Opcodes.POP));
							this.instructions.remove(insn.getPrevious().getPrevious().getPrevious().getPrevious());
							this.instructions.remove(insn.getPrevious().getPrevious().getPrevious());
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);
							insn = next;
							nChanges++;
							continue;
						}
					} else if (insn.getPrevious().getOpcode() == Opcodes.DUP_X2) {
						AbstractInsnNode beforeDupx2 = insn.getPrevious().getPrevious();
						if (isBinaryOpcode(beforeDupx2.getOpcode()) && is1WordGetField(beforeDupx2.getPrevious()) && beforeDupx2.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD
								&& is1WordGetField(beforeDupx2.getPrevious().getPrevious().getPrevious())
								&& beforeDupx2.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
							AbstractInsnNode wayAbove = beforeDupx2.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
							if (is1WordGetField(wayAbove) && wayAbove.getPrevious().getOpcode() == Opcodes.ALOAD && is1WordGetField(wayAbove.getPrevious().getPrevious())
									&& wayAbove.getPrevious().getPrevious().getPrevious().getOpcode() == ALOAD) {
								AbstractInsnNode wayAbove2 = wayAbove.getPrevious();
								AbstractInsnNode wayAbove3 = wayAbove2.getPrevious();
								AbstractInsnNode wayAbove4 = wayAbove3.getPrevious();
								this.instructions.remove(wayAbove);
								this.instructions.remove(wayAbove2);
								this.instructions.remove(wayAbove3);
								this.instructions.remove(wayAbove4);
								AbstractInsnNode next = insn.getNext();
								this.instructions.remove(insn.getPrevious());
								this.instructions.insert(insn, wayAbove);
								this.instructions.insert(insn, wayAbove2);
								this.instructions.insert(insn, wayAbove3);
								this.instructions.insert(insn, wayAbove4);
								this.instructions.remove(insn);
								if (TaintUtils.DEBUG_OPT)
									System.out.println("pop swap 6");
								insn = next;
								nChanges++;
								continue;
							}
						} else if (isBinaryOpcode(beforeDupx2.getOpcode()) && is1WordLoadOp(beforeDupx2.getPrevious()) && is1WordLoadOp(beforeDupx2.getPrevious().getPrevious())
								&& is1WordLoadOp(beforeDupx2.getPrevious().getPrevious().getPrevious()) && is1WordLoadOp(beforeDupx2.getPrevious().getPrevious().getPrevious().getPrevious())) {
							AbstractInsnNode up = beforeDupx2.getPrevious().getPrevious().getPrevious();
							AbstractInsnNode up2 = beforeDupx2.getPrevious().getPrevious().getPrevious().getPrevious();
							AbstractInsnNode next = insn.getNext();
							this.instructions.remove(up);
							this.instructions.remove(up2);
							this.instructions.remove(insn.getPrevious());
							this.instructions.insert(insn, up);
							this.instructions.insert(insn, up2);
							this.instructions.remove(insn);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("pop swap 7");
							insn = next;
							nChanges++;
							continue;
						}
					} else if (isStoreOp(insn.getPrevious().getOpcode()) && is1WordLoadOp(insn.getPrevious().getPrevious())) {
						if (TaintUtils.DEBUG_OPT)
							System.out.println("float up pop");
						AbstractInsnNode next = insn.getNext();
						AbstractInsnNode insertBefore = insn.getPrevious().getPrevious();
						this.instructions.remove(insn);
						this.instructions.insertBefore(insertBefore, insn);
						insn = next;
						nChanges++;
						continue;
					} else if (is1WordLoadOp(insn.getPrevious())) {
						AbstractInsnNode next = insn.getNext();
						if (TaintUtils.DEBUG_OPT)
							System.out.println("pop over load");
						this.instructions.remove(insn.getPrevious());
						this.instructions.remove(insn);
						insn = next;
						nChanges++;
						continue;
					} else if (is1WordGetField(insn.getPrevious()) && insn.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
						AbstractInsnNode next = insn.getNext();
						this.instructions.remove(insn.getPrevious().getPrevious());
						this.instructions.remove(insn.getPrevious());
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("pop over getfield");
						insn = next;
						nChanges++;
						continue;
					} else if (insn.getPrevious().getOpcode() == Opcodes.IALOAD) {
						AbstractInsnNode next = insn.getNext();
						this.instructions.insert(insn, new InsnNode(Opcodes.POP2));
						this.instructions.remove(insn.getPrevious());
						this.instructions.remove(insn);
						if (TaintUtils.DEBUG_OPT)
							System.out.println("pop over iaload");
						insn = next;
						nChanges++;
						continue;
					}
					nPop++;
					break;
				case Opcodes.POP2:
					if (insn.getPrevious().getOpcode() == Opcodes.DUP2_X1) {
						//if it's obvious what's 3 under, then we can ignore this and just move that insn up here
						if (is1WordLoadOp(insn.getPrevious().getPrevious()) && is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious())
								&& is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious())) {
							AbstractInsnNode next = insn.getNext();
							AbstractInsnNode val = insn.getPrevious().getPrevious().getPrevious().getPrevious();
							if (TaintUtils.DEBUG_OPT)
								System.out.println("POP2 over DUP2_X1, prevs are " + insn.getPrevious().getPrevious() + ", " + insn.getPrevious().getPrevious().getPrevious() + ","
										+ insn.getPrevious().getPrevious().getPrevious().getPrevious());
							this.instructions.remove(val);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("removing " + insn.getPrevious());
							this.instructions.remove(insn.getPrevious());
							if (TaintUtils.DEBUG_OPT)
								System.out.println("adding " + val);
							this.instructions.insert(insn, val);
							if (TaintUtils.DEBUG_OPT)
								System.out.println("removing " + insn);

							this.instructions.remove(insn);

							insn = next;
							nChanges++;
							continue;
						}
					} else if (insn.getPrevious().getOpcode() == Opcodes.DUP2_X2) {
						if (insn.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD && insn.getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD
								&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ILOAD
								&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.GETFIELD
								&& insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD) {
							AbstractInsnNode next = insn.getNext();

							AbstractInsnNode i1 = insn.getPrevious().getPrevious();
							AbstractInsnNode i2 = insn.getPrevious().getPrevious().getPrevious();
							AbstractInsnNode insertBefore = insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
//							System.out.println("insert before " + insertBefore);
//							System.out.println(i2);
//							System.out.println(i1);
//							System.out.println("removing: " + insn);
//							System.out.println(insn.getPrevious());
							this.instructions.remove(i1);
							this.instructions.remove(i2);
							this.instructions.insertBefore(insertBefore, i2);
							this.instructions.insertBefore(insertBefore, i1);
							this.instructions.remove(insn.getPrevious());
							this.instructions.remove(insn);

							insn = next;
							nChanges++;
							continue;
						}
						else if(is1WordLoadOp(insn.getPrevious().getPrevious()) &&
								is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious()) &&
								is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious()) &&
								is1WordLoadOp(insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious()))
						{
								/* ILOAD ILOAD ALOAD ALOAD DUP2_X2 POP2*/
							AbstractInsnNode next = insn.getNext();
							AbstractInsnNode i1 = insn.getPrevious().getPrevious();
							AbstractInsnNode i2 = insn.getPrevious().getPrevious().getPrevious();
							AbstractInsnNode i3 = insn.getPrevious().getPrevious().getPrevious().getPrevious();
							AbstractInsnNode i4 = insn.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
							
							this.instructions.remove(i3);
							this.instructions.remove(i4);
							
							this.instructions.insert(i1, i3);
							this.instructions.insert(i1, i4);
							this.instructions.remove(insn.getPrevious()); //dup2x2
							this.instructions.remove(insn);//pop2
							insn = next;
							nChanges++;
							continue;
						}
					}
					break;
				}
				//				insn.accept(analyzer);
				insn = insn.getNext();
				idx++;
			}
			if (TaintUtils.DEBUG_OPT)
				System.out.println(owner + "." + name + "NPOP: " + nPop);
			HashSet<Integer> lvsToObliterate = new HashSet<Integer>();
			for (Entry<Integer, Boolean> i : lvIsWrittenNotRead.entrySet())
				if (i.getValue()) {
					if (TaintUtils.DEBUG_OPT)
						System.out.println("BLow away: " + i.getKey());
					lvsToObliterate.add(i.getKey());
				}
			if (!lvsToObliterate.isEmpty()) {
				insn = this.instructions.getFirst();
				while (insn != null) {
					if (insn.getType() == AbstractInsnNode.VAR_INSN) {
						if (lvsToObliterate.contains(((VarInsnNode) insn).var) && ((VarInsnNode) insn).getOpcode() == ISTORE) {
							AbstractInsnNode next = insn.getNext();
							if (TaintUtils.DEBUG_OPT)
								System.out.println("remove " + insn);
							this.instructions.insert(insn, new InsnNode(Opcodes.POP));
							this.instructions.remove(insn);
							nChanges++;
							if (TaintUtils.DEBUG_OPT)
								System.out.println("removed a store");
							insn = next;
							continue;
						}
					} else if (insn.getType() == AbstractInsnNode.IINC_INSN) {
						if (lvsToObliterate.contains(((IincInsnNode) insn).var)) {
							AbstractInsnNode next = insn.getNext();
							if (TaintUtils.DEBUG_OPT)
								System.out.println("remove " + insn);
							this.instructions.remove(insn);
							nChanges++;
							if (TaintUtils.DEBUG_OPT)
								System.out.println("removed a store");
							insn = next;
							continue;
						}
					}
					insn = insn.getNext();
				}
			}
			return nChanges;
		}

		@Override
		public void visitEnd() {

			int nChanges = doOptPass();
			if (TaintUtils.DEBUG_OPT)
				System.out.println("Optimizations: " + nChanges);
			while (nChanges > 0) {

				nChanges = doOptPass();
				if (TaintUtils.DEBUG_OPT)
					System.out.println("Optimizations: " + nChanges);
			}
			this.accept(cmv);
			super.visitEnd();
		}
	}
}
