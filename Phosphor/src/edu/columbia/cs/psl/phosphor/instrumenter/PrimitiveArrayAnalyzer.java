package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;
import java.util.Map.Entry;

public class PrimitiveArrayAnalyzer extends MethodVisitor {
	public boolean isEmptyMethod = true;

	final class PrimitiveArrayAnalyzerMN extends MethodNode {
		private final String className;
		private final boolean shouldTrackExceptions;
		private final MethodVisitor cmv;
		boolean[] endsWithGOTO;
		int curLabel = 0;
		HashMap<Integer, Boolean> lvsThatAreArrays = new HashMap<Integer, Boolean>();
		ArrayList<FrameNode> inFrames = new ArrayList<FrameNode>();
		ArrayList<FrameNode> outFrames = new ArrayList<FrameNode>();

		public PrimitiveArrayAnalyzerMN(int access, String name, String desc, String signature, String[] exceptions, String className, MethodVisitor cmv) {
			super(Opcodes.ASM5,access, name, desc, signature, exceptions);
			this.className = className;
			this.cmv = cmv;
			if(Configuration.IMPLICIT_EXCEPTION_FLOW)
				shouldTrackExceptions = true;
			else
				shouldTrackExceptions = false;
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
		public void visitCode() {
			if (DEBUG)
				System.out.println("Visiting: " + className + "." + name + desc);
			Label firstLabel = new Label();
			super.visitCode();
			visitLabel(firstLabel);

		}

		//			@Override
		//			public void visitVarInsn(int opcode, int var) {
		//				if(opcode == Opcodes.ASTORE)
		//				{
		//					boolean isPrimArray = TaintAdapter.isPrimitiveStackType(analyzer.stack.get(analyzer.stack.size() - 1));
		//					if(lvsThatAreArrays.containsKey(var))
		//					{
		//						if(lvsThatAreArrays.get(var) != isPrimArray)
		//						{
		//							throw new IllegalStateException("This analysis is currently too lazy to handle when you have 1 var slot take different kinds of arrays");
		//						}
		//					}
		//					lvsThatAreArrays.put(var, isPrimArray);
		//				}
		//				super.visitVarInsn(opcode, var);
		//			}
		 private  void visitFrameTypes(final int n, final Object[] types,
		            final List<Object> result) {
		        for (int i = 0; i < n; ++i) {
		            Object type = types[i];
		            result.add(type);
		            if (type == Opcodes.LONG || type == Opcodes.DOUBLE) {
		                result.add(Opcodes.TOP);
		            }
		        }
		    }

		FrameNode generateFrameNode(int type, int nLocal, Object[] local, int nStack, Object[] stack)
		{
			FrameNode ret = new FrameNode(type, nLocal, local, nStack, stack);
			ret.local = new ArrayList<Object>();
			ret.stack= new ArrayList<Object>();
			visitFrameTypes(nLocal, local, ret.local);
			visitFrameTypes(nStack, stack, ret.stack);
			return ret;
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			if (DEBUG)
				System.out.println("Visitframe curlabel " + (curLabel - 1));
			super.visitFrame(type, nLocal, local, nStack, stack);
			if (DEBUG)
				System.out.println("label " + (curLabel - 1) + " reset to " + Arrays.toString(stack));
			if (inFrames.size() == curLabel - 1)
				inFrames.add(generateFrameNode(type, nLocal, local, nStack, stack));
			else
				inFrames.set(curLabel - 1, generateFrameNode(type, nLocal, local, nStack, stack));
//							System.out.println(name+" " +Arrays.toString(local));
			//				if (curLabel > 0) {
			//				System.out.println("And resetting outframe " + (curLabel - 2));
			//					if (outFrames.size() == curLabel - 1)
			//						outFrames.add(new FrameNode(type, nLocal, local, nStack, stack));
			//					 if(outFrames.get(curLabel -1) == null)
			//						outFrames.set(curLabel - 1, new FrameNode(type, nLocal, local, nStack, stack));
			//				}
		}

		@Override
		public void visitLabel(Label label) {
			//				if (curLabel >= 0)
			if (DEBUG)
				System.out.println("Visit label: " + curLabel + " analyzer: " + analyzer.stack + " inframes size " + inFrames.size() + " " + outFrames.size());
			if (analyzer.locals == null || analyzer.stack == null)
				inFrames.add(new FrameNode(0, 0, new Object[0], 0, new Object[0]));
			else
				inFrames.add(new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), analyzer.stack.size(), analyzer.stack.toArray()));
			//				if (outFrames.size() <= curLabel) {
			//					if(analyzer.stack == null)
			outFrames.add(null);
			if (curLabel > 0 && outFrames.get(curLabel - 1) == null && analyzer.stack != null)
				outFrames.set(curLabel - 1, new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), analyzer.stack.size(), analyzer.stack.toArray()));
			if (DEBUG)
				System.out.println("Added outframe for " + (outFrames.size() - 1) + " : " + analyzer.stack);
			//				}

			super.visitLabel(label);
			curLabel++;
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			if (DEBUG)
				System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);
			outFrames.set(curLabel - 1, new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), analyzer.stack.size(), analyzer.stack.toArray()));
			super.visitTableSwitchInsn(min, max, dflt, labels);
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			if (DEBUG)
				System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);
			outFrames.set(curLabel - 1, new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), analyzer.stack.size(), analyzer.stack.toArray()));
			super.visitLookupSwitchInsn(dflt, keys, labels);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ATHROW) {
				if (DEBUG)
					System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);
				if (analyzer.locals != null && analyzer.stack != null)
					outFrames.set(curLabel - 1, new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), analyzer.stack.size(), analyzer.stack.toArray()));
			}
			super.visitInsn(opcode);
		}

		public void visitJumpInsn(int opcode, Label label) {
			//				System.out.println(opcode);
			//				if (opcode == Opcodes.GOTO) {
			super.visitJumpInsn(opcode, label);
			int nToPop = 0;
			switch (opcode) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				//pop 1
				nToPop = 1;
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
				//pop 2
				nToPop = 2;
				break;
			case Opcodes.GOTO:
				//pop none
				break;
			default:
				throw new IllegalArgumentException();
			}
			//The analyzer won't have executed yet, so simulate it did :'(
			List<Object> stack = new ArrayList<Object>(analyzer.stack);
			//				System.out.println("got to remove " + nToPop +  " from " + analyzer.stack + " in " + className + "."+name );
			while (nToPop > 0 && !stack.isEmpty()) {
				stack.remove(stack.size() - 1);
				nToPop--;
			}

			if (DEBUG)
				System.out.println(name + " Rewriting " + curLabel + " OUT to " + stack);
			outFrames.set(curLabel - 1, new FrameNode(0, analyzer.locals.size(), analyzer.locals.toArray(), stack.size(), stack.toArray()));
			visitLabel(new Label());
			//				}

		}

		@Override
		public void visitEnd() {
			final HashMap<Integer, LinkedList<Integer>> neverAutoBoxByFrame = new HashMap<Integer, LinkedList<Integer>>();
			final HashMap<Integer, LinkedList<Integer>> alwaysAutoBoxByFrame = new HashMap<Integer, LinkedList<Integer>>();
			final HashMap<Integer, LinkedList<Integer>> outEdges = new HashMap<Integer, LinkedList<Integer>>();
			final HashSet<Integer> insertACHECKCASTBEFORE = new HashSet<Integer>();
			final HashSet<Integer> insertACONSTNULLBEFORE = new HashSet<Integer>();
			Analyzer a = new Analyzer(new BasicArrayInterpreter((this.access & Opcodes.ACC_STATIC) != 0)) {
			    protected int[] insnToLabel;

				int getLabel(int insn) {
					int label = -1;
					for (int j = 0; j <= insn; j++) {
						label = insnToLabel[j];
					}
					return label;
				}

				int getInsnAfterFrameFor(int insn) {
					int r = 0;
					for (int i = 0; i < insn; i++) {
						if (instructions.get(i).getType() == AbstractInsnNode.FRAME)
							r = i + 1;
					}
					return r;
				}

				int getLastInsnByLabel(int label) {
					int r = 0;
					for (int j = 0; j < insnToLabel.length; j++) {
						if (insnToLabel[j] == label) {
							if (instructions.get(j).getType() == AbstractInsnNode.FRAME)
								continue;
							r = j;
						}
					}
					return r;
				}

				int getFirstInsnByLabel(int label) {
					for (int j = 0; j < insnToLabel.length; j++) {
						if (insnToLabel[j] == label) {
							if (instructions.get(j).getType() == AbstractInsnNode.FRAME || instructions.get(j).getType() == AbstractInsnNode.LABEL
									|| instructions.get(j).getType() == AbstractInsnNode.LINE)
								continue;
							return j;
						}
					}
					return -1;
				}

				protected Frame newFrame(int nLocals, int nStack) {
					return new Frame(nLocals, nStack){
						@Override
						public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {
							if(insn.getOpcode() > 200)
								return;
							super.execute(insn, interpreter);
						}
					};
				}
				@Override
				public Frame[] analyze(String owner, MethodNode m) throws AnalyzerException {
					Iterator<AbstractInsnNode> insns = m.instructions.iterator();
					insnToLabel = new int[m.instructions.size()];
//											System.out.println("PAAA"+ name);
					int label = -1;
					boolean isFirst = true;
					while (insns.hasNext()) {
						AbstractInsnNode insn = insns.next();
						int idx = m.instructions.indexOf(insn);

						if (insn instanceof LabelNode) {
							label++;
						}

						insnToLabel[idx] = (isFirst ? 1 : label);
						isFirst = false;
						//														System.out.println(idx + "->"+label);
					}
					Frame[] ret = super.analyze(owner, m);
					if (shouldTrackExceptions) {
						insns = m.instructions.iterator();
						while (insns.hasNext()) {
							AbstractInsnNode insn = insns.next();
							int idx = m.instructions.indexOf(insn);
							if (insn.getOpcode() == Opcodes.ATHROW) {
								//Are we in a try/catch block that can catch this?


								for(TryCatchBlockNode each : m.tryCatchBlocks){
									try{
										Class<?> caught = Class.forName((each.type == null ? "java.lang.Throwable" : each.type.replace('/','.')),false,PrimitiveArrayAnalyzer.class.getClassLoader());
										if(caught == Throwable.class){
											//if catching Throwable, we'll catch this regardless of whether we can load the exception type or not
											newControlFlowEdge(idx,m.instructions.indexOf(each.handler));
											break;
										}
										Class<?> thrown = null;
										try{
											String onStack = ret[idx].getStack(ret[idx].getStackSize()-1).toString();
											if(!onStack.startsWith("L"))
												continue;
											thrown = Class.forName(Type.getType(ret[idx].getStack(0).toString()).getClassName(),false, PrimitiveArrayAnalyzer.class.getClassLoader());
										}catch (Throwable t){
											continue;
										}
										if(caught.isAssignableFrom(thrown))
										{
											//Found a handler for this thrown
											newControlFlowEdge(idx,m.instructions.indexOf(each.handler));
											break;
										}
									}catch(Throwable t){} //Maybe can't load exception type, that's ok
								}
							}
						}
					}

//					if (DEBUG)
//						for (int i = 0; i < inFrames.size(); i++) {
//							System.out.println("IN: " + i + " " + inFrames.get(i).stack);
//						}
//					if (DEBUG)
//						for (int i = 0; i < outFrames.size(); i++) {
//							System.out.println("OUT: " + i + " " + (outFrames.get(i) == null ? "null" : outFrames.get(i).stack));
//						}

					for (Entry<Integer, LinkedList<Integer>> edge : edges.entrySet()) {
					    Integer successor = edge.getKey();
						if (edge.getValue().size() > 1) {
							int labelToSuccessor = getLabel(successor);

							if (DEBUG)
								System.out.println(name + " Must merge: " + edge.getValue() + " into " + successor + " AKA " + labelToSuccessor);
							if (DEBUG)
								System.out.println("Input to successor: " + inFrames.get(labelToSuccessor).stack);

							for (Integer toMerge : edge.getValue()) {
								if(shouldTrackExceptions)
								{
									BasicBlock b = implicitAnalysisblocks.get(toMerge);
									if(b.insn.getOpcode() == Opcodes.ATHROW)
										continue;
								}
								int labelToMerge = getLabel(toMerge);
								if (DEBUG)
									System.out.println(toMerge + " AKA " + labelToMerge);
								if (DEBUG)
									System.out.println((outFrames.get(labelToMerge) == null ? "null" : outFrames.get(labelToMerge).stack));
								if(inFrames.get(labelToSuccessor) == null || outFrames.get(labelToMerge) == null) //e.g. for an edge into the first instruction in an exception handler
									continue;
								if (!outFrames.get(labelToMerge).stack.isEmpty() && !inFrames.get(labelToSuccessor).stack.isEmpty()) {
									Object output1Top = outFrames.get(labelToMerge).stack.get(outFrames.get(labelToMerge).stack.size() - 1);
									Object inputTop = inFrames.get(labelToSuccessor).stack.get(inFrames.get(labelToSuccessor).stack.size() - 1);
									if (output1Top == Opcodes.TOP)
										output1Top = outFrames.get(labelToMerge).stack.get(outFrames.get(labelToMerge).stack.size() - 2);
									if (inputTop == Opcodes.TOP)
										inputTop = inFrames.get(labelToSuccessor).stack.get(inFrames.get(labelToSuccessor).stack.size() - 2);
//									System.out.println(className+"."+name+ " IN"+inputTop +" OUT " + output1Top);
									if (output1Top != null && output1Top != inputTop) {
										Type inputTopType = TaintAdapter.getTypeForStackType(inputTop);
										Type outputTopType = TaintAdapter.getTypeForStackType(output1Top);
										if ((output1Top == Opcodes.NULL) && inputTopType.getSort() == Type.ARRAY && inputTopType.getElementType().getSort() != Type.OBJECT
												&& inputTopType.getDimensions() == 1) {
											insertACONSTNULLBEFORE.add(toMerge);
										} else if ((inputTopType.getSort() == Type.OBJECT || (inputTopType.getSort() == Type.ARRAY && inputTopType.getElementType().getSort() == Type.OBJECT)) && outputTopType.getSort() == Type.ARRAY && outputTopType.getElementType().getSort() != Type.OBJECT
												&& inputTopType.getDimensions() == 1) {
											insertACHECKCASTBEFORE.add(toMerge);
										}
									}
								}
								if (!outFrames.get(labelToMerge).local.isEmpty() && !inFrames.get(labelToSuccessor).local.isEmpty()) {
									for (int i = 0; i < Math.min(outFrames.get(labelToMerge).local.size(), inFrames.get(labelToSuccessor).local.size()); i++) {
										Object out = outFrames.get(labelToMerge).local.get(i);
										Object in = inFrames.get(labelToSuccessor).local.get(i);
//										System.out.println(name +" " +out + " out, " + in + " In" + " i "+i);
										if (out instanceof String && in instanceof String) {
											Type tout = Type.getObjectType((String) out);
											Type tin = Type.getObjectType((String) in);
											if (tout.getSort() == Type.ARRAY && tout.getElementType().getSort() != Type.OBJECT && tout.getDimensions() == 1 && tin.getSort() == Type.OBJECT) {
												int insnN = getLastInsnByLabel(labelToMerge);
//												System.out.println(name+desc);
//																							System.out.println(outFrames.get(labelToMerge).local + " out, \n" + inFrames.get(labelToSuccessor).local + " In" + " i "+i);
//												System.out.println("T1::"+tout + " to " + tin + " this may be unsupported but should be handled by the above! in label " + instructions.get(insnN));
//												System.out.println("In insn is " + getFirstInsnByLabel(labelToSuccessor));
//												System.out.println("insn after frame is " + insnN +", " + instructions.get(insnN) + "<"+Printer.OPCODES[instructions.get(insnN).getOpcode()]);
//													System.out.println(inFrames.get(labelToSuccessor).local);
												if (!alwaysAutoBoxByFrame.containsKey(insnN))
													alwaysAutoBoxByFrame.put(insnN, new LinkedList<Integer>());
												alwaysAutoBoxByFrame.get(insnN).add(i);
											}
										}
									}
								}
							}
						}
					}

					//TODO: if the output of a frame is an array but the input is an obj, hint to always box?
					//or is that necessary, because we already assume that it's unboxed.
					return ret;
				}

				HashMap<Integer, LinkedList<Integer>> edges = new HashMap<Integer, LinkedList<Integer>>();


				LinkedList<Integer> varsStoredThisInsn = new LinkedList<Integer>();
				HashSet<String> visited = new HashSet<String>();
				int insnIdxOrderVisited = 0;

				@Override
				protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
//					if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
//						this.newControlFlowEdge(insnIndex, successorIndex, true);
//					}
					return true;
				}

				@Override
				protected void newControlFlowEdge(int insn, int successor) {
					newControlFlowEdge(insn, successor, false);

				}

				protected void newControlFlowEdge(int insn, int successor, boolean isExceptionalEdge){
					if(visited.contains(insn+"-"+successor))
						return;
					visited.add(insn+"-"+successor);
					if (!edges.containsKey(successor))
						edges.put(successor, new LinkedList<Integer>());
					if (!edges.get(successor).contains(insn))
						edges.get(successor).add(insn);
					if (!outEdges.containsKey(insn))
						outEdges.put(insn, new LinkedList<Integer>());
					if (!outEdges.get(insn).contains(successor))
						outEdges.get(insn).add(successor);

					BasicBlock fromBlock;
					if(!implicitAnalysisblocks.containsKey(insn))
					{
						//insn not added yet
						fromBlock = new BasicBlock();
						fromBlock.idx = insn;
						fromBlock.insn = instructions.get(insn);
						implicitAnalysisblocks.put(insn,fromBlock);
					}
					else
						fromBlock = implicitAnalysisblocks.get(insn);

					AbstractInsnNode insnN = instructions.get(insn);
					fromBlock.isJump = (insnN.getType()== AbstractInsnNode.JUMP_INSN && insnN.getOpcode() != Opcodes.GOTO)
							|| insnN.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN || insnN.getType() == AbstractInsnNode.TABLESWITCH_INSN;
					if(fromBlock.isJump && insnN.getType() == AbstractInsnNode.JUMP_INSN)
					{
						switch(insnN.getOpcode())
						{
							case Opcodes.IF_ICMPEQ:
							case Opcodes.IF_ICMPNE:
							case Opcodes.IF_ICMPGE:
							case Opcodes.IF_ICMPGT:
							case Opcodes.IF_ICMPLT:
							case Opcodes.IF_ICMPLE:
							case Opcodes.IF_ACMPEQ:
							case Opcodes.IF_ACMPNE:
								fromBlock.is2ArgJump = true;
								break;
						}
					}
					BasicBlock succesorBlock;
					if(implicitAnalysisblocks.containsKey(successor))
						succesorBlock = implicitAnalysisblocks.get(successor);
					else
					{
						succesorBlock = new BasicBlock();
						succesorBlock.idx = successor;
						succesorBlock.insn = instructions.get(successor);
						implicitAnalysisblocks.put(successor, succesorBlock);
						if(succesorBlock.insn.getType() == AbstractInsnNode.IINC_INSN)
						{
							succesorBlock.varsWritten.add(new LVAccess(((IincInsnNode)succesorBlock.insn).var, "I"));
						}
						else if(succesorBlock.insn.getType() == AbstractInsnNode.VAR_INSN)
						{
							switch(succesorBlock.insn.getOpcode())
							{
								case ISTORE:
									succesorBlock.varsWritten.add(new LVAccess(((VarInsnNode)succesorBlock.insn).var, "I"));
									break;
								case ASTORE:
									succesorBlock.varsWritten.add(new LVAccess(((VarInsnNode)succesorBlock.insn).var, "Ljava/lang/Object;"));
									break;
								case DSTORE:
									succesorBlock.varsWritten.add(new LVAccess(((VarInsnNode)succesorBlock.insn).var, "D"));
									break;
								case LSTORE:
									succesorBlock.varsWritten.add(new LVAccess(((VarInsnNode)succesorBlock.insn).var, "J"));
									break;
							}
						}
						else if(succesorBlock.insn.getType() == AbstractInsnNode.FIELD_INSN)
						{
							FieldInsnNode fin = (FieldInsnNode) succesorBlock.insn;
							if(fin.getOpcode() == Opcodes.PUTFIELD)
							{
								Frame fr = this.getFrames()[successor];
								if(fr != null && fr.getStack(fr.getStackSize()-2) == BasicArrayInterpreter.THIS_VALUE) {
									succesorBlock.fieldsWritten.add(new Field(false,fin.owner,fin.name,fin.desc));
								}
							}
							else if(fin.getOpcode() == Opcodes.PUTSTATIC){
								succesorBlock.fieldsWritten.add(new Field(true,fin.owner,fin.name,fin.desc));
							}
						}
						else if(succesorBlock.insn.getType() == AbstractInsnNode.METHOD_INSN){
							MethodInsnNode min = (MethodInsnNode) succesorBlock.insn;
							if(min.getOpcode() == INVOKEVIRTUAL || min.getOpcode() == INVOKESPECIAL){
								Type[] desc = Type.getArgumentTypes(min.desc);
								if(
										(desc.length == 1 && (Type.getReturnType(min.desc).getSort()==Type.VOID || min.desc.equals("Ljava/lang/StringBuilder;")))
												||
												(desc.length == 2 && Type.getReturnType(min.desc).getSort() == Type.VOID && min.name.startsWith("set")))
								{
									Frame fr = this.getFrames()[successor];
									if(fr != null && fr.getStack(fr.getStackSize()-2) instanceof BasicArrayInterpreter.BasicThisFieldValue) {
										BasicArrayInterpreter.BasicThisFieldValue vv = (BasicArrayInterpreter.BasicThisFieldValue) fr.getStack(fr.getStackSize()-2);
										succesorBlock.fieldsWritten.add(vv.getField());
									}
								}
							}
						}
						else if(succesorBlock.insn.getOpcode() == Opcodes.ATHROW){
							BasicValue ex = (BasicValue) this.getFrames()[successor].getStack(0);
							if(shouldTrackExceptions && ex!= null && ex.getType() != null && (ex.getType().getDescriptor().contains("Exception") || ex.getType().getDescriptor().contains("Error")))
							{
								succesorBlock.exceptionsThrown.add(ex.getType().getInternalName() + "#" + successor);
							}
						}
					}
					fromBlock.successors.add(succesorBlock);
					succesorBlock.predecessors.add(fromBlock);

					if (fromBlock.isJump) {
						if (fromBlock.covered)
							succesorBlock.onTrueSideOfJumpFrom.add(fromBlock);
						else {
							succesorBlock.onFalseSideOfJumpFrom.add(fromBlock);
							fromBlock.covered = true;
						}
					}
					super.newControlFlowEdge(insn, successor);
				}
			};
			try {

				Frame[] frames = a.analyze(className, this);
				for(int i = 0 ; i < instructions.size(); i++)
				{
					if(frames[i] == null)
					{
						//TODO dead code elimination.
						//This should be done more generically
						//But, this worked for JDT's stupid bytecode, so...
						AbstractInsnNode insn = instructions.get(i);
						//while (insn != null && insn.getType() != AbstractInsnNode.LABEL) {
							if(insn.getOpcode() == Opcodes.ATHROW || insn.getOpcode() == Opcodes.GOTO || (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN))
							{
								instructions.insertBefore(insn, new InsnNode(Opcodes.ATHROW));
								instructions.remove(insn);
								break;
							}
							else if (insn instanceof FrameNode)
							{
								FrameNode fn = (FrameNode) insn;
								fn.local = Collections.EMPTY_LIST;
								fn.stack = Collections.singletonList((Object) "java/lang/Throwable");
							}
							else if (!(insn instanceof LineNumberNode) && !(insn instanceof LabelNode)){
								instructions.insertBefore(insn, new InsnNode(Opcodes.NOP));
								instructions.remove(insn);
							}
							//insn = insn.getNext();
					//	}
					}
				}
//				HashMap<Integer,BasicBlock> cfg = new HashMap<Integer, BasicBlock>();
//				for(Integer i : outEdges.keySet())
//				{
//					BasicBlock b = new BasicBlock();
//					b.idx = i;
//					b.outEdges = outEdges.get(i);
//					int endIdx = this.instructions.size();
//					for(Integer jj : outEdges.get(i))
//						if(i < endIdx)
//							endIdx = jj;
//					for(int j =i; j < endIdx; j++)
//					{
//						if(instructions.get(i) instanceof VarInsnNode)
//						{
//							VarInsnNode n = ((VarInsnNode) instructions.get(i));
//							b.varsAccessed.add(n.var);
//						}
//					}
//					cfg.put(i, b);
//				}
//				for(Integer i : cfg.keySet())
//				{
//					computeVarsAccessed(i,cfg);
//				}
				ArrayList<Integer> toAddNullBefore = new ArrayList<Integer>();
//				toAddNullBefore.addAll(insertACONSTNULLBEFORE);

				toAddNullBefore.addAll(insertACHECKCASTBEFORE);
				toAddNullBefore.addAll(neverAutoBoxByFrame.keySet());
				toAddNullBefore.addAll(alwaysAutoBoxByFrame.keySet());
				Collections.sort(toAddNullBefore);

				HashMap<LabelNode, LabelNode> problemLabels = new HashMap<LabelNode, LabelNode>();
				HashMap<LabelNode, HashSet<Integer>> problemVars = new HashMap<LabelNode, HashSet<Integer>>();
				int nNewNulls = 0;
				for (Integer i : toAddNullBefore) {
					AbstractInsnNode insertAfter = this.instructions.get(i + nNewNulls);

					if (insertACONSTNULLBEFORE.contains(i)) {
//						if (DEBUG)
//							System.out.println("Adding Null before: " + i);
//						if (insertAfter.getOpcode() == Opcodes.GOTO)
//							insertAfter = insertAfter.getPrevious();
//						this.instructions.insert(insertAfter, new InsnNode(Opcodes.ACONST_NULL));
//						nNewNulls++;
					} else if (insertACHECKCASTBEFORE.contains(i)) {
						if (DEBUG)
							System.out.println("Adding checkcast before: " + i + " (plus " + nNewNulls + ")");
						if (insertAfter.getOpcode() == Opcodes.GOTO)
							insertAfter = insertAfter.getPrevious();
						this.instructions.insert(insertAfter, new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Object.class)));
						nNewNulls++;
					} else if (neverAutoBoxByFrame.containsKey(i)) {
						if (insertAfter.getOpcode() == Opcodes.GOTO)
							insertAfter = insertAfter.getPrevious();
						for (int j : neverAutoBoxByFrame.get(i)) {
//							System.out.println("Adding nevefbox: before " + i + " (plus " + nNewNulls + ")");

							this.instructions.insert(insertAfter, new VarInsnNode(TaintUtils.NEVER_AUTOBOX, j));
							nNewNulls++;
						}
					} else if (alwaysAutoBoxByFrame.containsKey(i)) {
						for (int j : alwaysAutoBoxByFrame.get(i)) {
//							System.out.println("Adding checkcast always: before " + i + " (plus " + nNewNulls + ")");
//								while(insertAfter.getType() == AbstractInsnNode.LABEL ||
//										insertAfter.getType() == AbstractInsnNode.LINE||
//										insertAfter.getType() == AbstractInsnNode.FRAME)
//									insertAfter = insertAfter.getNext();
							AbstractInsnNode query = insertAfter.getNext();
							while(query.getNext() != null && (query.getType() == AbstractInsnNode.LABEL ||
									query.getType() == AbstractInsnNode.LINE ||
									query.getType() == AbstractInsnNode.FRAME || query.getOpcode() > 200))
								query = query.getNext();
							if(query.getOpcode() == Opcodes.ALOAD && query.getNext().getOpcode() == Opcodes.MONITOREXIT)
								insertAfter = query.getNext();
							if(query.getType() == AbstractInsnNode.JUMP_INSN)
								insertAfter = query;
							if(insertAfter.getType() == AbstractInsnNode.JUMP_INSN)
							{
								insertAfter = insertAfter.getPrevious();
//								System.out.println(Printer.OPCODES[insertAfter.getNext().getOpcode()]);
//								System.out.println("insertbefore  : " + ((JumpInsnNode) insertAfter.getNext()).toString());
								if(insertAfter.getNext().getOpcode() != Opcodes.GOTO)
								{
									this.instructions.insert(insertAfter, new VarInsnNode(TaintUtils.ALWAYS_BOX_JUMP, j));
								}
								else
								{
//									System.out.println("box immediately");
									this.instructions.insert(insertAfter, new VarInsnNode(TaintUtils.ALWAYS_AUTOBOX, j));
								}
							}
							else
							{
								if(insertAfter.getOpcode() == Opcodes.ATHROW)
									this.instructions.insertBefore(query, new VarInsnNode(TaintUtils.ALWAYS_AUTOBOX, j));
								else
									this.instructions.insert(insertAfter, new VarInsnNode(TaintUtils.ALWAYS_AUTOBOX, j));
							}
							nNewNulls++;
						}
					}
				}

