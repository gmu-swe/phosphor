package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.*;
import edu.columbia.cs.psl.phosphor.runtime.CharacterUtils;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class TaintLoadCoercer extends MethodVisitor implements Opcodes {
	PrimitiveArrayAnalyzer primitiveArrayFixer;

	public void setPrimitiveArrayFixer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.primitiveArrayFixer = primitiveArrayFixer;
	}
	private boolean ignoreExistingFrames;
	private InstOrUninstChoosingMV instOrUninstChoosingMV;
	private boolean aggressivelyReduceMethodSize;
	public TaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv, boolean ignoreExistingFrames, final InstOrUninstChoosingMV instOrUninstChoosingMV, boolean aggressivelyReduceMethodSize) {
		super(Opcodes.ASM5);
		this.mv = new UninstTaintLoadCoercerMN(className, access, name, desc, signature, exceptions, cmv);
		this.ignoreExistingFrames = ignoreExistingFrames;
		this.instOrUninstChoosingMV = instOrUninstChoosingMV;
		this.aggressivelyReduceMethodSize = aggressivelyReduceMethodSize;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		if (owner.equals(Type.getInternalName(Character.class)) && (name.startsWith("codePointAt") || name.startsWith("toChars") || name.startsWith("codePointBefore")
		|| name.startsWith("reverseBytes")
		|| name.startsWith("toLowerCase")
		|| name.startsWith("toTitleCase")
		|| name.startsWith("toUpperCase")
		)) {
			super.visitMethodInsn(opcode, Type.getInternalName(CharacterUtils.class), name, descriptor, isInterface);
		} else
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
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
//				System.out.println("UTLC " + name + desc + " " + this.instructions.size());
				
				boolean canIgnoreTaintsTillEnd = true;
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
						case Opcodes.GETFIELD:
						case Opcodes.GETSTATIC:
							canIgnoreTaintsTillEnd = false;
							break;
						case Opcodes.PUTFIELD:
							canIgnoreTaintsTillEnd = false;
						case Opcodes.PUTSTATIC:
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
						if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING || Configuration.WITH_TAGS_FOR_JUMPS)
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
						canIgnoreTaintsTillEnd = false;
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
						canIgnoreTaintsTillEnd = false;
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
				if(this.instructions.size() > 20000) {
					if (canIgnoreTaintsTillEnd) {
//					System.out.println("Can opt " + className + name + " "+canIgnoreTaintsTillEnd);
						insn = this.instructions.getFirst();
						this.instructions.insert(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
						while (insn != null) {
							if (insn.getOpcode() == PUTSTATIC) {
								Type origType = Type.getType(((FieldInsnNode) insn).desc);
								if (origType.getSort() == Type.ARRAY && (origType.getElementType().getSort() != Type.OBJECT || origType.getElementType().getInternalName().equals("java/lang/Object"))) {
									if (origType.getElementType().getSort() != Type.OBJECT) {
										Type wrappedType = MultiDTaintedArray.getTypeForType(origType);
										if (origType.getDimensions() == 1) {
											this.instructions.insertBefore(insn, new InsnNode(DUP));
											this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
											this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
											this.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, ((FieldInsnNode) insn).owner, ((FieldInsnNode) insn).name + TaintUtils.TAINT_FIELD, wrappedType.getDescriptor()));
										} else {
											((FieldInsnNode) insn).desc = wrappedType.getDescriptor();
											this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
											this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
										}
									} else {
										this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
										this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, origType.getInternalName()));
									}
								}
							} else if (insn.getOpcode() == ARETURN) {
								Type origType = Type.getReturnType(this.desc);
								if (origType.getSort() == Type.ARRAY && (origType.getElementType().getSort() != Type.OBJECT || origType.getElementType().getInternalName().equals("java/lang/Object"))) {
									Type wrappedType = MultiDTaintedArray.getTypeForType(origType);
									this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
									this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
								}
							}
							insn = insn.getNext();
						}
						super.visitEnd();
						this.accept(cmv);
						return;
					}
					else if(aggressivelyReduceMethodSize) //only do this if it's REALLY bad
					{
						//Not able to use these cheap tricks. Let's bail.
						insn = this.instructions.getFirst();
						this.instructions.insert(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
						this.instructions.insert(new VarInsnNode(TaintUtils.IGNORE_EVERYTHING,0));
						instOrUninstChoosingMV.disableTainting();
						super.visitEnd();
						this.accept(cmv);
						return;
					}

				}

				HashSet<SinkableArrayValue> swapPop = new HashSet<>();
				
				insn = this.instructions.getFirst();
				for (int i = 0; i < frames.length; i++) {
					// // System.out.print(i + " " );
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
				//Before we start mucking around with the instruction list, find the analyzed frame that exists at each relevant value source
				HashMap<SinkableArrayValue,Frame> framesAtValues = new HashMap<>();
				for(SinkableArrayValue v : relevantValues)
				{
					if (v.getSrc() != null && framesAtValues.get(v) == null) {
						if (this.instructions.contains(v.getSrc())) {
							int k = this.instructions.indexOf(v.getSrc());
							if (k < frames.length && frames[k] != null) {
								framesAtValues.put(v, frames[k]);
							}
						}
					}

				}

				//for debug
//				insn = this.instructions.getFirst();
//				for(int i = 0; i< frames.length; i++)
//				{
//					String fr = "";
//					if (frames[i] != null)
//						for (int j = 0; j < frames[i].getStackSize(); j++)
//							fr += (frames[i].getStack(j)) + " ";
//					String fr2 = "";
//					if (frames[i] != null)
//						for (int j = 0; j < frames[i].getLocals(); j++)
//						{
//							fr2 += (frames[i].getLocal(j)) + " ";
//						}
////					  System.out.println(fr);
//					this.instructions.insertBefore(insn, new LdcInsnNode(fr));
//					this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
//					insn = insn.getNext();
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
							if(framesAtValues.containsKey(v))
							{
								Frame f = framesAtValues.get(v);
								Object r = f.getStack(f.getStackSize()-1);
								if(!(r instanceof SinkableArrayValue))
								{
									continue;
								}
							}
							SinkableArrayValue masterDup = v;
							if (v.masterDup != null)
								masterDup = v.masterDup;
							int i = 0;

							if(relevantValues.contains(masterDup)) //bottom one is relevant
								this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.DUP_TAINT_AT_0+i));
							i++;
							if(v.getSrc().getOpcode() == Opcodes.DUP2_X1)
								i+=2;
