package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;

public class PrimitiveArrayAnalyzer extends MethodVisitor {
	final class PrimitiveArrayAnalyzerMN extends MethodNode {
		private final String className;
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
			Analyzer a = new Analyzer(new BasicArrayInterpreter()) {
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

				@Override
				public Frame[] analyze(String owner, MethodNode m) throws AnalyzerException {
					Iterator<AbstractInsnNode> insns = m.instructions.iterator();
					insnToLabel = new int[m.instructions.size()];
					endsWithGOTO = new boolean[insnToLabel.length];

//											System.out.println("PAAA"+ name);
					int label = -1;
					boolean isFirst = true;
					while (insns.hasNext()) {
						AbstractInsnNode insn = insns.next();
						int idx = m.instructions.indexOf(insn);

						if (insn instanceof LabelNode) {
							label++;
						}

						if (insn.getOpcode() == Opcodes.GOTO) {
							endsWithGOTO[idx] = true;
						}
						insnToLabel[idx] = (isFirst ? 1 : label);
						isFirst = false;
						//														System.out.println(idx + "->"+label);
					}
					Frame[] ret = super.analyze(owner, m);
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
								int labelToMerge = getLabel(toMerge);
								if (DEBUG)
									System.out.println(toMerge + " AKA " + labelToMerge);
								if (DEBUG)
									System.out.println((outFrames.get(labelToMerge) == null ? "null" : outFrames.get(labelToMerge).stack));
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
				protected void newControlFlowEdge(int insn, int successor) {
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
						fromBlock.idxOrder = insnIdxOrderVisited;
						insnIdxOrderVisited++;
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
						succesorBlock.idxOrder = insnIdxOrderVisited;
						insnIdxOrderVisited++;
						succesorBlock.insn = instructions.get(successor);
						implicitAnalysisblocks.put(successor, succesorBlock);
						if(succesorBlock.insn.getType() == AbstractInsnNode.IINC_INSN)
						{
							succesorBlock.varsWritten.add(((IincInsnNode)succesorBlock.insn).var);
						}
						else if(succesorBlock.insn.getType() == AbstractInsnNode.VAR_INSN)
						{
							switch(succesorBlock.insn.getOpcode())
							{
							case ISTORE:
							case ASTORE:
							case DSTORE:
							case LSTORE:
								succesorBlock.varsWritten.add(((VarInsnNode)succesorBlock.insn).var);
								break;
							}
						}
					}
					fromBlock.successors.add(succesorBlock);
					succesorBlock.predecessors.add(fromBlock);
					
					if(fromBlock.isJump)
						{
						if(fromBlock.covered)
							succesorBlock.onTrueSideOfJumpFrom.add(fromBlock);
						else
						{
							succesorBlock.onFalseSideOfJumpFrom.add(fromBlock);
							fromBlock.covered = true;
						}
						}
					super.newControlFlowEdge(insn, successor);
				}
			};
			try {

				Frame[] frames = a.analyze(className, this);
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
				
			} catch (AnalyzerException e) {
				e.printStackTrace();
			}

