package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;

public class ImplicitUnnecessaryTaintLoadRemover extends MethodVisitor implements Opcodes {
	public ImplicitUnnecessaryTaintLoadRemover(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5,access, name, desc, signature, exceptions) {
			boolean isConstantLoadOpcode(AbstractInsnNode insn)
			{
				switch(insn.getOpcode())
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
					return true;
				case LDC:
					return ((LdcInsnNode)insn).cst instanceof Number;
				}
				return false;
			}
			boolean isStackManipulationOpcode(int opcode)
			{
				switch(opcode)
				{
				case DUP:
				case POP:
				case DUP_X1:
				case DUP_X2:
				case DUP2:
				case DUP2_X1:
				case DUP2_X2:
				case SWAP:
				case POP2:
					return true;
				}
				return false;
			}
			@Override
			public void visitEnd() {
				super.visitEnd();
				NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc, null);
				List[] stacks = new List[this.instructions.size() + 1];
				List[] cstStacks = new List[this.instructions.size() + 1];

				int insnN = 0;
				AbstractInsnNode insn = this.instructions.getFirst();
				boolean inIgnoreEverything = false;
				while (insn != null) {
					if (analyzer.stack != null)
					{
						stacks[insnN] = new ArrayList(analyzer.stack);
						cstStacks[insnN] = new ArrayList(analyzer.stackConstantVals);
					}
					switch(insn.getType())
					{
					case AbstractInsnNode.INSN:
					switch(insn.getOpcode())
					{
					case TaintUtils.IGNORE_EVERYTHING:
						inIgnoreEverything = !inIgnoreEverything;
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
					case Opcodes.LSUB:
					case Opcodes.LMUL:
					case Opcodes.LADD:
					case Opcodes.LDIV:
					case Opcodes.LREM:
					case Opcodes.LAND:
					case Opcodes.LOR:
					case Opcodes.LXOR:			
					case Opcodes.LCMP:
					case Opcodes.DCMPL:
					case Opcodes.DCMPG:
					case Opcodes.FCMPL:
					case Opcodes.FCMPG:
					case Opcodes.FADD:
					case Opcodes.FREM:
					case Opcodes.FSUB:
					case Opcodes.FMUL:
					case Opcodes.FDIV:
						if(inIgnoreEverything)
							break;
						List cstStack = cstStacks[insnN];
						if(cstStack.get(cstStack.size() - 1) != null
								|| (cstStack.size() > 2 && 
										(cstStack.get(cstStack.size() - 2)  instanceof Long 
												|| cstStack.get(cstStack.size() - 2)  instanceof Double)))
						{
//							System.out.println("From " + insn);
							//OK see if we can easily find the constant load
							AbstractInsnNode cstLoad = insn.getPrevious();
							while(cstLoad != null && !isConstantLoadOpcode(cstLoad) && !isStackManipulationOpcode(cstLoad.getOpcode()))
							{
//								System.out.println(cstLoad);
								cstLoad = cstLoad.getPrevious();
							}
							if(cstLoad != null && isConstantLoadOpcode(cstLoad))
							{
//								System.out.println(cstLoad);
//								System.out.println("Got an " + Printer.OPCODES[insn.getOpcode()] + " at " + stacks[insnN] + cstStacks[insnN]);
								this.instructions.insertBefore(cstLoad, new InsnNode(TaintUtils.DONT_LOAD_TAINT));
								this.instructions.insert(cstLoad, new InsnNode(TaintUtils.DONT_LOAD_TAINT));

									if (!inIgnoreEverything) {
										inIgnoreEverything = true;
										this.instructions.insertBefore(insn, new InsnNode(TaintUtils.IGNORE_EVERYTHING));
										this.instructions.insert(insn, new InsnNode(TaintUtils.IGNORE_EVERYTHING));
									}
							}
						}
						break;
					}
						break;
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
