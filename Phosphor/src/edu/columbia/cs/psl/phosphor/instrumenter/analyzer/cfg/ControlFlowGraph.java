package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraphBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A directed graph that represents all of the possible execution paths for a method.
 */

public class ControlFlowGraph {
    
    /**
     * The single point of entry for this graph
     */
    private final EntryPoint entryPoint;

    /**
     * The single point of exit for this graph
     */
    private final ExitPoint exitPoint;

    /**
     * An unmodifiable list containing all of the basic blocks (i.e., code sequences with no jump into or out of the middle
     * of the block) for the method that this graph represents. The elements are in increasing ordered by the index of
     * the first instruction in the block.
     */
    private final List<BasicBlock> basicBlocks;

    /**
     * An unmodifiable mapping from labels to the basic block that they start
     */
    public final Map<LabelNode, BasicBlock> labelBlockMap;

    /**
     * The underlying flow graph for this graph
     */
    public final FlowGraph<ControlFlowNode> backingGraph;

    /**
     * A mapping from nodes to unmodifiable set of their successors. Serves as a "cache" for calls to getSuccessors
     */
    private final Map<ControlFlowNode, Set<ControlFlowNode>> successorsStore = new HashMap<>();

    /**
     * Constructs a new control flow graph the represents all of the possible execution paths for the specified method.
     *
     * @param methodNode a method whose graph should be created
     */
    public ControlFlowGraph(MethodNode methodNode) {
        this.entryPoint = new EntryPoint();
        this.exitPoint = new ExitPoint();
        FlowGraphBuilder<ControlFlowNode> builder = new FlowGraphBuilder<ControlFlowNode>()
                .addEntryPoint(entryPoint)
                .addExitPoint(exitPoint);
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        if(instructions.length == 0) {
            this.basicBlocks = Collections.emptyList();
            this.labelBlockMap = Collections.emptyMap();
            this.backingGraph = builder.addEdge(entryPoint, exitPoint).build();
        } else {
            int[] leaderIndices = calculateLeaders(methodNode.instructions, methodNode.tryCatchBlocks);
            this.basicBlocks = createBasicBlocks(instructions, leaderIndices);
            for(BasicBlock basicBlock : basicBlocks) {
                builder.addVertex(basicBlock);
            }
            this.labelBlockMap = createLabelBlockMapping();
            addControlFlowEdges(methodNode.tryCatchBlocks, builder);
            this.backingGraph = builder.build();
        }
    }

    /**
     * @return the single point of entry for this graph
     */
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    /**
     * @return the single point of exit for this graph
     */
    public ExitPoint getExitPoint() {
        return exitPoint;
    }

    /**
     * @return this graph's unmodifiable list of basic blocks
     */
    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    /**
     * @param node the node whose successors are to be returned
     * @return a set containing the vertices that immediately succeed the specified node
     * @throws IllegalArgumentException if the specified node is not in this graph
     */
    public Set<ControlFlowNode> getSuccessors(ControlFlowNode node) {
        if(!successorsStore.containsKey(node)) {
            successorsStore.put(node, backingGraph.getSuccessors(node));
        }
        return successorsStore.get(node);
    }

    /**
     * @return the underlying flow graph for this graph
     */
    public FlowGraph<ControlFlowNode> getFlowGraph() {
        return backingGraph;
    }

    /**
     * @param instructions a sequence of instructions in a method
     * @param tryCatchBlocks the try catch blocks for the method
     * @return a list in ascending order of the indices of instructions that are the first instruction of some basic
     *              block for the method
     */
    private int[] calculateLeaders(InsnList instructions, java.util.List<TryCatchBlockNode> tryCatchBlocks) {
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
     * @return an unmodifiable list of basic blocks for the specified sequence of instructions order by instruction index
     *          of the leader of the block
     */
    private List<BasicBlock> createBasicBlocks(AbstractInsnNode[] instructions, int[] leaderIndices) {
        List<BasicBlock> blocks = new ArrayList<>();
        int l = 0; // Current index into leaderIndices
        int start = leaderIndices[l++];
        for(int i = 0; i < leaderIndices.length; i++) {
            int end = (l < leaderIndices.length) ? leaderIndices[l++] : instructions.length;
            blocks.add(new BasicBlock(instructions, start, end));
            start = end;
        }
        return Collections.unmodifiableList(blocks);
    }

    /**
     * @return a unmodifiable mapping from LabelNodes to the basic block that they start
     */
    private Map<LabelNode, BasicBlock> createLabelBlockMapping() {
        Map<LabelNode, BasicBlock> labelBlockMap = new HashMap<>();
        for(BasicBlock block : basicBlocks) {
            AbstractInsnNode insn = block.getFirstInsn();
            if(insn instanceof LabelNode) {
                labelBlockMap.put((LabelNode) insn, block);
            }
        }
        return Collections.unmodifiableMap(labelBlockMap);
    }

    /**
     * Adds edges to the graph.
     *
     * @param tryCatchBlocks the try catch blocks for the method
     * @param builder the flow graph builder being used to construct the graph
     */
    private void addControlFlowEdges(java.util.List<TryCatchBlockNode> tryCatchBlocks, FlowGraphBuilder<ControlFlowNode> builder) {
        builder.addEdge(entryPoint, basicBlocks.get(0));
        for(int i = 0; i < basicBlocks.size(); i++) {
            AbstractInsnNode lastInsn = basicBlocks.get(i).getLastInsn();
            if(lastInsn instanceof JumpInsnNode) {
                builder.addEdge(basicBlocks.get(i), labelBlockMap.get(((JumpInsnNode) lastInsn).label));
                if(lastInsn.getOpcode() != Opcodes.GOTO) {
                    builder.addEdge(basicBlocks.get(i), basicBlocks.get(i + 1));
                }
            } else if(lastInsn instanceof TableSwitchInsnNode) {
                builder.addEdge(basicBlocks.get(i), labelBlockMap.get(((TableSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((TableSwitchInsnNode) lastInsn).labels) {
                    builder.addEdge(basicBlocks.get(i), labelBlockMap.get(label));
                }
            } else if(lastInsn instanceof LookupSwitchInsnNode) {
                builder.addEdge(basicBlocks.get(i), labelBlockMap.get(((LookupSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((LookupSwitchInsnNode) lastInsn).labels) {
                    builder.addEdge(basicBlocks.get(i), labelBlockMap.get(label));
                }
            } else if(isExitInstruction(lastInsn)) {
                builder.addEdge(basicBlocks.get(i), exitPoint);
            } else if(i < basicBlocks.size() - 1) {
                builder.addEdge(basicBlocks.get(i), basicBlocks.get(i + 1));
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
    private static boolean isExitInstruction(AbstractInsnNode instruction) {
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
}