//							this.instructions.insertBefore(v.getSrc(), new LdcInsnNode(masterDup.toString() +  " " +masterDup.otherDups.toString()));
//							this.instructions.insertBefore(v.getSrc(),new InsnNode(Opcodes.POP));
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

			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				PrintWriter pw = null;
				try {
					pw = new PrintWriter("lastMethod.txt");
					TraceClassVisitor tcv = new TraceClassVisitor(null, new PhosphorTextifier(), pw);
					this.accept(tcv);
					tcv.visitEnd();
					pw.flush();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				throw new IllegalStateException(e);
			}
			super.visitEnd();


			this.accept(cmv);
		}
	}
	public static void main(String[] args) throws Throwable {
		Configuration.IMPLICIT_TRACKING =true;
		Configuration.MULTI_TAINTING =true;
		Configuration.IMPLICIT_EXCEPTION_FLOW = true;
//		Configuration.IMPLICIT_LIGHT_TRACKING = true;
//		Configuration.ARRAY_LENGTH_TRACKING = true;
//		Configuration.ARRAY_INDEX_TRACKING = true;
//		Configuration.ANNOTATE_LOOPS = true;
//		Instrumenter.instrumentClass("asdf", new FileInputStream("z.class"), false);
		ClassReader cr = new ClassReader(new FileInputStream("z.class"));
//		ClassReader cr = new ClassReader(new FileInputStream("target/classes/VarInLoop.class"));
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
//				mv = new SpecialOpcodeRemovingMV(mv,false,className,false);
//				NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, className, desc, mv);
				mv = new TaintLoadCoercer(className, access, name, desc, signature, exceptions, mv, true, null, false);
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
