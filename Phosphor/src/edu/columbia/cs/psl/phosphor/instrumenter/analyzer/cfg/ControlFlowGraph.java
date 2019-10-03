package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.IntObjectAMT;
import edu.columbia.cs.psl.phosphor.struct.IntSinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowNode.addEdge;

/**
 * A directed graph that represents the ways that program control can flow through instructions.
 *
 *
 * Uses algorithms for calculating dominators, immediate dominators, and dominance frontiers from the following:
 * K.D. Cooper, T.J. Harvey, and K. Kennedy, “A Simple, Fast Dominance Algorithm,” Rice University,
 * Department of Computer Science Technical Report 06-33870, 2006.
 * http://www.cs.rice.edu/~keith/EMBED/dom.pdf
 */
public class ControlFlowGraph {

    private static final boolean TRACK_EXCEPTIONAL_CONTROL_FLOWS = Configuration.IMPLICIT_EXCEPTION_FLOW;

    /**
     * Special node used as the single entry point for this graph
     */
    private final EntryBlock entryBlock;

    /**
     * Special node used as the single exit point for this graph
     */
    private final ExitBlock exitBlock;

    /**
     * Nodes in this graph that contain instructions from the original sequence
     */
    private final BasicBlock[] basicBlocks;

    /**
     * The nodes of this graph in reverse post-order with respect to this graph
     */
    private ControlFlowNode[] reversePostOrder;

    /**
     * The nodes of this graph in reverse post-order with respect to the traverse of this graph
     */
    private ControlFlowNode[] transverseReversePostOrder;

    private ControlFlowGraph(EntryBlock entryBlock, ExitBlock exitBlock, BasicBlock[] basicBlocks) {
        this.basicBlocks = basicBlocks;
        this.entryBlock = entryBlock;
        this.exitBlock = exitBlock;
        assignReversePostOrderNumbers();
    }

    private int[] calculateDominators() {
        int[] dominators = new int[reversePostOrder.length];
        for(int i = 1; i < dominators.length; i++) {
            dominators[i] = -1; // initialize the dominators as undefined, except for the start node which should be itself
        }
        boolean changed = true;
        while(changed) {
            changed = false;
            for(int i = 1; i < dominators.length; i++) {
                int newImmediateDom = -1;
                for(ControlFlowNode predecessor : reversePostOrder[i].predecessors) {
                    if(dominators[predecessor.reversePostOrderIndex] != -1) {
                        if(newImmediateDom == -1) {
                            newImmediateDom = predecessor.reversePostOrderIndex;
                        } else {
                            newImmediateDom = intersect(dominators, predecessor.reversePostOrderIndex, newImmediateDom);
                        }
                    }
                }
                if(dominators[i] != newImmediateDom) {
                    dominators[i] = newImmediateDom;
                    changed = true;
                }
            }
        }
        return dominators;
    }

    /* Helper function for calculateDominators. */
    private int intersect(int[] dominators, int node1, int node2) {
        while(node1 != node2) {
            while(node1 > node2) {
                node1 = dominators[node1];
            }
            while(node2 > node1) {
                node2 = dominators[node2];
            }
        }
        return node1;
    }

    /**
     * Calculates the reverse post-ordering of this graph's nodes for this graph and the transverse of this graph.
     * Numbers this graph's nodes with their positions in the calculated orderings.
     */
    private void assignReversePostOrderNumbers() {
        SinglyLinkedList<ControlFlowNode> stack = new SinglyLinkedList<>();
        clearMarks(entryBlock, exitBlock, basicBlocks);
        dfs(entryBlock, stack, false);
        int i = 0;
        for(ControlFlowNode node : stack) {
            node.reversePostOrderIndex = i++;
        }
        this.reversePostOrder = stack.toArray(new ControlFlowNode[0]);
        stack.clear();
        clearMarks(entryBlock, exitBlock, basicBlocks);
        dfs(exitBlock, stack, true);
        i = 0;
        for(ControlFlowNode node : stack) {
            node.transposeReversePostOrderIndex = i++;
        }
        this.transverseReversePostOrder = stack.toArray(new ControlFlowNode[0]);
    }

    /* Helper method for numberNodes. Performs a depth first search of the graph . */
    private static void dfs(ControlFlowNode node, SinglyLinkedList<ControlFlowNode> stack, boolean reverseGraph) {
        node.mark();
        for(ControlFlowNode child : (reverseGraph ? node.predecessors : node.successors)) {
            if(!child.isMarked()) {
                dfs(child, stack, reverseGraph);
            }
        }
        stack.push(node);
    }

