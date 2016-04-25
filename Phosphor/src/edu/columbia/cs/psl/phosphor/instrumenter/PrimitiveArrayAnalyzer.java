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

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class PrimitiveArrayAnalyzer extends MethodVisitor {
	final class PrimitiveArrayAnalyzerMN extends MethodNode {
		private final String className;
		private final MethodVisitor cmv;
		boolean[] endsWithGOTO;
		int curLabel = 0;
		HashMap<Integer, Boolean> lvsThatAreArrays = new HashMap<Integer, Boolean>();
		ArrayList<FrameNode> inFrames = new ArrayList<FrameNode>();
		ArrayList<FrameNode> outFrames = new ArrayList<FrameNode>();

		public PrimitiveArrayAnalyzerMN(int access, String name, String desc,
				String signature, String[] exceptions, String className,
				MethodVisitor cmv) {
			super(Opcodes.ASM5,access, name, desc, signature, exceptions);
			this.className = className;
			this.cmv = cmv;
		}

		@Override
		public void visitCode() {
			if (DEBUG)
				System.out.println("Visiting: " + className + "." + name + desc);
			Label firstLabel = new Label();
			super.visitCode();
			visitLabel(firstLabel);

		}

		private void visitFrameTypes(final int n, final Object[] types,
				final List<Object> result) {
			for (int i = 0; i < n; ++i) {
				Object type = types[i];
				result.add(type);
				if (type == Opcodes.LONG || type == Opcodes.DOUBLE) {
					result.add(Opcodes.TOP);
				}
			}
		}

		FrameNode generateFrameNode(int type, int nLocal, Object[] local,
				int nStack, Object[] stack) {
			FrameNode ret = new FrameNode(type, nLocal, local, nStack, stack);
			ret.local = new ArrayList<Object>();
			ret.stack= new ArrayList<Object>();
			visitFrameTypes(nLocal, local, ret.local);
			visitFrameTypes(nStack, stack, ret.stack);
			return ret;
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local,
				int nStack, Object[] stack) {
			if (DEBUG)
				System.out.println("Visitframe curlabel " + (curLabel - 1));

			super.visitFrame(type, nLocal, local, nStack, stack);
			
			if (DEBUG)
				System.out.println("label " + (curLabel - 1) + " reset to " + Arrays.toString(stack));
			if (inFrames.size() == curLabel - 1)
				inFrames.add(generateFrameNode(type, nLocal, local, nStack, stack));
			else
				inFrames.set(curLabel - 1, generateFrameNode(type, nLocal, local, nStack, stack));
		}

		@Override
		public void visitLabel(Label label) {
			if (DEBUG)
				System.out.println("Visit label: " + curLabel
						+ " analyzer: " + analyzer.stack
						+ " inframes size " + inFrames.size()
						+ " " + outFrames.size());

			if (analyzer.locals == null || analyzer.stack == null)
				inFrames.add(new FrameNode(0, 0, new Object[0], 0, new Object[0]));
			else
				inFrames.add(new FrameNode(0, analyzer.locals.size(),
							analyzer.locals.toArray(),
							analyzer.stack.size(),
							analyzer.stack.toArray()));

			outFrames.add(null);

			if (curLabel > 0
					&& outFrames.get(curLabel - 1) == null
					&& analyzer.stack != null)
				outFrames.set(curLabel - 1,
						new FrameNode(0, analyzer.locals.size(),
										 analyzer.locals.toArray(),
										 analyzer.stack.size(),
										 analyzer.stack.toArray()));
			if (DEBUG)
				System.out.println("Added outframe for "
						+ (outFrames.size() - 1) + " : " + analyzer.stack);

			super.visitLabel(label);
			curLabel++;
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			if (DEBUG)
				System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);

			outFrames.set(curLabel - 1,
					new FrameNode(0, analyzer.locals.size(),
									 analyzer.locals.toArray(),
									 analyzer.stack.size(),
									 analyzer.stack.toArray()));
			super.visitTableSwitchInsn(min, max, dflt, labels);
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			if (DEBUG)
				System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);

			outFrames.set(curLabel - 1,
					new FrameNode(0, analyzer.locals.size(),
									 analyzer.locals.toArray(),
									 analyzer.stack.size(),
									 analyzer.stack.toArray()));
			super.visitLookupSwitchInsn(dflt, keys, labels);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ATHROW) {
				if (DEBUG)
					System.out.println("Rewriting " + curLabel + " OUT to " + analyzer.stack);

				if (analyzer.locals != null && analyzer.stack != null)
					outFrames.set(curLabel - 1,
							new FrameNode(0, analyzer.locals.size(),
											 analyzer.locals.toArray(),
											 analyzer.stack.size(),
											 analyzer.stack.toArray()));
			}
			super.visitInsn(opcode);
		}

		public void visitJumpInsn(int opcode, Label label) {
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
			// The analyzer won't have executed yet, so simulate it did :'(
			List<Object> stack = new ArrayList<Object>(analyzer.stack);
			// System.out.println("got to remove " + nToPop
			// + " from " + analyzer.stack + " in " + className + "."+name );
			while (nToPop > 0 && !stack.isEmpty()) {
				stack.remove(stack.size() - 1);
				nToPop--;
			}

			if (DEBUG)
				System.out.println(name + " Rewriting " + curLabel + " OUT to " + stack);

			outFrames.set(curLabel - 1,
					new FrameNode(0, analyzer.locals.size(),
									 analyzer.locals.toArray(),
									 stack.size(),
									 stack.toArray()));
			visitLabel(new Label());
		}

		@Override
		public void visitEnd() {
			final HashMap<Integer, LinkedList<Integer>> neverAutoBoxByFrame =
										new HashMap<Integer, LinkedList<Integer>>();
			final HashMap<Integer, LinkedList<Integer>> alwaysAutoBoxByFrame =
										new HashMap<Integer, LinkedList<Integer>>();
			final HashMap<Integer, LinkedList<Integer>> outEdges =
										new HashMap<Integer, LinkedList<Integer>>();
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
							if (instructions.get(j).getType() == AbstractInsnNode.FRAME
									|| instructions.get(j).getType() == AbstractInsnNode.LABEL
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
						// System.out.println(idx + "->"+label);
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
								System.out.println(name + " Must merge: " + edge.getValue()
										+ " into " + successor + " AKA " + labelToSuccessor);
							if (DEBUG)
								System.out.println("Input to successor: " + inFrames.get(labelToSuccessor).stack);

							for (Integer toMerge : edge.getValue()) {
								int labelToMerge = getLabel(toMerge);
								if (DEBUG)
									System.out.println(toMerge + " AKA " + labelToMerge);
								if (DEBUG)
									System.out.println((outFrames.get(labelToMerge) == null
												? "null"
												: outFrames.get(labelToMerge).stack));
								if (!outFrames.get(labelToMerge).stack.isEmpty()
										&& !inFrames.get(labelToSuccessor).stack.isEmpty()) {
									Object output1Top = outFrames.get(labelToMerge)
										.stack.get(outFrames.get(labelToMerge).stack.size() - 1);
									Object inputTop = inFrames.get(labelToSuccessor)
										.stack.get(inFrames.get(labelToSuccessor).stack.size() - 1);

									if (output1Top == Opcodes.TOP)
										output1Top = outFrames.get(labelToMerge)
											.stack.get(outFrames.get(labelToMerge).stack.size() - 2);
									if (inputTop == Opcodes.TOP)
										inputTop = inFrames.get(labelToSuccessor)
											.stack.get(inFrames.get(labelToSuccessor).stack.size() - 2);

									// System.out.println(className+"."+name+ " IN"+inputTop +" OUT " + output1Top);
									if (output1Top != null && output1Top != inputTop) {
										Type inputTopType = TaintAdapter.getTypeForStackType(inputTop);
										Type outputTopType = TaintAdapter.getTypeForStackType(output1Top);
										if ((output1Top == Opcodes.NULL)
												&& inputTopType.getSort() == Type.ARRAY
												&& inputTopType.getElementType().getSort() != Type.OBJECT
												&& inputTopType.getDimensions() == 1) {
											insertACONSTNULLBEFORE.add(toMerge);
										} else if ((inputTopType.getSort() == Type.OBJECT
													|| (inputTopType.getSort() == Type.ARRAY
														&& inputTopType.getElementType().getSort() == Type.OBJECT))
												&& outputTopType.getSort() == Type.ARRAY
												&& outputTopType.getElementType().getSort() != Type.OBJECT
												&& inputTopType.getDimensions() == 1) {
											insertACHECKCASTBEFORE.add(toMerge);
										}
									}
								}

								if (!outFrames.get(labelToMerge).local.isEmpty()
										&& !inFrames.get(labelToSuccessor).local.isEmpty()) {
									for (int i = 0; i < Math.min(outFrames.get(labelToMerge).local.size(),
												inFrames.get(labelToSuccessor).local.size()); i++) {
										Object out = outFrames.get(labelToMerge).local.get(i);
										Object in = inFrames.get(labelToSuccessor).local.get(i);
//										System.out.println(name +" " +out + " out, " + in + " In" + " i "+i);
//
										if (out instanceof String && in instanceof String) {
											Type tout = Type.getObjectType((String) out);
											Type tin = Type.getObjectType((String) in);
											if (tout.getSort() == Type.ARRAY
													&& tout.getElementType().getSort() != Type.OBJECT
													&& tout.getDimensions() == 1
													&& tin.getSort() == Type.OBJECT) {
												int insnN = getLastInsnByLabel(labelToMerge);
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
					if (!implicitAnalysisblocks.containsKey(insn)) {
						//insn not added yet
						fromBlock = new BasicBlock();
						fromBlock.idx = insn;
						fromBlock.insn = instructions.get(insn);
						implicitAnalysisblocks.put(insn,fromBlock);
					} else
						fromBlock = implicitAnalysisblocks.get(insn);
					
					AbstractInsnNode insnN = instructions.get(insn);
					fromBlock.isJump = (insnN.getType() == AbstractInsnNode.JUMP_INSN
							&& insnN.getOpcode() != Opcodes.GOTO)
						|| insnN.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN
						|| insnN.getType() == AbstractInsnNode.TABLESWITCH_INSN;

					if (fromBlock.isJump && insnN.getType() == AbstractInsnNode.JUMP_INSN) {
						switch(insnN.getOpcode()) {
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
					if (implicitAnalysisblocks.containsKey(successor))
						succesorBlock = implicitAnalysisblocks.get(successor);
					else {
						succesorBlock = new BasicBlock();
						succesorBlock.idx = successor;
						succesorBlock.insn = instructions.get(successor);
						implicitAnalysisblocks.put(successor, succesorBlock);
						if (succesorBlock.insn.getType() == AbstractInsnNode.IINC_INSN) {
							succesorBlock.varsWritten.add(((IincInsnNode)succesorBlock.insn).var);
						} else if (succesorBlock.insn.getType() == AbstractInsnNode.VAR_INSN) {
							switch(succesorBlock.insn.getOpcode()) {
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

					if(fromBlock.isJump) {
						if(fromBlock.covered)
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
				ArrayList<Integer> toAddNullBefore = new ArrayList<Integer>();
				toAddNullBefore.addAll(insertACONSTNULLBEFORE);
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
						if (DEBUG)
							System.out.println("Adding Null before: " + i);
						if (insertAfter.getOpcode() == Opcodes.GOTO)
							insertAfter = insertAfter.getPrevious();
						this.instructions.insert(insertAfter, new InsnNode(Opcodes.ACONST_NULL));
						nNewNulls++;
					} else if (insertACHECKCASTBEFORE.contains(i)) {
						if (DEBUG)
							System.out.println("Adding checkcast before: " + i + " (plus " + nNewNulls + ")");
						if (insertAfter.getOpcode() == Opcodes.GOTO)
							insertAfter = insertAfter.getPrevious();
						this.instructions.insert(insertAfter,
								new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Object.class)));
						nNewNulls++;
					} else if (neverAutoBoxByFrame.containsKey(i)) {
						if (insertAfter.getOpcode() == Opcodes.GOTO)
							insertAfter = insertAfter.getPrevious();
						for (int j : neverAutoBoxByFrame.get(i)) {
							// System.out.println("Adding nevefbox: before " + i + " (plus " + nNewNulls + ")");
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
							while (query.getNext() != null
									&& (query.getType() == AbstractInsnNode.LABEL
										|| query.getType() == AbstractInsnNode.LINE
										|| query.getType() == AbstractInsnNode.FRAME))
								query = query.getNext();
							if (query.getOpcode() == Opcodes.ALOAD
									&& query.getNext().getOpcode() == Opcodes.MONITOREXIT)
								insertAfter = query.getNext();
							if (insertAfter.getType() == AbstractInsnNode.JUMP_INSN) {
								insertAfter = insertAfter.getPrevious();
//								System.out.println("insertbefore  : " + ((JumpInsnNode) insertAfter.getNext()).toString());
								if (insertAfter.getNext().getOpcode() != Opcodes.GOTO) {
									this.instructions.insert(insertAfter,
											new VarInsnNode(TaintUtils.ALWAYS_BOX_JUMP, j));
								} else {
//									System.out.println("box immediately");
									this.instructions.insert(insertAfter,
											new VarInsnNode(TaintUtils.ALWAYS_AUTOBOX, j));
								}
							} else {
//								System.out.println("InsertAfter: " + insertAfter);
								this.instructions.insert(insertAfter,
										new VarInsnNode(TaintUtils.ALWAYS_AUTOBOX, j));
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
								if ((toCheck.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP
											|| toCheck.getOpcode() ==TaintUtils.ALWAYS_AUTOBOX)
										&& ((VarInsnNode) toCheck).var == lv.index) {
									// System.out.println("LV " + lv.name + " will be a prob around " + toCheck);
									LabelNode beforeProblem = new LabelNode(new Label());
									LabelNode afterProblem = new LabelNode(new Label());
									this.instructions.insertBefore(toCheck, beforeProblem);
									this.instructions.insert(toCheck.getNext(), afterProblem);
									LocalVariableNode newLV =
										new LocalVariableNode(lv.name, lv.desc, lv.signature,
												afterProblem, veryEnd, lv.index);
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

			if (Configuration.IMPLICIT_TRACKING) {
				if (implicitAnalysisblocks.size() > 1) {
					Stack<BasicBlock> stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();
					stack.add(implicitAnalysisblocks.get(0));
					while (!stack.isEmpty()) {
						BasicBlock b = stack.pop();
						if (b.visited)
							continue;
						b.visited = true;
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						//Propogate markings to successors
						for (BasicBlock s : b.successors) {
							s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
							s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
							s.resolvedBlocks.addAll(b.resolvedBlocks);
							
							s.onFalseSideOfJumpFrom.remove(s);
							s.onTrueSideOfJumpFrom.remove(s);
							if (!s.visited)
								stack.add(s);
						}
					}
					// calculatePostDominators(implicitAnalysisblocks.get(0));
					for (BasicBlock b : implicitAnalysisblocks.values()) {
						// System.out.println(b.domBlocks);
						for (BasicBlock d : b.onFalseSideOfJumpFrom) {
							if (b.onTrueSideOfJumpFrom.contains(d)) {
								b.resolvedBlocks.add(d);
								b.resolvedHereBlocks.add(d);
							}
						}
						if (!b.resolvedBlocks.isEmpty()) {
							b.onTrueSideOfJumpFrom.removeAll(b.resolvedBlocks);
							b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
							// System.out.println("!!Remove " + b.resolvedBlocks + " at " + b.idx + " - " + b.insn);
						}
						b.visited = false;
						// System.out.println(b.insn + " dom: "  + b.domBlocks +", antidom: " + b.antiDomBlocks);
					}

					stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();
					stack.add(implicitAnalysisblocks.get(0));
					while (!stack.isEmpty()) {
						BasicBlock b = stack.pop();
						if (b.visited)
							continue;
						b.visited = true;
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
						// Propogate markings to successors
						for (BasicBlock s : b.successors) {
							s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
							s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
							s.resolvedBlocks.addAll(b.resolvedBlocks);
							if (!s.visited)
								stack.add(s);
						}
					}
					// calculatePostDominators(implicitAnalysisblocks.get(0));
					boolean hadChanges = true;
					while (hadChanges) {
						hadChanges = false;
						for (BasicBlock b : implicitAnalysisblocks.values()) {
							HashSet<BasicBlock> remove = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
							for (BasicBlock r : b.resolvedHereBlocks) {
								for (BasicBlock p : b.predecessors)
									if (p.resolvedBlocks.contains(r)) {
										remove.add(r);
										hadChanges = true;
									}
							}
							b.visited =false;
							b.resolvedHereBlocks.removeAll(remove);
						}
					}

					for (BasicBlock j : implicitAnalysisblocks.values()) {
						if (j.isJump) {
							stack = new Stack<PrimitiveArrayAnalyzer.BasicBlock>();
							stack.addAll(j.successors);
							while (!stack.isEmpty()) {
								BasicBlock b = stack.pop();
								if (b.visited)
									continue;
								b.visited = true;
								if (b.onFalseSideOfJumpFrom.contains(j)) {
									j.varsWrittenTrueSide.addAll(b.varsWritten);
									stack.addAll(b.successors);
								} else if (b.onTrueSideOfJumpFrom.contains(j)) {
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

							for(int i : diff) {
								instructions.insertBefore(b.insn,
										new VarInsnNode(TaintUtils.FORCE_CTRL_STORE, i));
							}
							instructions.insertBefore(b.insn,
									new VarInsnNode(TaintUtils.BRANCH_START, jumpID));
							jumpIDs.put(b, jumpID);
							if(b.is2ArgJump)
								jumpID++;
						}
					}

					for (BasicBlock b : implicitAnalysisblocks.values()) {
						for (BasicBlock r : b.resolvedHereBlocks) {
							// System.out.println("Resolved: " + jumpIDs.get(r) + " at " + b.idx);
							// System.out.println("GOt" + jumpIDs);
							AbstractInsnNode insn = b.insn;
							while (insn.getType() == AbstractInsnNode.FRAME
									|| insn.getType() == AbstractInsnNode.LINE
									|| insn.getType() == AbstractInsnNode.LABEL)
								insn = insn.getNext();
							instructions.insertBefore(insn,
									new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
							if(r.is2ArgJump)
								instructions.insertBefore(insn,
										new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)+1));
						}

						if (b.successors.isEmpty()) {
							instructions.insertBefore(b.insn,
									new InsnNode(TaintUtils.FORCE_CTRL_STORE));
							HashSet<BasicBlock> live =
								new HashSet<PrimitiveArrayAnalyzer.BasicBlock>(b.onFalseSideOfJumpFrom);
							live.addAll(b.onTrueSideOfJumpFrom);
							for (BasicBlock r : live) {
								instructions.insertBefore(b.insn,
										new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
								if (r.is2ArgJump)
									instructions.insertBefore(b.insn,
											new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r) + 1));
							}
						}
					}
					nJumps = jumpID;
				}
			}

			AbstractInsnNode insn = instructions.getFirst();
			while(insn != null) {
				if (insn.getType() == AbstractInsnNode.FRAME) {
					// Insert a note before the instruction before this guy
					AbstractInsnNode insertBefore = insn;
					while (insertBefore != null
							&& (insertBefore.getType() == AbstractInsnNode.FRAME
								|| insertBefore.getType() == AbstractInsnNode.LINE
								|| insertBefore.getType() == AbstractInsnNode.LABEL))
						insertBefore = insertBefore.getPrevious();
					if (insertBefore != null)
						this.instructions.insertBefore(insertBefore,
								new InsnNode(TaintUtils.FOLLOWED_BY_FRAME));
				}
				insn = insn.getNext();
			}
			this.accept(cmv);
		}

		HashMap<Integer,BasicBlock> implicitAnalysisblocks =
			new HashMap<Integer,PrimitiveArrayAnalyzer.BasicBlock>();

		void calculatePostDominators(BasicBlock b)
		{
			if(b.visited)
				return;
			b.visited = true;
			b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
			b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
			//Propogate markings to successors
			for (BasicBlock s : b.successors) {
				s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
				s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
				s.resolvedBlocks.addAll(b.resolvedBlocks);
				if(!s.visited)
					calculatePostDominators(s);
			}
		}
	}

	static class BasicBlock{
		int idx;
		// LinkedList<Integer> outEdges = new LinkedList<Integer>();
		AbstractInsnNode insn;
		boolean covered;
		boolean visited;
		boolean isJump;
		boolean is2ArgJump;
		HashSet<BasicBlock> successors = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> predecessors  = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> resolvedHereBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> resolvedBlocks = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onFalseSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<BasicBlock> onTrueSideOfJumpFrom = new HashSet<PrimitiveArrayAnalyzer.BasicBlock>();
		HashSet<Integer> varsWritten = new HashSet<Integer>();
		HashSet<Integer> varsWrittenTrueSide = new HashSet<Integer>();
		HashSet<Integer> varsWrittenFalseSide = new HashSet<Integer>();

		@Override
		public String toString() {
			return insn.toString();
		}
	}

	private static boolean isPrimitiveArrayType(BasicValue v) {
		if (v == null || v.getType() == null)
			return false;
		return v.getType().getSort() == Type.ARRAY
			&& v.getType().getElementType().getSort() != Type.OBJECT;
	}

	static final boolean DEBUG = false;
	public HashSet<Type> wrapperTypesToPreAlloc = new HashSet<Type>();
	public int nJumps;

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		super.visitMethodInsn(opcode, owner, name, desc,itfc);
		Type returnType = Type.getReturnType(desc);
		Type newReturnType = TaintUtils.getContainerReturnType(returnType);
		if (newReturnType != returnType
				&& !(returnType.getSort() == Type.ARRAY
					&& returnType.getDimensions() > 1))
			wrapperTypesToPreAlloc.add(newReturnType);
	}

	public PrimitiveArrayAnalyzer(final String className, int access, final String name,
			final String desc, String signature, String[] exceptions,
			final MethodVisitor cmv) {
		super(Opcodes.ASM5);
		this.mv = new PrimitiveArrayAnalyzerMN(access, name,
				desc, signature, exceptions, className, cmv);
	}

	public PrimitiveArrayAnalyzer(Type singleWrapperTypeToAdd) {
		super(Opcodes.ASM5);
		this.mv = new PrimitiveArrayAnalyzerMN(0, null, null, null, null, null, null);
		if(singleWrapperTypeToAdd.getSort() == Type.OBJECT
				&& singleWrapperTypeToAdd
					.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct")
				&& !singleWrapperTypeToAdd
					.getInternalName().contains("MultiDTainted"))
			this.wrapperTypesToPreAlloc.add(singleWrapperTypeToAdd);
	}

	private NeverNullArgAnalyzerAdapter analyzer;

	public void setAnalyzer(NeverNullArgAnalyzerAdapter preAnalyzer) {
		analyzer = preAnalyzer;
	}
}
