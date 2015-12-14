package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.LinkedList;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;

public class UninstTaintLoadCoercer extends MethodVisitor implements Opcodes{
	public UninstTaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5,access, name, desc, signature, exceptions) {
			@Override
			public void visitEnd() {
				AbstractInsnNode insn = this.instructions.getFirst();
				AnalyzerAdapter an = new AnalyzerAdapter(className, access, name, desc, null);
				LinkedList<ArrayList<Object>> stacks = new LinkedList<ArrayList<Object>>();
				while(insn != null)
				{
					if(insn instanceof MethodInsnNode)
					{
						MethodInsnNode min = (MethodInsnNode) insn;
//						System.out.println(min.name+min.desc);
						//Are we calling uninst?
						if(!Instrumenter.isIgnoredMethodFromOurAnalysis(min.owner, min.name, min.desc))
						{
							//Does it have arrays passed?
							Type[] args = Type.getArgumentTypes(min.desc);
							int nOnStack = 0;
							for(int i = args.length -1; i >=0;i--)
							{
								Type t= args[i];
								if(t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT)
								{
									//ok, now lets see if this array is trivially being loaded and we can also load its taint
									int stackDepth = an.stack.size() - nOnStack;
									AbstractInsnNode prev =insn.getPrevious();
									boolean found = false;
									int nInsnsBack = 1;
									while (prev != null) {
										if (prev.getType() == AbstractInsnNode.LABEL) {
											break;
										}
										ArrayList<Object> stack =stacks.get(stacks.size()- nInsnsBack);
										if(stack.size() == stackDepth)// && stack.get(stack.size() - 1).equals(t.getDescriptor()))
										{
											if(prev instanceof FieldInsnNode)
											{
												if (prev.getOpcode() == Opcodes.GETFIELD || prev.getOpcode() == Opcodes.GETFIELD) {
													found = true;
													this.instructions.insertBefore(prev, new InsnNode(TaintUtils.DONT_LOAD_TAINT));
												}
											}
											break;
										}
										prev = prev.getPrevious();
										nInsnsBack++;
									}
									if(found)
										this.instructions.insertBefore(insn, new IntInsnNode(TaintUtils.DONT_LOAD_TAINT, i));
								}
								nOnStack += t.getSize();
							}
						}
					}
					if (insn.getOpcode() < 200)
						insn.accept(an);
					if(an.stack == null)
						stacks.add(null);
					else
						stacks.add(new ArrayList<Object>(an.stack));
					insn = insn.getNext();
				}
				this.accept(cmv);
				super.visitEnd();
			}
		});
	}
}