    /**
     * @return a mapping from the reverse post-order index of nodes in this graph to lists of the reverse post-order
     *              indices of the successors of each node
     */
    public IntObjectAMT<IntSinglyLinkedList> getReversePostOrderSuccessorsMap() {
        IntObjectAMT<IntSinglyLinkedList> nodeSuccessorsMap = new IntObjectAMT<>();
        for(ControlFlowNode node : reversePostOrder) {
            IntSinglyLinkedList successors = new IntSinglyLinkedList();
            for(ControlFlowNode successor : node.successors) {
                successors.enqueue(successor.reversePostOrderIndex);
            }
            nodeSuccessorsMap.put(node.reversePostOrderIndex, successors);
        }
        return nodeSuccessorsMap;
    }

    /**
     * @param methodNode a method whose graph should be created
     * @return a control flow graph for the specified method
     */
    public static ControlFlowGraph analyze(final MethodNode methodNode) {
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        int[] leaderIndices = calculateLeaders(methodNode.instructions);
        BasicBlock[] basicBlocks = createBasicBlocks(instructions, leaderIndices);
        Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(basicBlocks);
        EntryBlock entryBlock = new EntryBlock();
        ExitBlock exitBlock = new ExitBlock();
        addControlFlowEdges(labelBlockMap, entryBlock, exitBlock, basicBlocks);
        return new ControlFlowGraph(entryBlock, exitBlock, basicBlocks);
    }

    /**
     * @param instructions a sequence of instructions
     * @return a list in ascending order of the indices of instructions that are the first instruction of some basic
     *              block
     */
    private static int[] calculateLeaders(InsnList instructions) {
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
            } else if(isExitInstruction(insn)) {
                leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
            }
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
     * Calculates and sets the successors and predecessors of each of the specified basic blocks.
     *
     * @param labelBlockMap a mapping from LabelNodes to the basic block that they start
     * @param entryBlock special node used to represent the single entry point of the instruction sequence
     * @param exitBlock special node used to represent the single exit point of the instruction sequence
     * @param basicBlocks a list of basic blocks for the instruction sequence whose successors and predecessors are empty
     *               before this method executes and set after this method executes
     */
    private static void addControlFlowEdges(Map<LabelNode, BasicBlock> labelBlockMap, EntryBlock entryBlock, ExitBlock exitBlock,
                                            BasicBlock[] basicBlocks) {
        addEdge(entryBlock, basicBlocks[0]);
        for(int i = 0; i < basicBlocks.length; i++) {
            AbstractInsnNode lastInsn = basicBlocks[i].getLastInsn();
            if(lastInsn instanceof JumpInsnNode) {
                addEdge(basicBlocks[i], labelBlockMap.get(((JumpInsnNode) lastInsn).label));
                if(lastInsn.getOpcode() != Opcodes.GOTO) {
                    addEdge(basicBlocks[i], basicBlocks[i + 1]);
                }
            } else if(lastInsn instanceof TableSwitchInsnNode) {
                addEdge(basicBlocks[i], labelBlockMap.get(((TableSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((TableSwitchInsnNode) lastInsn).labels) {
                    addEdge(basicBlocks[i], labelBlockMap.get(label));
                }
            } else if(lastInsn instanceof LookupSwitchInsnNode) {
                addEdge(basicBlocks[i], labelBlockMap.get(((LookupSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((LookupSwitchInsnNode) lastInsn).labels) {
                    addEdge(basicBlocks[i], labelBlockMap.get(label));
                }
            } else if(isExitInstruction(lastInsn)) {
                addEdge(basicBlocks[i], exitBlock);
            } else if(i < basicBlocks.length - 1) {
                addEdge(basicBlocks[i], basicBlocks[i + 1]);
            }
        }
    }

    /**
     * @param instruction an instruction to be checked
     * @return true if he specified instruction node triggers a method exit.
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

    /**
     * Sets the marked field of the specified nodes to false
     * @param first a non-null node whose marked field will be set to false
     * @param second a non-null node whose marked field will be set to false
     * @param nodes non-null nodes whose marked fields will be set to false
     */
    private static void clearMarks(ControlFlowNode first, ControlFlowNode second, ControlFlowNode... nodes) {
        first.unmark();
        second.unmark();
        for(ControlFlowNode node : nodes) {
            node.unmark();
        }
    }
}
