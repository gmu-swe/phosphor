package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraphBuilder;
import edu.columbia.cs.psl.phosphor.struct.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Creates directed graphs that represents all of the possible execution paths for a method.
 */

public class ControlFlowGraphCreator {

    /**
     * @param methodNode a method whose graph should be created
     * @return a control flow graph for the specified method
     */
    public static FlowGraph<ControlFlowNode> createControlFlowGraph(final MethodNode methodNode) {
        EntryPoint entryPoint = new EntryPoint();
        ExitPoint exitPoint = new ExitPoint();
        FlowGraphBuilder<ControlFlowNode> builder = new FlowGraphBuilder<ControlFlowNode>().addEntryPoint(entryPoint).addExitPoint(exitPoint);
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        if(instructions.length == 0) {
            return builder.addEdge(entryPoint, exitPoint).build();
        } else {
            int[] leaderIndices = calculateLeaders(methodNode.instructions, methodNode.tryCatchBlocks);
            BasicBlock[] basicBlocks = createBasicBlocks(instructions, leaderIndices);
            for(BasicBlock basicBlock : basicBlocks) {
                builder.addVertex(basicBlock);
            }
            Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(basicBlocks);
            addControlFlowEdges(labelBlockMap, entryPoint, exitPoint, basicBlocks, methodNode.tryCatchBlocks, builder);
            return builder.build();
        }
    }