//				System.out.println(name+desc);
				//fix LVs for android (sigh)
//				for(LabelNode l : problemLabels.keySet())
//				{
//					System.out.println("Problem label: "+l);
//				}
				boolean hadChanges = true;
				while (hadChanges) {
					hadChanges = false;

					HashSet<LocalVariableNode> newLVNodes = new HashSet<LocalVariableNode>();
					if (this.localVariables != null) {
						for (Object _lv : this.localVariables) {
							LocalVariableNode lv = (LocalVariableNode) _lv;
							AbstractInsnNode toCheck = lv.start;
							LabelNode veryEnd = lv.end;
							while (toCheck != null && toCheck != lv.end) {
								if ((toCheck.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP || toCheck.getOpcode() ==TaintUtils.ALWAYS_AUTOBOX) && ((VarInsnNode) toCheck).var == lv.index) {
//									System.out.println("LV " + lv.name + " will be a prob around " + toCheck);
									LabelNode beforeProblem = new LabelNode(new Label());
									LabelNode afterProblem = new LabelNode(new Label());
									this.instructions.insertBefore(toCheck, beforeProblem);
									this.instructions.insert(toCheck.getNext(), afterProblem);
									LocalVariableNode newLV = new LocalVariableNode(lv.name, lv.desc, lv.signature, afterProblem, veryEnd, lv.index);
									lv.end = beforeProblem;
									newLVNodes.add(newLV);
									hadChanges = true;
									break;
								}
								toCheck = toCheck.getNext();
							}
						}
						this.localVariables.addAll(newLVNodes);
					}
				}

			} catch (Throwable e) {
				System.err.println("While analyzing " + className);
				e.printStackTrace();
			}
			if (Configuration.ANNOTATE_LOOPS) {
				GraphBasedAnalyzer.doGraphAnalysis(this, implicitAnalysisblocks);
			}

			if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
				boolean hasJumps = false;
				HashSet<BasicBlock> tryCatchHandlers = new HashSet<>();
				if(shouldTrackExceptions && nTryCatch > 0)
				{
					int exceptionHandlerCount = 1;
					hasJumps = true;
					for (Object o : tryCatchBlocks) {
						TryCatchBlockNode t = (TryCatchBlockNode) o;
						BasicBlock startBlock = null;
						BasicBlock handlerBlock = null;
						BasicBlock endBlock = null;
						Integer startKey = null;
						Integer endKey = null;


						for(Entry<Integer,BasicBlock> e : implicitAnalysisblocks.entrySet()){
							BasicBlock b =e.getValue();
							Integer i = e.getKey();
							if(b.insn == t.handler){
								handlerBlock = b;
							}
							if(b.insn == t.start)
							{
								startBlock = b;
								startKey = i;
							}
							if(b.insn == t.end) {
								endBlock = b;
								endKey = i;
							}
							if(startBlock != null && handlerBlock != null && endBlock != null)
								break;
						}
						if(startBlock == handlerBlock || endBlock == null)
							continue;

						//Identify all of the instructions in this try block
						if(startBlock != null && endBlock != null)
						{
							for(int i =startKey; i<= endKey; i++){
								if(implicitAnalysisblocks.get(i) != null)
								implicitAnalysisblocks.get(i).coveredByTryBlockFor.add(t.type);
							}
						}
						handlerBlock.exceptionsHandled.add(t.type);
						tryCatchHandlers.add(handlerBlock);
						startBlock.isTryBlockStart = true;
						startBlock.exceptionsHandled.add(t.type);
						handlerBlock.onFalseSideOfJumpFrom.add(startBlock);
						handlerBlock.handlerForRegionStartingAt.add(startBlock);
						startBlock.successors.add(handlerBlock);
						startBlock.ex_count = exceptionHandlerCount;
						startBlock.tryBlockEnd = endBlock;
						startBlock.handledAt = handlerBlock;


						exceptionHandlerCount++;
						for(BasicBlock suc : startBlock.successors)
						{
							if(!suc.onFalseSideOfJumpFrom.contains(startBlock))
								suc.onTrueSideOfJumpFrom.add(startBlock);
						}
//						handlerBlock.onTrueSideOfJumpFrom.remove(startBlock);
//						System.out.println(handlerBlock + " " + handlerBlock.onTrueSideOfJumpFrom + " " + handlerBlock.onFalseSideOfJumpFrom);
					}
//					debug(instructions.getFirst());

				}

				for(BasicBlock b : implicitAnalysisblocks.values())
					if(b.isJump)
					{
						hasJumps = true;
						break;
					}
				if (implicitAnalysisblocks.size() > 1 && hasJumps) {
					Stack<BasicBlock> stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();

					//Fix successors to only point to jumps or labels
					boolean changed = true;
					while (changed) {
						changed = false;
						for (BasicBlock b : implicitAnalysisblocks.values()) {
								for (BasicBlock s : b.successors) {
									if (s.isInteresting()){
										changed |= b.successorsCompact.add(s);
									}
									else
									{
										changed |= b.successorsCompact.addAll(s.successorsCompact);
									}
								}
						}
					}
					//Post dominator analysis
					for(BasicBlock b : implicitAnalysisblocks.values())
						b.postDominators.add(b);
					changed = true;
					while(changed)
					{
						changed = false;
						for(BasicBlock b : implicitAnalysisblocks.values())
						{
							if(b.successorsCompact.size() > 0 && b.isInteresting())
							{
//								System.out.println(b.idx +  " " + b.postDominators + " ... " + b.successorsCompact);
								HashSet<BasicBlock> intersectionOfPredecessors = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
								Iterator<BasicBlock> iter = b.successorsCompact.iterator();
								BasicBlock successor = iter.next();
								intersectionOfPredecessors.addAll(successor.postDominators);
									while (iter.hasNext()) {
										BasicBlock lastSuc = successor;
										successor = iter.next();
										if (lastSuc.postDominators.contains(b)) {
											intersectionOfPredecessors.clear();
											intersectionOfPredecessors.addAll(successor.postDominators);
										}
										else if (successor.postDominators.contains(b)) {
											// = successor.postDominators

										} else
											intersectionOfPredecessors.retainAll(successor.postDominators);
									}
								changed |= b.postDominators.addAll(intersectionOfPredecessors);
							}
						}
					}


					//Add in markings for where jumps are resolved
					for(BasicBlock j : implicitAnalysisblocks.values())
					{
						if(j.isJump || j.isTryBlockStart)
						{
//							System.out.println(j + " " +j.postDominators);
							j.postDominators.remove(j);
							BasicBlock min = null;
							//Do a traversal of ALL of our successors in order to find the closest postDominator
							stack.add(j);
							HashMap<BasicBlock,Integer> distances = new HashMap<>();
							distances.put(j,0);
							HashSet<BasicBlock> visited = new HashSet<>();
							while (!stack.isEmpty()) {
								BasicBlock b = stack.pop();
								if (!visited.add(b))
									continue;
								int myDist = distances.get(b);
								for (BasicBlock s : b.successors) {
									Integer cur = distances.get(s);
									if (cur == null)
										distances.put(s, myDist + 1);
									else {
										if (myDist + 1 < cur) {
											distances.put(s, myDist + 1);
											visited.remove(s);
										}
									}
									stack.add(s);
								}
							}
							for(BasicBlock d : j.postDominators)
							{
								if(min == null || distances.get(min) > distances.get(d))//Might be a back edge outside of a loop though!
									min = d;
							}
//							System.out.println(j + " resolved at " + min +", of " + j.postDominators);
							if (min != null) {
								min.resolvedBlocks.add(j);
								min.resolvedHereBlocks.add(j);
							}
							else
							{
								//There are no post-dominators of this branch. That means that one leg of the
								//branch goes to a return. So, we'll say that this gets resolved at each return that is a successor
								for(BasicBlock b : visited)
								{
									if(b.insn.getOpcode() >= Opcodes.IRETURN && b.insn.getOpcode() < Opcodes.RETURN){
										b.resolvedHereBlocks.add(j);
									}
								}
							}
						}
					}

//					for(BasicBlock j : implicitAnalysisblocks.values())
//					{
//						this.instructions.insertBefore(j.insn, new LdcInsnNode(j.idx + " " + j.onTrueSideOfJumpFrom + " " + j.onFalseSideOfJumpFrom +" RH:" + j.resolvedHereBlocks + j.successorsCompact +" "+j.exceptionsThrown));
//						this.instructions.insertBefore(j.insn,new InsnNode(Opcodes.POP));
//					}
					//Propogate forward true-side/false-side to determine which vars are written
					stack.add(implicitAnalysisblocks.get(0));
//					stack.addAll(tryCatchHandlers);
					while (!stack.isEmpty()) {
						BasicBlock b = stack.pop();
						if (b.visited)
							continue;
						b.visited = true;

//						System.out.println("\t"+b.onFalseSideOfJumpFrom+b.onTrueSideOfJumpFrom+ b.resolvedHereBlocks);
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						b.onTrueSideOfJumpFrom.removeAll(b.resolvedBlocks);
						//Propogate markings to successors
						for (BasicBlock s : b.successors) {
							boolean _changed = false;

							_changed |= s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
							_changed |= s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
//							if(!s.visited)
								_changed |= s.resolvedBlocks.addAll(b.resolvedBlocks);
//							if(_changed)
//								s.visited = false;
							s.onFalseSideOfJumpFrom.remove(s);
							s.onTrueSideOfJumpFrom.remove(s);
//							if (!s.visited)
								stack.add(s);
						}
					}

//					for(BasicBlock j : implicitAnalysisblocks.values())
//					{
//						System.out.println(j.idx +  " " + j.onTrueSideOfJumpFrom + " "+j.onFalseSideOfJumpFrom);
//						this.instructions.insertBefore(j.insn, new LdcInsnNode(j.idx + " " + j.onTrueSideOfJumpFrom + " " + j.onFalseSideOfJumpFrom +" SUC:" + j.successors));
//						this.instructions.insertBefore(j.insn,new InsnNode(Opcodes.POP));
//					}
					for(BasicBlock j : implicitAnalysisblocks.values())
						j.visited = false;


					for(BasicBlock j : implicitAnalysisblocks.values())
					{
//						System.out.println(j.idx + " " + j.postDominators);
						if(j.isJump || j.isTryBlockStart)
						{
							stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();
							stack.addAll(j.successors);
							HashSet<BasicBlock> visited = new HashSet<>();
//							System.out.println("START JUMP " + j.idx);
							while(!stack.isEmpty())
							{
								BasicBlock b = stack.pop();
								if(!visited.add(b))
									continue;
//								System.out.println(b.idx);
//								System.out.println(b.exceptionsThrown.toString() + b.onFalseSideOfJumpFrom + " "+b.onTrueSideOfJumpFrom);
//								System.out.println(b.fieldsWritten);
//								System.out.println(b.varsWritten);
								if(b.onTrueSideOfJumpFrom.contains(j))
								{
									j.varsWrittenTrueSide.addAll(b.varsWritten);
									j.fieldsWrittenTrueSide.addAll(b.fieldsWritten);
									j.exceptionsThrownTrueSide.addAll(b.exceptionsThrown);
									stack.addAll(b.successors);
								}
								else if(b.onFalseSideOfJumpFrom.contains(j))
								{
									j.varsWrittenFalseSide.addAll(b.varsWritten);
									j.fieldsWrittenFalseSide.addAll(b.fieldsWritten);
									j.exceptionsThrownFalseSide.addAll(b.exceptionsThrown);
									stack.addAll(b.successors);
								}
							}
//							System.out.println("Visited: " + visited);
						}
					}
					HashMap<BasicBlock, Integer> jumpIDs = new HashMap<PrimitiveArrayAnalyzer.BasicBlock, Integer>();
					int jumpID = 0;
					for (BasicBlock r : implicitAnalysisblocks.values()) {
						if(r.isTryBlockStart){
							//Need to actually insert this code at every exit from the SCC that is this try-catch block.

							//Find the end of the handler
							//this is any block that succeeds the handler and either: has no successors or has a successor in common with the start block
							LinkedList<BasicBlock> handlerEndBlock = new LinkedList<>();
							LinkedList<BasicBlock> toCheck = new LinkedList<>(r.handledAt.successors);
							HashSet<BasicBlock> visited = new HashSet<>();
							while(!toCheck.isEmpty()){
								BasicBlock e = toCheck.pop();
								if(!visited.add(e))
									continue;
								if(e.successors.size() == 0)
								{
									handlerEndBlock.add(e);
								}
								else if(r.postDominators.contains(e))
								{
									handlerEndBlock.add(e);
								}
								else
								{
									toCheck.addAll(e.successors);
								}
							}

							AbstractInsnNode lastInstructionInTryBlock = r.tryBlockEnd.insn;
							while (lastInstructionInTryBlock.getType() == AbstractInsnNode.FRAME || lastInstructionInTryBlock.getType() == AbstractInsnNode.LINE || lastInstructionInTryBlock.getType() == AbstractInsnNode.LABEL)
								lastInstructionInTryBlock = lastInstructionInTryBlock.getNext();


							//Set up the force control store's at the bottom of the try block
							HashSet<LVAccess> lvsOnlyInHandler = new HashSet<>(r.varsWrittenFalseSide);
							lvsOnlyInHandler.removeAll(r.varsWrittenTrueSide);
							HashSet<Field> fieldsOnlyInHandler = new HashSet<>(r.fieldsWrittenFalseSide);
							fieldsOnlyInHandler.removeAll(r.fieldsWrittenTrueSide);
							for (LVAccess i : lvsOnlyInHandler)
								this.instructions.insertBefore(lastInstructionInTryBlock, i.getNewForceCtrlStoreNode());
							for (Field f : fieldsOnlyInHandler)
								this.instructions.insertBefore(lastInstructionInTryBlock, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));


							AbstractInsnNode handledAtInsn = r.handledAt.insn;
							HashSet<String> handledHereAlready = new HashSet<>();
							HashSet<Integer> forceStoreAlready = new HashSet<>();
							while (handledAtInsn.getType() == AbstractInsnNode.FRAME || handledAtInsn.getType() == AbstractInsnNode.LINE || handledAtInsn.getType() == AbstractInsnNode.LABEL || handledAtInsn.getOpcode() > 200) {
								if(handledAtInsn.getOpcode() == TaintUtils.EXCEPTION_HANDLER_START){
									TypeInsnNode tin = (TypeInsnNode) handledAtInsn;
									if(tin.desc != null)
										handledHereAlready.add(tin.desc);
								}
								else if(handledAtInsn.getOpcode() == TaintUtils.FORCE_CTRL_STORE && handledAtInsn.getType() == AbstractInsnNode.VAR_INSN){
									VarInsnNode vn = (VarInsnNode) handledAtInsn;
									forceStoreAlready.add(vn.var);
								}
								handledAtInsn = handledAtInsn.getNext();
							}

							//Then do all of the force-ctr-stores
							//In the exception handler, force a store of what was written
							HashSet<LVAccess> diff = new HashSet<LVAccess>();

							diff.addAll(r.varsWrittenTrueSide);
							diff.removeAll(r.varsWrittenFalseSide);

							HashSet<Field> diffFields = new HashSet<>();
							diffFields.addAll(r.fieldsWrittenTrueSide);
							diffFields.removeAll(r.fieldsWrittenFalseSide);


							for (LVAccess i : diff)
								if(!forceStoreAlready.contains(i.idx))
									instructions.insertBefore(handledAtInsn, i.getNewForceCtrlStoreNode());
							for (Field f : diffFields)
								instructions.insertBefore(handledAtInsn, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));

							//At the START of the handler, note that it's the start...
							if(handledHereAlready.size() == 0)
								instructions.insertBefore(handledAtInsn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_START, null));
							for (String ex : r.exceptionsHandled) {
								if (ex == null)
									ex = "java/lang/Throwable";
								if(!handledHereAlready.contains(ex))
									instructions.insertBefore(handledAtInsn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_START, ex));
								this.instructions.insertBefore(lastInstructionInTryBlock,new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_END,ex));
							}

							//At the END of the handler, remove this exception from the queue
							for(BasicBlock b : handlerEndBlock) {
								AbstractInsnNode insn = b.insn;
//								while (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL)
//									insn = insn.getNext();
								//Peek backwards to see if we are behind a GOTO
								while(insn != null && insn.getPrevious() != null && mightEndBlock(insn.getPrevious())) {
									insn = insn.getPrevious();
								}
								if(insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.FRAME)
									insn = b.insn;
//								System.out.println(b +"," + insn);


								instructions.insertBefore(insn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_END, null));
							}

