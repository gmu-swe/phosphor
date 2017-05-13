package edu.columbia.cs.psl.phosphor.instrumenter;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;
import org.objectweb.asm.util.TraceClassVisitor;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.InstMethodSinkInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.PFrame;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.SinkableArrayValue;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;

public class TaintLoadCoercer extends MethodVisitor implements Opcodes {
	PrimitiveArrayAnalyzer primitiveArrayFixer;

	public void setPrimitiveArrayFixer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.primitiveArrayFixer = primitiveArrayFixer;
	}
	private boolean ignoreExistingFrames;
	public TaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv, boolean ignoreExistingFrames) {
		super(Opcodes.ASM5);
		this.mv = new UninstTaintLoadCoercerMN(className, access, name, desc, signature, exceptions, cmv);
		this.ignoreExistingFrames = ignoreExistingFrames;
	}

	class UninstTaintLoadCoercerMN extends MethodNode {
		String className;
		MethodVisitor cmv;
		public UninstTaintLoadCoercerMN(String className, int access, String name, String desc, String signature, String[] exceptions, MethodVisitor cmv) {
			super(Opcodes.ASM5, access, name, desc, signature, exceptions);
			this.className = className;
			this.cmv = cmv;
//			System.out.println(name+desc);
		}
		@Override
		protected LabelNode getLabelNode(Label l) {
			if(!Configuration.READ_AND_SAVE_BCI)
				return super.getLabelNode(l);
			if (!(l.info instanceof LabelNode)) {
				l.info = new LabelNode(l);
			}
			return (LabelNode) l.info;
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
			HashMap<AbstractInsnNode, Value> liveValues = new HashMap<AbstractInsnNode, Value>();
			LinkedList<SinkableArrayValue> relevantValues = new LinkedList<SinkableArrayValue>();
			Type[] args = Type.getArgumentTypes(desc);
			final int nargs = args.length + ((Opcodes.ACC_STATIC & access) == 0 ? 1 : 0);
			//				final HashMap<Integer, HashSet<Integer>> insnToPredecessor = new HashMap<Integer, HashSet<Integer>>();
			Analyzer a = new Analyzer(new InstMethodSinkInterpreter(relevantValues, liveValues)) {
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
				String graph ="";
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
											case AbstractInsnNode.LINE:
//												System.out.println("Line " + ((LineNumberNode)insn).line);
												break;
					case AbstractInsnNode.INSN:
						switch (insn.getOpcode()) {
						case Opcodes.AASTORE:
						case Opcodes.BASTORE:
						case Opcodes.CASTORE:
						case Opcodes.IASTORE:
						case Opcodes.FASTORE:
						case Opcodes.LASTORE:
						case Opcodes.DASTORE:
						case Opcodes.SASTORE:
							BasicValue value3 = (BasicValue) f.getStack(f.getStackSize() - 1);

							BasicValue value1 = (BasicValue) f.getStack(f.getStackSize() - 3);

							BasicValue value2 = (BasicValue) f.getStack(f.getStackSize() - 2);

							if (Configuration.ARRAY_INDEX_TRACKING) {
								if (!(value2 instanceof SinkableArrayValue && ((SinkableArrayValue) value2).isConstant)) {
									if(value1 instanceof SinkableArrayValue && insn.getOpcode() != Opcodes.AASTORE)
									{
										relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
									}
									relevantValues.addAll(((SinkableArrayValue) value2).tag(insn));
								}
//								System.out.println(value1 + ", " + value2+", "+value3);
							}
//							if(Configuration.ARRAY_LENGTH_TRACKING)
//								relevantValues.addAll(((SinkableArrayValue)f.getStack(f.getStackSize() - 2)).tag(insn));
							if(value1 instanceof SinkableArrayValue && value3 instanceof SinkableArrayValue)
							{

								if (((SinkableArrayValue) value1).isNewArray && ((SinkableArrayValue) value3).isConstant)// && (Configuration.ARRAY_INDEX_TRACKING || ((SinkableArrayValue) value2).isConstant))
								{
									//Storing constant to new array - no need to tag, ever.
									break;
								}
							}
							if (value3 instanceof SinkableArrayValue && TaintUtils.isPrimitiveOrPrimitiveArrayType(value3.getType())) {
									relevantValues.addAll(((SinkableArrayValue) value3).tag(insn));
							}
							if (value1 instanceof SinkableArrayValue && TaintUtils.isPrimitiveOrPrimitiveArrayType(value1.getType())) {

								relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
							}
							
							break;
						case Opcodes.ARETURN:
//															System.out.println("ARETURN " + name+desc + " - " + f.getStack(f.getStackSize()-1));
//							if (Type.getReturnType(desc).getDescriptor().equals("Ljava/lang/Object;") || TaintUtils.isPrimitiveArrayType(Type.getReturnType(desc))) {
								BasicValue v = (BasicValue) f.getStack(f.getStackSize() - 1);
								if (v instanceof SinkableArrayValue && (TaintUtils.isPrimitiveArrayType(v.getType()) || (v.getType() == null && TaintUtils.isPrimitiveArrayType(Type.getReturnType(desc)))))
								{
									relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
								}
//							}
							break;
						case Opcodes.IALOAD:
						case Opcodes.SALOAD:
						case Opcodes.CALOAD:
						case Opcodes.BALOAD:
						case Opcodes.LALOAD:
						case Opcodes.DALOAD:
						case Opcodes.FALOAD:
//							value3 = (BasicValue) f.getStack(f.getStackSize() - 1);
//							value1 = (BasicValue) f.getStack(f.getStackSize() - 2);
//							System.out.println(Printer.OPCODES[insn.getOpcode()] + value3+value1);
							if(Configuration.ARRAY_INDEX_TRACKING)
							{
								value1 = (BasicValue) f.getStack(f.getStackSize()-1);
								if(value1 instanceof SinkableArrayValue && !(
										((SinkableArrayValue)value1).isConstant
										|| (
												((SinkableArrayValue)value1).copyOf != null) && ((SinkableArrayValue)value1).copyOf.isConstant)
											)
									relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
							}
							break;
						case Opcodes.AALOAD:
							if(Configuration.ARRAY_INDEX_TRACKING)
							{
								value1 = (BasicValue) f.getStack(f.getStackSize()-1);
								if(value1 instanceof SinkableArrayValue && !(
										((SinkableArrayValue)value1).isConstant
										|| (
												((SinkableArrayValue)value1).copyOf != null) && ((SinkableArrayValue)value1).copyOf.isConstant)
											)
								{
									SinkableArrayValue sv = (SinkableArrayValue) value1;
									
									relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
								}
							}
							break;
						case IRETURN:
						case FRETURN:
						case LRETURN:
						case DRETURN:
							v = (BasicValue) f.getStack(f.getStackSize() - 1);
							relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
							break;
						}
//					case AbstractInsnNode.INT_INSN:
//						switch (insn.getOpcode()) {
//						case NEWARRAY:
//							if (Configuration.ARRAY_LENGTH_TRACKING)
//							{
//								relevantValues.addAll(((SinkableArrayValue) f.getStack(f.getStackSize() - 1)).tag(insn));
//							}
//							break;
//						}
//						break;
					case AbstractInsnNode.VAR_INSN:
						if (insn.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP || insn.getOpcode() == TaintUtils.ALWAYS_AUTOBOX) {
							VarInsnNode vinsn = ((VarInsnNode) insn);
							BasicValue v = (BasicValue) f.getLocal(vinsn.var);
							if (v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()))
								relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
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
									&& (
											(TaintUtils.isPrimitiveOrPrimitiveArrayType(vt) || TaintUtils.isPrimitiveOrPrimitiveArrayType(value.getType()))
											|| (TaintUtils.isPrimitiveOrPrimitiveArrayType(value.getType())
													&& !TaintUtils.isPrimitiveOrPrimitiveArrayType(vt)))) {
									relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
							}
							break;
						}
						break;
					case AbstractInsnNode.TABLESWITCH_INSN:
					case AbstractInsnNode.LOOKUPSWITCH_INSN:
						if(Configuration.WITH_TAGS_FOR_JUMPS)
						{
							BasicValue value = (BasicValue) f.getStack(f.getStackSize() - 1);
							if (value instanceof SinkableArrayValue
									&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
									relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
							}	
						}
						break;
					case AbstractInsnNode.IINC_INSN:
						if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
						{
							IincInsnNode iinc = (IincInsnNode) insn;
							BasicValue value = (BasicValue) f.getLocal(iinc.var);
							if (value instanceof SinkableArrayValue
									&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
									relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
							}	
						}
						break;
					case AbstractInsnNode.JUMP_INSN:
						if(Configuration.WITH_TAGS_FOR_JUMPS)
						{
							switch(insn.getOpcode())
							{
							case Opcodes.IFGE:
							case Opcodes.IFGT:
							case Opcodes.IFLE:
							case Opcodes.IFLT:
							case Opcodes.IFNE:
							case Opcodes.IFEQ:
								BasicValue value = (BasicValue) f.getStack(f.getStackSize() - 1);
								if (value instanceof SinkableArrayValue
										&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
										relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
								}
								break;
							case Opcodes.IF_ICMPEQ:
							case Opcodes.IF_ICMPLE:
							case Opcodes.IF_ICMPLT:
							case Opcodes.IF_ICMPGT:
							case Opcodes.IF_ICMPGE:
							case Opcodes.IF_ICMPNE:
								value = (BasicValue) f.getStack(f.getStackSize() - 1);
								if (value instanceof SinkableArrayValue
										&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
										relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
								}
								value = (BasicValue) f.getStack(f.getStackSize() - 2);
								if (value instanceof SinkableArrayValue
										&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
										relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
								}
								break;
							}
						}
						break;
					case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
						InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) insn;
						Type[] margs = Type.getArgumentTypes(idin.desc);