			if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
				boolean hasJumps = false;
				for(BasicBlock b : implicitAnalysisblocks.values())
					if(b.isJump)
					{
						hasJumps = true;
						break;
					}
				if (implicitAnalysisblocks.size() > 1 && hasJumps) {
					Stack<BasicBlock> stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();

					//Fix successors to only point to jumps or labels
					
					/*
					 * 		public HashSet<BasicBlock> calculateSuccessorsCompact() {
			if(compactSuccessorsCalculated)
				return successorsCompact;
			for (BasicBlock b : successors) {
				compactSuccessorsCalculated = true;
				if(b.isInteresting())
					successorsCompact.add(b);
				else
					successorsCompact.addAll(b.calculateSuccessorsCompact());
			}
			return successorsCompact;
		}
					 */
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
								HashSet<BasicBlock> intersectionOfPredecessors = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
								Iterator<BasicBlock> iter = b.successorsCompact.iterator();
								BasicBlock successor = iter.next();
								intersectionOfPredecessors.addAll(successor.postDominators);
								while(iter.hasNext())
								{
									successor = iter.next();
									intersectionOfPredecessors.retainAll(successor.postDominators);
								}
								changed |= b.postDominators.addAll(intersectionOfPredecessors);
							}
						}
					}
					
					//Add in markings for where jumps are resolved
					for(BasicBlock j : implicitAnalysisblocks.values())
					{
						if(j.isJump)
						{
//							System.out.println(j + " " +j.postDominators);
							j.postDominators.remove(j);
							BasicBlock min = null;
							for(BasicBlock d : j.postDominators)
							{
								if(min == null || min.idxOrder > d.idxOrder)
									min = d;
							}
//							System.out.println(j + " resolved at " + min);
							if (min != null) {
								min.resolvedBlocks.add(j);
								min.resolvedHereBlocks.add(j);
							}
						}
					}
					
					//Propogate forward true-side/false-side to determine which vars are written
					stack.add(implicitAnalysisblocks.get(0));
					while (!stack.isEmpty()) {
						BasicBlock b = stack.pop();
						if (b.visited)
							continue;
						b.visited = true;
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						b.onTrueSideOfJumpFrom.removeAll(b.resolvedBlocks);
						//Propogate markings to successors
						for (BasicBlock s : b.successors) {
							boolean _changed = false;
							_changed |= s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
							_changed |= s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
							_changed |= s.resolvedBlocks.addAll(b.resolvedBlocks);
							if(_changed)
								s.visited = false;
							s.onFalseSideOfJumpFrom.remove(s);
							s.onTrueSideOfJumpFrom.remove(s);
							if (!s.visited)
								stack.add(s);
						}
					}

					
					for(BasicBlock j : implicitAnalysisblocks.values())
					{
//						this.instructions.insertBefore(j.insn, new LdcInsnNode(j.idx + " " + j.onTrueSideOfJumpFrom + " " + j.onFalseSideOfJumpFrom));
//						System.out.println(j.idx + " " + j.postDominators);
						if(j.isJump)
						{
							stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();
							stack.addAll(j.successors);
							while(!stack.isEmpty())
							{
								BasicBlock b = stack.pop();
								if(b.visited)
									continue;
								b.visited = true;
								if(b.onFalseSideOfJumpFrom.contains(j))
								{
									j.varsWrittenTrueSide.addAll(b.varsWritten);
									stack.addAll(b.successors);
								}
								else if(b.onTrueSideOfJumpFrom.contains(j))
								{
									j.varsWrittenFalseSide.addAll(b.varsWritten);
									stack.addAll(b.successors);
								}
							}
						}
					}
					HashMap<BasicBlock, Integer> jumpIDs = new HashMap<PrimitiveArrayAnalyzer.BasicBlock, Integer>();
					int jumpID = 0;
					for (BasicBlock b : implicitAnalysisblocks.values()) {
						if (b.isJump) {
							jumpID++;
							HashSet<Integer> common = new HashSet<Integer>();
							common.addAll(b.varsWrittenFalseSide);
							common.retainAll(b.varsWrittenTrueSide);
							HashSet<Integer> diff =new HashSet<Integer>();
							diff.addAll(b.varsWrittenTrueSide);
							diff.addAll(b.varsWrittenFalseSide);
							diff.removeAll(common);

							for(int i : diff)
							{
								instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.FORCE_CTRL_STORE, i));
								
							}
							instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.BRANCH_START, jumpID));
							jumpIDs.put(b, jumpID);
							if(b.is2ArgJump)
								jumpID++;
						}
					}

					for (BasicBlock b : implicitAnalysisblocks.values()) {
//						System.out.println(b.idx + " -> " + b.successorsCompact);
//						System.out.println(b.successors);
//						System.out.println(b.resolvedBlocks);
						for (BasicBlock r : b.resolvedHereBlocks) {
//							System.out.println("Resolved: " + jumpIDs.get(r) + " at " + b.idx);
							//								System.out.println("GOt" + jumpIDs);
							AbstractInsnNode insn = b.insn;
							while (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL)
								insn = insn.getNext();
							instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
							if(r.is2ArgJump)
								instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)+1));
						}
						if(b.successors.isEmpty())
						{
							instructions.insertBefore(b.insn, new InsnNode(TaintUtils.FORCE_CTRL_STORE));
//							if (b.insn.getOpcode() != Opcodes.ATHROW) {
								HashSet<BasicBlock> live = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>(b.onFalseSideOfJumpFrom);
								live.addAll(b.onTrueSideOfJumpFrom);
								for (BasicBlock r : live) {
									instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
									if (r.is2ArgJump)
										instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r) + 1));
								}
//							}
						}
						//						System.out.println(b.insn + " - " + b.domBlocks + "-" + b.antiDomBlocks);
					}
					nJumps = jumpID;
				}

			}
//			System.out.println(name);
			if (Configuration.ANNOTATE_LOOPS) {
				SCCAnalyzer scc = new SCCAnalyzer();
				int max = 0;
				for(Integer i : implicitAnalysisblocks.keySet())
				{
					if(i > max)
						max = i;
				}
				BasicBlock[] flatGraph = new BasicBlock[max + 1];
				for(int i = 0; i < flatGraph.length; i++)
					flatGraph[i] = implicitAnalysisblocks.get(i);
				List<List<BasicBlock>> sccs = scc.scc(flatGraph);
				for (List<BasicBlock> c : sccs) {
					if (c.size() == 1)
						continue;
//					System.out.println(c);
					for (BasicBlock b : c) {
						if (b.successors.size() > 1)
							if (!c.containsAll(b.successors)) {
								// loop header
								this.instructions.insertBefore(b.insn, new InsnNode(TaintUtils.LOOP_HEADER));
							}
					}
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

		void calculatePostDominators(BasicBlock b)
		{
			if(b.visited)
				return;
			b.visited = true;
			b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
			b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
			//Propogate markings to successors
			for(BasicBlock s : b.successors)
			{
				s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
				s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
				s.resolvedBlocks.addAll(b.resolvedBlocks);
				if(!s.visited)
					calculatePostDominators(s);
			}
		}

	}
	static class BasicBlock{
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
		boolean isJump;
		boolean is2ArgJump;
		HashSet<BasicBlock> resolvedHereBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();

		private boolean compactSuccessorsCalculated;
		public boolean isInteresting()
		{
			return isJump || insn instanceof LabelNode;
		}

		HashSet<BasicBlock> resolvedBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onFalseSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onTrueSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<Integer> varsWritten = new HashSet<Integer>();
		
		HashSet<Integer> varsWrittenTrueSide = new HashSet<Integer>();
		HashSet<Integer> varsWrittenFalseSide = new HashSet<Integer>();
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
		}
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		super.visitMethodInsn(opcode, owner, name, desc,itfc);
		Type returnType = Type.getReturnType(desc);
		Type newReturnType = TaintUtils.getContainerReturnType(returnType);
		if(newReturnType != returnType && !(returnType.getSort() == Type.ARRAY))
			wrapperTypesToPreAlloc.add(newReturnType);
	}

	public PrimitiveArrayAnalyzer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5);
		this.mv = new PrimitiveArrayAnalyzerMN(access, name, desc, signature, exceptions, className, cmv);
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
}