//							debug(instructions.getFirst());
						}
						else if (r.isJump) {
							jumpID++;

							HashSet<LVAccess> common = new HashSet<LVAccess>();
							common.addAll(r.varsWrittenFalseSide);
							common.retainAll(r.varsWrittenTrueSide);
							HashSet<LVAccess> diff =new HashSet<LVAccess>();
							diff.addAll(r.varsWrittenTrueSide);
							diff.addAll(r.varsWrittenFalseSide);
							diff.removeAll(common);

							HashSet<Field> commonFields = new HashSet<>();
							commonFields.addAll(r.fieldsWrittenTrueSide);
							commonFields.retainAll(r.fieldsWrittenFalseSide);
							HashSet<Field> diffFields = new HashSet<>();
							diffFields.addAll(r.fieldsWrittenFalseSide);
							diffFields.addAll(r.fieldsWrittenTrueSide);
							diffFields.removeAll(common);


							HashSet<String> commonExceptionsThrown = new HashSet<>();
							commonExceptionsThrown.addAll(r.exceptionsThrownFalseSide);
							commonExceptionsThrown.retainAll(r.exceptionsThrownTrueSide);
							HashSet<String> diffExceptions = new HashSet<>();
							diffExceptions.addAll(r.exceptionsThrownTrueSide);
							diffExceptions.addAll(r.exceptionsThrownFalseSide);
							diffExceptions.removeAll(commonExceptionsThrown);


//							System.out.println(b.idx + " " + b.varsWrittenFalseSide +" "+b.varsWrittenTrueSide + " " + b.exceptionsThrownFalseSide +  b.exceptionsThrownTrueSide);

							instructions.insertBefore(r.insn, new VarInsnNode(TaintUtils.BRANCH_START, jumpID));
							jumpIDs.put(r, jumpID);
							if (r.is2ArgJump)
								jumpID++;

							for (LVAccess i : diff)
								instructions.insertBefore(r.insn, i.getNewForceCtrlStoreNode());
							for (Field f : diffFields)
								instructions.insertBefore(r.insn, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));