    /**
     * @param instructions a sequence of instructions in a method
     * @param tryCatchBlocks the try catch blocks for the method
     * @return a list in ascending order of the indices of instructions that are the first instruction of some basic
     *              block for the method
     */
    private static int[] calculateLeaders(InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks) {
        Set<AbstractInsnNode> leaders = new HashSet<>();
        leaders.add(instructions.getFirst()); // First instruction is the leader for the first block
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(insn instanceof JumpInsnNode) {
                leaders.add(((JumpInsnNode) insn).label); // Mark the target of the jump as a leader
                leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
            } else if(insn instanceof TableSwitchInsnNode) {
                // Mark the targets of the switch as leaders
                leaders.add(((TableSwitchInsnNode) insn).dflt);
                for(AbstractInsnNode node : ((TableSwitchInsnNode) insn).labels) {
                    leaders.add(node);
                }
                leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
            } else if(insn instanceof LookupSwitchInsnNode) {
                // Mark the targets of the switch as leaders
                leaders.add(((LookupSwitchInsnNode) insn).dflt);
                for(AbstractInsnNode node : ((LookupSwitchInsnNode) insn).labels) {
                    leaders.add(node);
                }
                leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
            } else if(isExitInstruction(insn) && insn.getNext() != null) {
                leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
            }
        }
        // Add the start labels for exception handlers as leaders
        for(TryCatchBlockNode tryCatch : tryCatchBlocks) {
            leaders.add(tryCatch.handler);
        }
        int[] leaderIndices = new int[leaders.size()];
        int i = 0;
        for(AbstractInsnNode leader : leaders) {
            leaderIndices[i++] = instructions.indexOf(leader);
        }
        Arrays.sort(leaderIndices);
        return leaderIndices;
    }

    /**
     * @param instructions a sequence of instructions
     * @param leaderIndices a list of the indices in ascending order of instructions in the sequence that are the first
     *                instruction of some basic block
     * @return a list of basic blocks for the sequence of instructions
     */
    private static BasicBlock[] createBasicBlocks(AbstractInsnNode[] instructions, int[] leaderIndices) {
        BasicBlock[] blocks = new BasicBlock[leaderIndices.length];
        int l = 0; // Current index into leaderIndices
        int start = leaderIndices[l++];
        for(int i = 0; i < blocks.length; i++) {
            int end = (l < leaderIndices.length) ? leaderIndices[l++] : instructions.length;
            blocks[i] = new BasicBlock(instructions, start, end);
            start = end;
        }
        return blocks;
    }

    /**
     * @param blocks a list of basic blocks for an instruction sequence
     * @return a mapping from LabelNodes to the basic block that they start
     */
    private static Map<LabelNode, BasicBlock> createLabelBlockMapping(BasicBlock[] blocks) {
        Map<LabelNode, BasicBlock> labelBlockMap = new HashMap<>();
        for(BasicBlock block : blocks) {
            AbstractInsnNode insn = block.getFirstInsn();
            if(insn instanceof LabelNode) {
                labelBlockMap.put((LabelNode) insn, block);
            }
        }
        return labelBlockMap;
    }

    /**
     * Adds edges to the graph.
     *
     * @param labelBlockMap a mapping from LabelNodes to the basic block that they start
     * @param entryPoint special node used to represent the single entry point of the instruction sequence
     * @param exitPoint special node used to represent the single exit point of the instruction sequence
     * @param basicBlocks a list of basic blocks for the instruction sequence whose successors and predecessors are empty
     *               before this method executes and set after this method executes
     * @param tryCatchBlocks the try catch blocks for the method
     * @param builder the flow graph builder being used to construct the graph
     */
    private static void addControlFlowEdges(Map<LabelNode, BasicBlock> labelBlockMap, EntryPoint entryPoint, ExitPoint exitPoint,
                                            BasicBlock[] basicBlocks, List<TryCatchBlockNode> tryCatchBlocks, FlowGraphBuilder<ControlFlowNode> builder) {
        builder.addEdge(entryPoint, basicBlocks[0]);
        for(int i = 0; i < basicBlocks.length; i++) {
            AbstractInsnNode lastInsn = basicBlocks[i].getLastInsn();
            if(lastInsn instanceof JumpInsnNode) {
                builder.addEdge(basicBlocks[i], labelBlockMap.get(((JumpInsnNode) lastInsn).label));
                if(lastInsn.getOpcode() != Opcodes.GOTO) {
                    builder.addEdge(basicBlocks[i], basicBlocks[i + 1]);
                }
            } else if(lastInsn instanceof TableSwitchInsnNode) {
                builder.addEdge(basicBlocks[i], labelBlockMap.get(((TableSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((TableSwitchInsnNode) lastInsn).labels) {
                    builder.addEdge(basicBlocks[i], labelBlockMap.get(label));
                }
            } else if(lastInsn instanceof LookupSwitchInsnNode) {
                builder.addEdge(basicBlocks[i], labelBlockMap.get(((LookupSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((LookupSwitchInsnNode) lastInsn).labels) {
                    builder.addEdge(basicBlocks[i], labelBlockMap.get(label));
                }
            } else if(isExitInstruction(lastInsn)) {
                builder.addEdge(basicBlocks[i], exitPoint);
            } else if(i < basicBlocks.length - 1) {
                builder.addEdge(basicBlocks[i], basicBlocks[i + 1]);
            }
        }
        for(TryCatchBlockNode tryCatch : tryCatchBlocks) {
            builder.addEdge(entryPoint, labelBlockMap.get(tryCatch.handler));
        }
    }

    /**
     * @param instruction the instruction to be checked
     * @return true if the specified instruction triggers a method exit
     */
    public static boolean isExitInstruction(AbstractInsnNode instruction) {
        switch(instruction.getOpcode()) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:
            case Opcodes.ATHROW:
                return true;
            default:
                return false;
        }
    }


    /**
     * @param graph the graph whose vertices are to be searched for basic blocks
     * @return a list of basic block that are vertices in the specified graph
     */
    public static ArrayList<BasicBlock> getBasicBlocks(FlowGraph<ControlFlowNode> graph) {
        ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
        for(ControlFlowNode node : graph.getVertices()) {
            if(node instanceof BasicBlock) {
                basicBlocks.add((BasicBlock) node);
            }
        }
        return basicBlocks;
    }
}
