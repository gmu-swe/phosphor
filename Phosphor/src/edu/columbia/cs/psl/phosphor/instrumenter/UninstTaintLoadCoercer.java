package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.InstMethodSinkInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.PFrame;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.SinkableArrayValue;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;

public class UninstTaintLoadCoercer extends MethodVisitor implements Opcodes {

	public UninstTaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			@Override
			public void visitEnd() {
				//				final AbstractInsnNode[] insns = new AbstractInsnNode[this.instructions.size()];
				//				{
				//					int i = 0;
				//					AbstractInsnNode insn = this.instructions.getFirst();
				//					while(insn != null)
				//					{
				//						insns[i] = insn;
				//						insn = insn.getNext();
				//						i++;
				//					}
				//				}
				LinkedList<SinkableArrayValue> relevantValues = new LinkedList<SinkableArrayValue>();
				Type[] args = Type.getArgumentTypes(desc);
				final int nargs = args.length + ((Opcodes.ACC_STATIC & access) == 0 ? 1 : 0);
				//				final HashMap<Integer, HashSet<Integer>> insnToPredecessor = new HashMap<Integer, HashSet<Integer>>();
				Analyzer a = new Analyzer(new InstMethodSinkInterpreter(relevantValues)) {
					//					@Override
					//					protected void newControlFlowEdge(int insn, int successor) {
					//						if(!insnToPredecessor.containsKey(successor))
					//							insnToPredecessor.put(successor, new HashSet<Integer>());
					//						insnToPredecessor.get(successor).add(insn);
					//					}
					@Override
					protected Frame newFrame(int nLocals, int nStack) {
						return new PFrame(nLocals, nStack, nargs);
					}

					@Override
					protected Frame newFrame(Frame src) {
						return new PFrame(src);
					}
				};
				try {
					System.out.println("UTLC " + name + desc);
					Frame[] frames = a.analyze(className, this);
					AbstractInsnNode insn = this.instructions.getFirst();
					for (int i = 0; i < frames.length; i++) {
						PFrame f = (PFrame) frames[i];
						switch (insn.getType()) {
						case AbstractInsnNode.INSN:
							switch(insn.getOpcode())
							{
							case Opcodes.AASTORE:
								BasicValue value3 = (BasicValue) f.getStack(f.getStackSize() -1);
								System.out.println("AASTORE " + value3);
								if (value3 instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(value3.getType())) {
									relevantValues.addAll(((SinkableArrayValue) value3).tag());
								}
								break;
							case Opcodes.AALOAD:
								break;
							}
						case AbstractInsnNode.VAR_INSN:
							break;
						case AbstractInsnNode.FIELD_INSN:
							switch(insn.getOpcode())
							{
							case Opcodes.PUTSTATIC:
							case Opcodes.PUTFIELD:
								BasicValue value = (BasicValue) f.getStack(f.getStackSize() -1);
								Type vt = Type.getType(((FieldInsnNode)insn).desc);
								if (value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall && 
										(TaintUtils.isPrimitiveArrayType(vt) || TaintUtils.isPrimitiveArrayType(value.getType()))) {
									relevantValues.addAll(((SinkableArrayValue) value).tag());
								}
								break;
								
							}
							break;
						case AbstractInsnNode.METHOD_INSN:
							MethodInsnNode min = (MethodInsnNode) insn;
							boolean uninstCall = Instrumenter.isIgnoredMethodFromOurAnalysis(min.owner, min.name, min.desc);
							Type[] margs = Type.getArgumentTypes(((MethodInsnNode) insn).desc);
//							if(!uninstCall)
							{
								System.out.println("call " + min.name +min.desc);
								for(int k = 0; k < f.getStackSize();k++)
									System.out.print(f.getStack(k));
								System.out.println();
								for(int k = 0; k < f.getLocals(); k++)
									System.out.print(""+k+f.getLocal(k)+ " ");
								System.out.println();
							}
							for (int j = 0; j < margs.length; j++) {
								Object o=f.getStack(f.getStackSize() - (margs.length-j));
								System.out.println(o);
								if (o instanceof SinkableArrayValue) {
									SinkableArrayValue v = (SinkableArrayValue) o;
									Type t = margs[j];
									//if we make an inst call and are passing a prim array type, then we need the tags
									if (!uninstCall && TaintUtils.isPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
										relevantValues.addAll(v.tag());
										System.out.println("relevant!");
									}
									//regardless of the kind of call, if we upcast then we need it
									else if (t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(v.getType()) && !v.flowsToInstMethodCall &&!min.owner.equals("java/lang/System") && !min.name.equals("arrayCopy")) {
										System.out.println("Releveant!");
										relevantValues.addAll(v.tag());
									}
								}
							}
			
							break;
						case AbstractInsnNode.TYPE_INSN:
							switch(insn.getOpcode())
							{
							case Opcodes.CHECKCAST:
								BasicValue value = (BasicValue) f.getStack(f.getStackSize() -1);
								if (value instanceof SinkableArrayValue && ((TypeInsnNode) insn).desc.equals("java/lang/Object") && TaintUtils.isPrimitiveArrayType(value.getType())
										&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
									System.out.println("Checkcast a prim array");
									relevantValues.addAll(((SinkableArrayValue) value).tag());
								}
								break;
							}
							break;
						}
						insn = insn.getNext();
					}
					insn = this.instructions.getFirst();
					for(int i = 0; i < frames.length; i++)
					{
						if(insn.getType() == AbstractInsnNode.FRAME)
						{
							FrameNode fn = (FrameNode) insn;
							int cnt = 0;
							for (int j = 0; j < fn.local.size(); j++) {
								Object l = fn.local.get(j);
								BasicValue v = (BasicValue) frames[i].getLocal(cnt);
								if (v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall) {
									fn.local.set(j, new TaggedValue(l));
								}
								cnt += v.getSize();
							}
						}
						insn=insn.getNext();
					}
					HashSet<AbstractInsnNode> added = new HashSet<AbstractInsnNode>();
					for (SinkableArrayValue v : relevantValues) {
						if (!added.add(v.src))
							continue;
						System.out.println(v.getType());
						if (v.src instanceof VarInsnNode)
							System.out.println(Printer.OPCODES[v.src.getOpcode()] + ((VarInsnNode) v.src).var);
						else
							System.out.println(v.src);
						if (this.instructions.contains(v.src)) {
							System.out.println("added");
							this.instructions.insertBefore(v.src, new InsnNode(TaintUtils.NEXT_INSN_TAINT_AWARE));
						}
					}
				} catch (AnalyzerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				super.visitEnd();

				this.accept(cmv);
			}
		});
		System.out.println("\t\t\t" + className);
	}
}