//							for (String s : diffExceptions)
//								instructions.insertBefore(r.insn, new TypeInsnNode(TaintUtils.FORCE_CTRL_STORE, s));



						}
						else if(shouldTrackExceptions && r.insn.getOpcode() >= Opcodes.IRETURN && r.insn.getOpcode() <= Opcodes.RETURN){
							//Return statement: check to see how we might have gotten here, and then find which exceptions we might have thrown if we came otherwise
							HashSet<String> missedExceptions = new HashSet<>();
							for(BasicBlock b : r.onFalseSideOfJumpFrom)
							{
								HashSet<String> tmp = new HashSet(b.exceptionsThrownTrueSide);
								tmp.removeAll(b.exceptionsThrownFalseSide);
								missedExceptions.addAll(tmp);
							}
							for(BasicBlock b : r.onTrueSideOfJumpFrom)
							{
								HashSet<String> tmp = new HashSet(b.exceptionsThrownFalseSide);
								tmp.removeAll(b.exceptionsThrownTrueSide);
								missedExceptions.addAll(tmp);
							}
							HashSet<String> filtered =new HashSet<>();
//							System.out.println(name + ":"+r.idx+" " + missedExceptions);
							for(String s : missedExceptions){
								if(s == null)
									s = "java/lang/Throwable";
								if(s.contains("#"))
									s = s.substring(0,s.indexOf('#'));
								if(filtered.add(s))
									instructions.insertBefore(r.insn, new TypeInsnNode(TaintUtils.UNTHROWN_EXCEPTION, s));
							}
						}
						else if(shouldTrackExceptions && (r.insn.getType() == AbstractInsnNode.METHOD_INSN || r.insn
						.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN)){
							//Are we in a try handler? If so, after this instruction, we should note that our execution may be contingent on some exception
							for(String s : r.coveredByTryBlockFor)
							{
								if(s == null)
									s = "java/lang/Throwable";
								instructions.insert(r.insn,new TypeInsnNode(TaintUtils.UNTHROWN_EXCEPTION_CHECK, s));
							}
						}
					}


					for (BasicBlock b : implicitAnalysisblocks.values()) {
//						System.out.println(b.idx + " -> " + b.successorsCompact);
//						System.out.println(b.successors);
//						System.out.println(b.resolvedBlocks);
						AbstractInsnNode insn = b.insn;
						while (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL)
							insn = insn.getNext();

						if(b.resolvedHereBlocks.size() == jumpIDs.size())
						{
							//Everything is resolved
							instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, -1));
						} else
							for (BasicBlock r : b.resolvedHereBlocks) {
//							System.out.println("Resolved: " + jumpIDs.get(r) + " at " + b.idx);
								//								System.out.println("GOt" + jumpIDs);
								if (r.isTryBlockStart) {

								} else {
									if (b.successors.size() > 0) {
										//for any return/athrow, we'll just bulk pop-all
										instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
										if (r.is2ArgJump)
											instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r) + 1));
									}
								}
							}
						if (b.resolvedHereBlocks.size() > 0) {
							insn = b.insn;
							while (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL || insn.getOpcode() > 200)
								insn = insn.getNext();
							if (insn.getOpcode() == Opcodes.NEW) //maybe its a NEW
							{
								//Need to patch all frames to have the correct label in them :'(
								AbstractInsnNode i = insn;
								while (i != null && i.getType() != AbstractInsnNode.LABEL)
									i = i.getPrevious();

								LinkedList<LabelNode> oldLabels = new LinkedList<>();
								oldLabels.add(((LabelNode)i));
								if(i.getPrevious() != null && i.getPrevious().getType() == AbstractInsnNode.LABEL)
									oldLabels.add(((LabelNode) i.getPrevious()));

								LabelNode newLabel = new LabelNode(new Label());
								instructions.insertBefore(insn, newLabel);
								i = instructions.getFirst();
								while (i != null) {
									if (i instanceof FrameNode) {
										FrameNode fr = (FrameNode) i;
										for (int j = 0; j < fr.stack.size(); j++) {
											if(oldLabels.contains(fr.stack.get(j))){
												fr.stack.set(j, newLabel.getLabel());
											}
										}
									}
									i = i.getNext();
								}
							}
						}
						if(b.successors.isEmpty() && !Configuration.IMPLICIT_LIGHT_TRACKING) //in light tracking mode no need to POP off of control at RETURN/THROW, becasue we don't reuse the obj
						{
							instructions.insertBefore(b.insn, new InsnNode(TaintUtils.FORCE_CTRL_STORE));
//							if (b.insn.getOpcode() != Opcodes.ATHROW) {
							instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.BRANCH_END, -1));