//						System.out.println(desc);
						for (int j = 0; j < margs.length; j++) {
							Object o = f.getStack(f.getStackSize() - (margs.length - j));
//															System.out.println(o);
							if (o instanceof SinkableArrayValue) {
								SinkableArrayValue v = (SinkableArrayValue) o;
								Type t = margs[j];
								//if we make an inst call and are passing a prim array type, then we need the tags
								if (TaintUtils.isPrimitiveOrPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
									relevantValues.addAll(v.tag(insn));
								}
								//regardless of the kind of call, if we upcast then we need it
								else if (t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(v.getType()) && !v.flowsToInstMethodCall
										//&& !min.owner.equals("java/lang/System") && !min.name.equals("arrayCopy")
										//TODO: when doing this for *uninst* arrays, it's ok to just leave as is and not patch
										) {
//																			System.out.println("Releveant!");
									relevantValues.addAll(v.tag(insn));
								}
							}
						}
						break;
					case AbstractInsnNode.METHOD_INSN:
						MethodInsnNode min = (MethodInsnNode) insn;
						margs = Type.getArgumentTypes(((MethodInsnNode) insn).desc);

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
							if (o instanceof SinkableArrayValue && ((SinkableArrayValue)o).getType() != null) {
//								System.out.println(min.name+min.desc);
//								System.out.println(((SinkableArrayValue) o).src);
//								System.out.println(insn);

								relevantValues.addAll(((SinkableArrayValue) o).tag(insn));; //being called on a primitive array type
//								System.out.println(o);
							}
						}
						for (int j = 0; j < margs.length; j++) {
							Object o = f.getStack(f.getStackSize() - (margs.length - j));

							if (o instanceof SinkableArrayValue) {
								SinkableArrayValue v = (SinkableArrayValue) o;
								Type t = margs[j];
								//if we make an inst call and are passing a prim array type, then we need the tags
								if (TaintUtils.isPrimitiveOrPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
									relevantValues.addAll(v.tag(insn));
								}
								//regardless of the kind of call, if we upcast then we need it
								else if ((t.getDescriptor().equals("Ljava/lang/Object;") || t.getDescriptor().equals("Ljava/io/Serializable;")) && (TaintUtils.isPrimitiveArrayType(v.getType())) && !v.flowsToInstMethodCall
										//&& !min.owner.equals("java/lang/System") && !min.name.equals("arrayCopy")
										//TODO: when doing this for *uninst* arrays, it's ok to just leave as is and not patch
										) {
									relevantValues.addAll(v.tag(insn));
								}
								else if(t.getDescriptor().equals("Ljava/lang/Object;") && min.owner.equals("java/lang/System") && min.name.equals("arraycopy") && !v.flowsToInstMethodCall)
									relevantValues.addAll(v.tag(insn));
							}
						}

						break;
					case AbstractInsnNode.TYPE_INSN:
						switch (insn.getOpcode()) {
						case Opcodes.CHECKCAST:
							BasicValue value = (BasicValue) f.getStack(f.getStackSize() - 1);
//							System.out.println(insn.getPrevious() + "CH " + value);
//							if(value instanceof SinkableArrayValue)
//							System.out.println(((SinkableArrayValue)value).deps);
							if (value instanceof SinkableArrayValue && ((TypeInsnNode) insn).desc.startsWith("java/") && TaintUtils.isPrimitiveArrayType(value.getType())
									&& !((SinkableArrayValue) value).flowsToInstMethodCall) {
								relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
							}
							break;
						case Opcodes.ANEWARRAY:
							if(Configuration.ARRAY_LENGTH_TRACKING)
								relevantValues.addAll(((SinkableArrayValue)f.getStack(f.getStackSize() - 1)).tag(insn));
							break;
						}
						break;
					case AbstractInsnNode.MULTIANEWARRAY_INSN:
						if(Configuration.ARRAY_LENGTH_TRACKING)
						{
							MultiANewArrayInsnNode ins = (MultiANewArrayInsnNode) insn;
							for (int j = 1; j <= ins.dims; j++)
								relevantValues.addAll(((SinkableArrayValue)f.getStack(f.getStackSize() - j)).tag(insn));
						}
						break;
					}
					insn = insn.getNext();
				}

				HashSet<SinkableArrayValue> swapPop = new HashSet<>();
				
				insn = this.instructions.getFirst();
				for (int i = 0; i < frames.length; i++) {
					// // System.out.print(i + " " );
					String fr = "";
					if (frames[i] != null)
						for (int j = 0; j < frames[i].getStackSize(); j++)
							fr += (frames[i].getStack(j)) + " ";
					String fr2 = "";
					if (frames[i] != null)
						for (int j = 0; j < frames[i].getLocals(); j++)
						{
							fr2 += (frames[i].getLocal(j)) + " ";
						}
//					  System.out.println(fr);
//					this.instructions.insertBefore(insn, new LdcInsnNode(fr));
//					this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
//					this.instructions.insertBefore(insn, new LdcInsnNode(fr2));
					if (insn.getType() == AbstractInsnNode.FRAME && frames[i] != null) {
						FrameNode fn = (FrameNode) insn;
//						System.out.println("Found frame " + i +", stack size" + fn.stack.size() + ", fn stack " + frames[i].getStackSize() + ", local size " + fn.local.size() + ", fn " + frames[i].getLocals());
						int analyzerFrameIdx = 0;
						for (int j = 0; j < fn.local.size(); j++) {
							Object valInExistingFrame = fn.local.get(j);
//							System.out.println(j+":"+l);
							BasicValue calculatedVal = (BasicValue) frames[i].getLocal(analyzerFrameIdx);

//							System.out.println(l + " vs  " + v);
							if (calculatedVal instanceof SinkableArrayValue && ((SinkableArrayValue) calculatedVal).flowsToInstMethodCall && 
									(TaintAdapter.isPrimitiveStackType(valInExistingFrame) || valInExistingFrame == Opcodes.NULL || valInExistingFrame.equals("java/lang/Object")
									)&& valInExistingFrame != Opcodes.TOP) {
								ignoreExistingFrames = false;
//								if(calculatedVal.getType() == null)
//								{
//									System.out.println("!!N" + " vs " + valInExistingFrame);
//								}
//																	System.out.println(">"+l + " vs  " + v);
//								if(!TaintUtils.getStackTypeForType(calculatedVal.getType()).equals(valInExistingFrame))
//								{
//									System.out.println(valInExistingFrame  + " -- " + calculatedVal);
//								}
								if(valInExistingFrame.equals("java/lang/Object")) //someone was lazy when they made this frame...
									fn.local.set(j, new TaggedValue(TaintUtils.getStackTypeForType(calculatedVal.getType())));
								else
									fn.local.set(j, new TaggedValue((ignoreExistingFrames ? TaintUtils.getStackTypeForType(calculatedVal.getType()) : valInExistingFrame)));
//								fn.local.set(j, new TaggedValue(l));
							}
							
							if((calculatedVal instanceof SinkableArrayValue) && calculatedVal.getType() == null && valInExistingFrame != Opcodes.NULL && valInExistingFrame != Opcodes.TOP)
							{
//								System.out.println("!!" +v+l);
//								fn.local.set(j, new TaggedValue(Opcodes.NULL));
							}
							analyzerFrameIdx++;
							if (valInExistingFrame == Opcodes.DOUBLE || valInExistingFrame == Opcodes.LONG)
								analyzerFrameIdx++;
						}
						for (int j = 0; j < fn.stack.size(); j++) {
							Object l = fn.stack.get(j);
							BasicValue v = (BasicValue) frames[i].getStack(j);
							//								System.out.println(j + "ZZZ "+ l + " vs " + v);
							if (v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall && (TaintAdapter.isPrimitiveStackType(l) || l == Opcodes.NULL || l.equals("java/lang/Object"))) {
								fn.stack.set(j, new TaggedValue((ignoreExistingFrames ? TaintUtils.getStackTypeForType(v.getType()) : l)));
							}
						}
					}
					insn = insn.getNext();
				}

				HashSet<AbstractInsnNode> added = new HashSet<AbstractInsnNode>();
