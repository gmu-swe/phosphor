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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
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
	PrimitiveArrayAnalyzer primitiveArrayFixer;

	public void setPrimitiveArrayFixer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.primitiveArrayFixer = primitiveArrayFixer;
	}

	public UninstTaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5);
		this.mv = new UninstTaintLoadCoercerMN(className, access, name, desc, signature, exceptions, cmv);
	}

	class UninstTaintLoadCoercerMN extends MethodNode {
		String className;
		MethodVisitor cmv;
		public UninstTaintLoadCoercerMN(String className, int access, String name, String desc, String signature, String[] exceptions, MethodVisitor cmv) {
			super(Opcodes.ASM5, access, name, desc, signature, exceptions);
			this.className = className;
			this.cmv = cmv;
		}

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
//					System.out.println("Req new frame " + nLocals +" nargs " + nargs);
					return new PFrame(nLocals, nStack, nargs);
				}

				@Override
				protected Frame newFrame(Frame src) {
					return new PFrame(src);
				}
			};
			try {
				//					System.out.println("UTLC " + name + desc);
				Frame[] frames = a.analyze(className, this);
				AbstractInsnNode insn = this.instructions.getFirst();
				for (int i = 0; i < frames.length; i++) {
					PFrame f = (PFrame) frames[i];
					if (f == null) {
						insn = insn.getNext();
						continue;
					}
					switch (insn.getType()) {
					//						case AbstractInsnNode.LINE:
					//							System.out.println("Line " + ((LineNumberNode)insn).line);
					//							break;
					case AbstractInsnNode.INSN:
						switch (insn.getOpcode()) {
						case Opcodes.AASTORE:
							BasicValue value3 = (BasicValue) f.getStack(f.getStackSize() - 1);
							//								System.out.println(this.name+this.desc+"AASTORE " + value3);
							if (value3 instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(value3.getType())) {
								relevantValues.addAll(((SinkableArrayValue) value3).tag());
							}
							break;
						case Opcodes.ARETURN:
							//								System.out.println("ARETURN " + desc + f.getStack(f.getStackSize()-1));
							if (Type.getReturnType(desc).getDescriptor().equals("Ljava/lang/Object;")) {
								BasicValue v = (BasicValue) f.getStack(f.getStackSize() - 1);
								if (v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()))
									relevantValues.addAll(((SinkableArrayValue) v).tag());
							}
						case Opcodes.AALOAD:
							break;
						}
					case AbstractInsnNode.VAR_INSN:
						if (insn.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP || insn.getOpcode() == TaintUtils.ALWAYS_AUTOBOX) {
							VarInsnNode vinsn = ((VarInsnNode) insn);
							BasicValue v = (BasicValue) f.getLocal(vinsn.var);
							if (v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()))
								relevantValues.addAll(((SinkableArrayValue) v).tag());
						}
						break;
					case AbstractInsnNode.FIELD_INSN:
						switch (insn.getOpcode()) {
						case Opcodes.PUTSTATIC:
						case Opcodes.PUTFIELD:
							BasicValue value = (BasicValue) f.getStack(f.getStackSize() - 1);
							Type vt = Type.getType(((FieldInsnNode) insn).desc);
							if (value instanceof SinkableArrayValue
									&& !((SinkableArrayValue) value).flowsToInstMethodCall
									&& ((!TaintUtils.LAZY_TAINT_ARRAY_INIT && (TaintUtils.isPrimitiveArrayType(vt) || TaintUtils.isPrimitiveArrayType(value.getType())) || (TaintUtils.LAZY_TAINT_ARRAY_INIT
											&& TaintUtils.isPrimitiveArrayType(value.getType()) && !TaintUtils.isPrimitiveArrayType(vt))))) {
								relevantValues.addAll(((SinkableArrayValue) value).tag());
							}
							break;
						}
						break;
					case AbstractInsnNode.METHOD_INSN:
						MethodInsnNode min = (MethodInsnNode) insn;
						boolean uninstCall = Instrumenter.isIgnoredClass(min.owner);
//						boolean uninstCall = Instrumenter.isIgnoredMethodFromOurAnalysis(min.owner, min.name, min.desc, true);
						Type[] margs = Type.getArgumentTypes(((MethodInsnNode) insn).desc);

						//							if(!uninstCall)
						//							{
//														System.out.println("call " + min.name +min.desc);
//														for(int k = 0; k < f.getStackSize();k++)
//															System.out.print(f.getStack(k));
//														System.out.println();
//														for(int k = 0; k < f.getLocals(); k++)
//															System.out.print(""+k+f.getLocal(k)+ " ");
//														System.out.println();
						//							}
						if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
							Object o = f.getStack(f.getStackSize() - margs.length - 1);
							if (o instanceof SinkableArrayValue) {
								break; //being called on a primitive array type
							}
						}
						for (int j = 0; j < margs.length; j++) {
							Object o = f.getStack(f.getStackSize() - (margs.length - j));
							//								System.out.println(o);
							if (o instanceof SinkableArrayValue) {
								SinkableArrayValue v = (SinkableArrayValue) o;
								Type t = margs[j];
								//if we make an inst call and are passing a prim array type, then we need the tags
								if (!uninstCall && TaintUtils.isPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
									relevantValues.addAll(v.tag());
									//										System.out.println("relevant!");
								}
								//regardless of the kind of call, if we upcast then we need it
								else if (t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(v.getType()) && !v.flowsToInstMethodCall
										&& !min.owner.equals("java/lang/System") && !min.name.equals("arrayCopy")) {
									//										System.out.println("Releveant!");
									relevantValues.addAll(v.tag());
								}
							}
						}

						break;
					case AbstractInsnNode.TYPE_INSN:
						switch (insn.getOpcode()) {
						case Opcodes.CHECKCAST:
							BasicValue value = (BasicValue) f.getStack(f.getStackSize() - 1);
							if (value instanceof SinkableArrayValue && ((TypeInsnNode) insn).desc.equals("java/lang/Object") && TaintUtils.isPrimitiveArrayType(value.getType())
									&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
								//									System.out.println("Checkcast a prim array");
								relevantValues.addAll(((SinkableArrayValue) value).tag());
							}
							break;
						}
						break;
					}
					insn = insn.getNext();
				}
				LinkedList<SinkableArrayValue> more = new LinkedList<SinkableArrayValue>();
				for (SinkableArrayValue v : relevantValues) {
					if (v.isStickyDup)
						more.addAll(v.resolveDupIssues());
				}
				relevantValues.addAll(more);
				insn = this.instructions.getFirst();
				for (int i = 0; i < frames.length; i++) {
					//						System.out.print(i + " " );
					//						String fr = "";
					//						if(frames[i] != null)
					//						for(int j = 0; j < frames[i].getStackSize(); j++)
					//							fr += (frames[i].getStack(j))+ " ";
					//						String fr2 = "";
					//						if(frames[i] != null)
					//						for(int j = 0; j < frames[i].getLocals(); j++)
					//							fr2 += (frames[i].getLocal(j))+ " ";
					////						System.out.println(fr);
					//						this.instructions.insertBefore(insn, new LdcInsnNode(fr));
					//						this.instructions.insertBefore(insn, new LdcInsnNode(fr2));
					if (insn.getType() == AbstractInsnNode.FRAME && frames[i] != null) {
						FrameNode fn = (FrameNode) insn;
						//							System.out.println("Found frame " + i +", stack size" + fn.stack.size() + ", fn stack " + frames[i].getStackSize() + ", local size " + fn.local.size() + ", fn " + frames[i].getLocals());
						int analyzerFrameIdx = 0;
						for (int j = 0; j < fn.local.size(); j++) {
							Object l = fn.local.get(j);
							BasicValue v = (BasicValue) frames[i].getLocal(analyzerFrameIdx);

							if (v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall && TaintAdapter.isPrimitiveStackType(l)) {
								//									System.out.println(l + " vs  " + v);
								fn.local.set(j, new TaggedValue(l));
							}
							analyzerFrameIdx++;
							if (l == Opcodes.DOUBLE || l == Opcodes.LONG)
								analyzerFrameIdx++;
						}
						for (int j = 0; j < fn.stack.size(); j++) {
							Object l = fn.stack.get(j);
							BasicValue v = (BasicValue) frames[i].getStack(j);
							//								System.out.println(j + "ZZZ "+ l + " vs " + v);
							if (v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall && TaintAdapter.isPrimitiveStackType(l)) {
								fn.stack.set(j, new TaggedValue(l));
							}
						}
					}
					insn = insn.getNext();
				}
				HashSet<AbstractInsnNode> added = new HashSet<AbstractInsnNode>();
				for (SinkableArrayValue v : relevantValues) {
					if (!added.add(v.src))
						continue;
					//						System.out.println(v.getType());
					//						System.out.println(v.src);
					//						if (v.src instanceof VarInsnNode)
					//							System.out.println(Printer.OPCODES[v.src.getOpcode()] + ((VarInsnNode) v.src).var);
					//						else if(v.src instanceof InsnNode)
					//							System.out.println(Printer.OPCODES[v.src.getOpcode()] + ", " + v.flowsToInstMethodCall+","+(v.srcVal != null ? v.srcVal.flowsToInstMethodCall : "."));
					if (this.instructions.contains(v.src)) {
						//							System.out.println("added");

						if (v.isStickyDup) {
							this.instructions.insert(v.src, new InsnNode(Opcodes.DUP_X1));
							this.instructions.insert(v.src, new InsnNode(TaintUtils.DONT_LOAD_TAINT));

							this.instructions.remove(v.src);
						} else {
							this.instructions.insertBefore(v.src, new InsnNode(TaintUtils.NEXT_INSN_TAINT_AWARE));
							if (v.src instanceof MethodInsnNode) {
								MethodInsnNode min = (MethodInsnNode) v.src;
								if(primitiveArrayFixer != null)
								primitiveArrayFixer.wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType(Type.getReturnType(min.desc)));
							}
							System.out.println("UTLC added before " + v.src);
						}
					}
				}

			} catch (AnalyzerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalStateException(e);
			}
			super.visitEnd();

			this.accept(cmv);
		}
	}
}