//							}
						}
						//						System.out.println(b.insn + " - " + b.domBlocks + "-" + b.antiDomBlocks);
					}
					nJumps = jumpID;
				}

			}

			this.maxStack += 100;

			AbstractInsnNode insn = instructions.getFirst();
			while(insn != null)
			{
				if(insn.getType() == AbstractInsnNode.FRAME)
				{
					//Insert a note before the instruction before this guy
					AbstractInsnNode insertBefore = insn;
					while (insertBefore != null && (insertBefore.getType() == AbstractInsnNode.FRAME || insertBefore.getType() == AbstractInsnNode.LINE
							|| insertBefore.getType() == AbstractInsnNode.LABEL))
						insertBefore = insertBefore.getPrevious();
					if (insertBefore != null)
						this.instructions.insertBefore(insertBefore, new InsnNode(TaintUtils.FOLLOWED_BY_FRAME));
				}
				insn = insn.getNext();
			}
			this.accept(cmv);
		}
		HashMap<Integer,BasicBlock> implicitAnalysisblocks = new HashMap<Integer,PrimitiveArrayAnalyzer.BasicBlock>();

	}

	private boolean mightEndBlock(AbstractInsnNode insn) {
		if(insn.getType() == AbstractInsnNode.LABEL)
			return true;
		switch(insn.getOpcode()){
			case Opcodes.GOTO:
			case Opcodes.RETURN:
			case Opcodes.IRETURN:
			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
			case Opcodes.ARETURN:
			case Opcodes.ATHROW:
				return true;
		}
		return false;
	}

	static void debug(AbstractInsnNode insn){
		while(insn != null) {
			if (insn.getType() == AbstractInsnNode.LABEL)
				System.out.println("LABEL " + ((LabelNode)insn).getLabel());
			if (insn.getOpcode() > 0) {
				if (insn.getOpcode() > PhosphorTextifier.OPCODES.length) {
					if (insn.getType() == AbstractInsnNode.TYPE_INSN)
						System.out.println(PhosphorTextifier.TYPE_OR_INT_OPCODES[insn.getOpcode() - 200]);
					else
						System.out.println(PhosphorTextifier.MORE_OPCODES[insn.getOpcode() - 200]);
				} else
					System.out.println(PhosphorTextifier.OPCODES[insn.getOpcode()]);
			}
			insn=insn.getNext();
		}
	}
	static class BasicBlock{
		public BasicBlock handledAt;
		protected int idxOrder;
		public HashSet<BasicBlock> postDominators= new HashSet<PrimitiveArrayAnalyzer.BasicBlock>() ;
		int idx;
//		LinkedList<Integer> outEdges = new LinkedList<Integer>();
		HashSet<BasicBlock> successorsCompact = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> successors = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> predecessors  = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		AbstractInsnNode insn;
		boolean covered;
		boolean visited;
		boolean isTryBlockStart;
		boolean isJump;
		boolean is2ArgJump;
		int ex_count;
		HashSet<String> exceptionsHandled = new HashSet<>();
		HashSet<String> coveredByTryBlockFor = new HashSet<>();
		HashSet<BasicBlock> handlerForRegionStartingAt = new HashSet<>();
		BasicBlock tryBlockEnd;

		HashSet<BasicBlock> resolvedHereBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();

		private boolean compactSuccessorsCalculated;
		public boolean isInteresting()
		{
			return isJump || isTryBlockStart || insn instanceof LabelNode;
		}

		HashSet<BasicBlock> resolvedBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onFalseSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onTrueSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<LVAccess> varsWritten = new HashSet<LVAccess>();
		HashSet<Field> fieldsWritten = new HashSet<>();
		HashSet<String> exceptionsThrown = new HashSet<>();
		HashSet<String> exceptionsThrownTrueSide = new HashSet<>();
		HashSet<String> exceptionsThrownFalseSide = new HashSet<>();

		HashSet<LVAccess> varsWrittenTrueSide = new HashSet<LVAccess>();
		HashSet<LVAccess> varsWrittenFalseSide = new HashSet<LVAccess>();
		HashSet<Field> fieldsWrittenTrueSide = new HashSet<>();
		HashSet<Field> fieldsWrittenFalseSide = new HashSet<>();

		public Set<BasicBlock> getSuccessorsOutsideOfRegion(int s, int e, Set<BasicBlock> visited){
			if(visited.contains(this))
				return Collections.emptySet();
			visited.add(this);
			if(idx < s || idx > e)
				return Collections.singleton(this);
			Set<BasicBlock> ret = new HashSet<>();
			for(BasicBlock suc : successors)
			{
				ret.addAll(suc.getSuccessorsOutsideOfRegion(s,e,visited));
			}
			return ret;

		}
		public AbstractInsnNode getNextNormalBlockAfterGOTO() {
			System.out.println(this + "," + successors+ " " + this.insn);
			if(this.insn instanceof LineNumberNode)
				System.out.println("("+((LineNumberNode)insn).line+")");
			if(this.insn.getOpcode() == Opcodes.GOTO){
				BasicBlock suc = successors.iterator().next();
				AbstractInsnNode insn = suc.insn;
				while (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL)
					insn = insn.getNext();
				return insn;
			}
			if(successors.size() > 1)
				throw new IllegalStateException();
			return successors.iterator().next().getNextNormalBlockAfterGOTO();
		}

		@Override
		public String toString() {
//			return insn.toString();
			return ""+idx;
		}
	}
	private static boolean isPrimitiveArrayType(BasicValue v) {
		if (v == null || v.getType() == null)
			return false;
		return v.getType().getSort() == Type.ARRAY && v.getType().getElementType().getSort() != Type.OBJECT;
	}

	static final boolean DEBUG = false;
	public HashSet<Type> wrapperTypesToPreAlloc = new HashSet<Type>();
	public int nJumps;
	@Override
	public void visitInsn(int opcode) {
		super.visitInsn(opcode);
		switch (opcode) {
		case Opcodes.FADD:
		case Opcodes.FREM:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
			if(Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("F"));
			break;
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("D"));
			break;
		case Opcodes.LSHL:
		case Opcodes.LUSHR:
		case Opcodes.LSHR:
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LADD:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("J"));
			break;
		case Opcodes.LCMP:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
		case Opcodes.FCMPG:
		case Opcodes.FCMPL:
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
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("I"));
			break;
		case Opcodes.IALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("I"));
			break;
		case Opcodes.BALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("B"));
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("Z"));
			break;
		case Opcodes.CALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("C"));
			break;
		case Opcodes.DALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("D"));
			break;
		case Opcodes.LALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("J"));
			break;
		case Opcodes.FALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("F"));
			break;
		case Opcodes.SALOAD:
			wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("S"));
			break;
		case Opcodes.I2C:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("C"));
			break;
		case Opcodes.I2B:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("B"));
			break;
		case Opcodes.I2D:
		case Opcodes.F2D:
		case Opcodes.L2D:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("D"));
			break;
		case Opcodes.I2F:
		case Opcodes.L2F:
		case Opcodes.D2F:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("F"));
			break;
		case Opcodes.I2L:
		case Opcodes.F2L:
		case Opcodes.D2L:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("J"));
			break;
		case Opcodes.I2S:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("S"));
			break;
		case Opcodes.F2I:
		case Opcodes.L2I:
		case Opcodes.D2I:
			if (Configuration.PREALLOC_STACK_OPS)
				wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("I"));
			break;
		case Opcodes.ATHROW:
			nThrow++;
			break;
		}
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		super.visitMethodInsn(opcode, owner, name, desc,itfc);
		Type returnType = Type.getReturnType(desc);
		Type newReturnType = TaintUtils.getContainerReturnType(returnType);
		isEmptyMethod = false;
		if(newReturnType != returnType && !(returnType.getSort() == Type.ARRAY))
			wrapperTypesToPreAlloc.add(newReturnType);
	}

	public MethodNode mn;
	public PrimitiveArrayAnalyzer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5);
		this.mn = new PrimitiveArrayAnalyzerMN(access, name, desc, signature, exceptions, className, cmv);
		this.mv = mn;
	}

	public PrimitiveArrayAnalyzer(Type singleWrapperTypeToAdd) {
		super(Opcodes.ASM5);
		this.mv = new PrimitiveArrayAnalyzerMN(0, null,null,null,null,null, null);
		if(singleWrapperTypeToAdd.getSort() == Type.OBJECT && singleWrapperTypeToAdd.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/Tainted"))
			this.wrapperTypesToPreAlloc.add(singleWrapperTypeToAdd);
	}

	NeverNullArgAnalyzerAdapter analyzer;

	public void setAnalyzer(NeverNullArgAnalyzerAdapter preAnalyzer) {
		analyzer = preAnalyzer;
	}

	public boolean hasFinally;
	public int nTryCatch;
	public int nThrow;
	HashMap<Label, LinkedList<Label>> newLabels = new HashMap<>();
	private Label addUniqueLabelFor(Label existing)
	{
		Label ret = new Label();
		if(!newLabels.containsKey(existing))
			newLabels.put(existing,new LinkedList<Label>());
		newLabels.get(existing).add(ret);
		return ret;
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);
		LinkedList<Label> lbls = this.newLabels.remove(label);
		if(lbls != null){
			for(Label l : lbls)
			super.visitLabel(l);
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

		super.visitTryCatchBlock(addUniqueLabelFor(start),
				end,
//				addUniqueLabelFor(end),
				handler, type);
		if (type == null)
			hasFinally = true;
		nTryCatch++;
	}
	static class LVAccess{
		int idx;
		String desc;

		@Override
		public String toString() {
			return "LVAccess{" +
					"idx=" + idx +
					", desc='" + desc + '\'' +
					'}';
		}

		public LVAccess(int idx, String desc) {
			this.idx = idx;
			this.desc = desc;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LVAccess lvAccess = (LVAccess) o;
			return idx == lvAccess.idx &&
					Objects.equals(desc, lvAccess.desc);
		}

		@Override
		public int hashCode() {

			return Objects.hash(idx, desc);
		}

		public VarInsnNode getNewForceCtrlStoreNode() {
			if(this.desc.equals("J") || this.desc.equals("D"))
				return new VarInsnNode(TaintUtils.FORCE_CTRL_STORE_WIDE,idx);
			return new VarInsnNode(TaintUtils.FORCE_CTRL_STORE,idx);
		}
	}
}