//				for(SinkableArrayValue v : liveValues)
//				{
//					System.out.println(System.identityHashCode(v));
//				}
				for (SinkableArrayValue v : relevantValues) {
					if (!added.add(v.getSrc()))
						continue;
//											System.out.println(v.getType());
//											System.out.println(v.src);
//											if (v.src instanceof VarInsnNode)
//												System.out.println(Printer.OPCODES[v.src.getOpcode()] + ((VarInsnNode) v.src).var);
//											else if(v.src instanceof InsnNode)
//												System.out.println(Printer.OPCODES[v.src.getOpcode()] + ", " + v.flowsToInstMethodCall+","+(v.srcVal != null ? v.srcVal.flowsToInstMethodCall : "."));
					if (this.instructions.contains(v.getSrc())) {
						if (v.masterDup != null || !v.otherDups.isEmpty()) {
							SinkableArrayValue masterDup = v;
							if(v.masterDup != null)
								masterDup = v.masterDup;
							int i = 0;
							if(relevantValues.contains(masterDup)) //bottom one is relevant
								this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.DUP_TAINT_AT_0+i));
							i++;
							if(v.getSrc().getOpcode() == Opcodes.DUP2_X1)
								i+=2;
//							this.instructions.insertBefore(v.getSrc(), new LdcInsnNode(masterDup.toString() +  " " +masterDup.otherDups.toString()));
							for(SinkableArrayValue d : masterDup.otherDups)
							{
								if(relevantValues.contains(d))
									this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.DUP_TAINT_AT_0+i));
								i++;
							}
						}
						else {
							if(v.getSrc().getOpcode() == Opcodes.DUP_X1)
								throw new IllegalStateException();
//							System.out.println("tag "+ Printer.OPCODES[v.getSrc().getOpcode()] + " -> " + v);
							this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.TRACKED_LOAD));
						}
					}
				}
				for(SinkableArrayValue v : swapPop)
				{
					this.instructions.insert(v.getSrc(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
					this.instructions.insert(v.getSrc(), new InsnNode(Opcodes.POP));
					this.instructions.insert(v.getSrc(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
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
	public static void main(String[] args) throws Throwable {
//		Configuration.IMPLICIT_TRACKING =false;
		Configuration.IMPLICIT_LIGHT_TRACKING = true;
		Configuration.ARRAY_LENGTH_TRACKING = true;
		Configuration.ARRAY_INDEX_TRACKING = true;
		Configuration.ANNOTATE_LOOPS = true;
//		Instrumenter.instrumentClass("asdf", new FileInputStream("z.class"), false);
		ClassReader cr = new ClassReader(new FileInputStream("z.class"));
//		ClassReader cr = new ClassReader(new FileInputStream("target/test-classes/Foo.class"));
		final String className = cr.getClassName();
		PrintWriter pw = new PrintWriter("z.txt");
		TraceClassVisitor tcv = new TraceClassVisitor(null, new PhosphorTextifier(), pw);
		
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
				{
			@Override
			protected String getCommonSuperClass(String arg0, String arg1) {
				try{
				return super.getCommonSuperClass(arg0, arg1);
				}
				catch(Throwable t)
				{
					return "java/lang/Object";
				}
			}
				};
		cr.accept(new ClassVisitor(Opcodes.ASM5,cw1) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
				return mv;
			}
		}, ClassReader.EXPAND_FRAMES);
		cr = new ClassReader(cw1.toByteArray());
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, tcv) {
			String className;

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, superName, interfaces);
				this.className = name;
			}
			HashSet<FieldNode> fields = new HashSet<FieldNode>();
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				fields.add(new FieldNode(access, name, desc, signature, value));
				return super.visitField(access, name, desc, signature, value);
			}
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				mv = new MethodVisitor(Opcodes.ASM5,mv) {
					@Override
					public void visitInsn(int opcode) {
						super.visitInsn(opcode);
					}
				};
//				NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, className, desc, mv);
				mv = new TaintLoadCoercer(className, access, name, desc, signature, exceptions, mv, true);
//				LocalVariableManager lvs = new LocalVariableManager(access, desc, mv, analyzer, mv, false);
//				mv = lvs;
				PrimitiveArrayAnalyzer paa = new PrimitiveArrayAnalyzer(className,access,name,desc,signature,exceptions,mv);
				NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, access, name, desc, paa);
				paa.setAnalyzer(an);
//				((PrimitiveArrayAnalyzer) mv).setAnalyzer(an);
				mv = an;
				//				ConstantValueNullTaintGenerator ctvn = new ConstantValueNullTaintGenerator(className, access, name, desc, signature, exceptions, mv);
				//				mv = ctvn;

				return mv;
			}
		};
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		pw.flush();
		
	}
}
