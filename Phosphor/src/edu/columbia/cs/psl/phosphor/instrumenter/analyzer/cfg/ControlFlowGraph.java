package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

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

    /**
     * Special node used as the single entry point for this graph
     */
    private final EntryPoint entryPoint;

    /**
     * Special nodes used as the entry points for any exception handlers
     */
    private final ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints;

    /**
     * Special node used as the single exit point for this graph
     */
    private final ExitPoint exitPoint;

    /**
     * Nodes in this graph that contain instructions from the original sequence
     */
    private final SinglyLinkedList<BasicBlock> basicBlocks;

    /**
     * The nodes of this graph in reverse post-order with respect to this graph or null if this value has not been
     * calculated since the graph was last modified
     */
    private ControlFlowNode[] reversePostOrder = null;

    /**
     * The nodes of this graph in reverse post-order with respect to the traverse of this graph or null if this value
     * has not been calculated since the graph was last modified
     */
    private ControlFlowNode[] transverseReversePostOrder = null;

    private ControlFlowGraph(EntryPoint entryPoint, ExitPoint exitPoint, SinglyLinkedList<BasicBlock> basicBlocks, ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints) {
        this.basicBlocks = basicBlocks;
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
        this.exceptionHandlerEntryPoints = exceptionHandlerEntryPoints;
    }

    /**
     * Clears any values calculated on the current graph's structure that would need to be recalculated if the graph
     * were modified.
     */
    void prepareForModification() {
        this.reversePostOrder = null;
        this.transverseReversePostOrder = null;
    }

    EntryPoint getEntryPoint() {
        return entryPoint;
    }

    ExitPoint getExitPoint() {
        return exitPoint;
    }

    BasicBlock[] getBasicBlocks() {
        return basicBlocks.toArray(new BasicBlock[0]);
    }

    Map<ControlFlowNode, Set<ControlFlowNode>> calculateDominanceFrontiers() {
        if(reversePostOrder == null) {
            assignReversePostOrderNumbers();
        }
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = new HashMap<>();
        for(ControlFlowNode node : reversePostOrder) {
            dominanceFrontiers.put(node, new HashSet<ControlFlowNode>());
        }
        int[] dominators = calculateDominators();
        for(int i = 0; i < reversePostOrder.length; i++) {
            if(reversePostOrder[i].predecessors.size() > 1) {
                for(ControlFlowNode predecessor : reversePostOrder[i].predecessors) {
                    ControlFlowNode runner = predecessor;
                    while(runner.reversePostOrderIndex != dominators[i]) {
                        dominanceFrontiers.get(runner).add(reversePostOrder[i]);
                        runner = reversePostOrder[dominators[runner.reversePostOrderIndex]];
                    }
                }
            }
        }
        return dominanceFrontiers;
    }

    /**
     * @return a mapping from nodes to their immediate dominator in the graph or null if they do not have an immediate
     *              dominator
     */
    Map<ControlFlowNode, ControlFlowNode> calculateImmediateDominators() {
        if(reversePostOrder == null) {
            assignReversePostOrderNumbers();
        }
        Map<ControlFlowNode, ControlFlowNode> immediateDominators = new HashMap<>();
        int[] dominators = calculateDominators();
        for(int i = 0; i < dominators.length; i++) {
            if(dominators[i] == i) {
                immediateDominators.put(reversePostOrder[i], null);
            } else {
                immediateDominators.put(reversePostOrder[i], reversePostOrder[dominators[i]]);
            }
        }
        return immediateDominators;
    }

    private int[] calculateDominators() {
        if(reversePostOrder == null) {
            assignReversePostOrderNumbers();
        }
        int[] dominators = new int[reversePostOrder.length];
        for(int i = 1; i < dominators.length; i++) {
            dominators[i] = -1; // initialize the dominators as undefined, except for the start node which should be itself
        }
        boolean changed = true;
        while(changed) {
            changed = false;
            for(int i = 1; i < dominators.length; i++) {
                int newImmediate = -1;
                for(ControlFlowNode predecessor : reversePostOrder[i].predecessors) {
                    if(dominators[predecessor.reversePostOrderIndex] != -1) {
                        if(newImmediate == -1) {
                            newImmediate = predecessor.reversePostOrderIndex;
                        } else {
                            newImmediate = intersect(dominators, predecessor.reversePostOrderIndex, newImmediate);
                        }
                    }
                }
                if(dominators[i] != newImmediate) {
                    dominators[i] = newImmediate;
                    changed = true;
                }
            }
        }
        return dominators;
    }

    /**
     * @return a mapping from nodes to their immediate post-dominator in the graph or null if they do not have an
     *              immediate post-dominator
     */
    Map<ControlFlowNode, ControlFlowNode> calculateImmediatePostDominators() {
        if(transverseReversePostOrder == null) {
            assignTransverseReversePostOrderNumbers();
        }
        Map<ControlFlowNode, ControlFlowNode> immediatePostDominators = new HashMap<>();
        int[] postDominators = calculatePostDominators();
        for(int i = 0; i < postDominators.length; i++) {
            if(postDominators[i] == i) {
                immediatePostDominators.put(transverseReversePostOrder[i], null);
            } else {
                immediatePostDominators.put(transverseReversePostOrder[i], transverseReversePostOrder[postDominators[i]]);
            }
        }
        return immediatePostDominators;
    }

    private int[] calculatePostDominators() {
        if(transverseReversePostOrder == null) {
            assignTransverseReversePostOrderNumbers();
        }
        int[] postDominators = new int[transverseReversePostOrder.length];
        for(int i = 1; i < postDominators.length; i++) {
            postDominators[i] = -1; // initialize the post-dominators as undefined, except for the end node which should be itself
        }
        boolean changed = true;
        while(changed) {
            changed = false;
            for(int i = 1; i < postDominators.length; i++) {
                int newImmediate = -1;
                for(ControlFlowNode successor : transverseReversePostOrder[i].successors) {
                    if(postDominators[successor.transposeReversePostOrderIndex] != -1) {
                        if(newImmediate == -1) {
                            newImmediate = successor.transposeReversePostOrderIndex;
                        } else {
                            newImmediate = intersect(postDominators, successor.transposeReversePostOrderIndex, newImmediate);
                        }
                    }
                }
                if(postDominators[i] != newImmediate) {
                    postDominators[i] = newImmediate;
                    changed = true;
                }
            }
        }
        return postDominators;
    }

    /* Helper function for calculateDominators and calculatePostDominators. */
    private static int intersect(int[] dominators, int node1, int node2) {
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
     * Calculates the reverse post-ordering of this graph's nodes for this graph. Numbers this graph's nodes with their
     * positions in the calculated ordering.
     */
    private void assignReversePostOrderNumbers() {
        SinglyLinkedList<ControlFlowNode> stack = new SinglyLinkedList<>();
        clearMarks(entryPoint, exitPoint, basicBlocks);
        for(ControlFlowNode node : exceptionHandlerEntryPoints) {
            dfs(node, stack, false);
        }
        dfs(entryPoint, stack, false);
        int i = 0;
        for(ControlFlowNode node : stack) {
            node.reversePostOrderIndex = i++;
        }
        this.reversePostOrder = stack.toArray(new ControlFlowNode[0]);
    }

    /**
     * Calculates the reverse post-ordering of this graph's nodes for the transverse of this graph. Numbers this graph's
     * nodes with their positions in the calculated ordering.
     */
    private void assignTransverseReversePostOrderNumbers() {
        SinglyLinkedList<ControlFlowNode> stack = new SinglyLinkedList<>();
        clearMarks(entryPoint, exitPoint, basicBlocks);
        dfs(exitPoint, stack, true);
        int i = 0;
        for(ControlFlowNode node : stack) {
            node.transposeReversePostOrderIndex = i++;
        }
        this.transverseReversePostOrder = stack.toArray(new ControlFlowNode[0]);
    }

    /* Helper function for assignReversePostOrderNumbers and assignTransverseReversePostOrderNumbers. Performs a depth
     * first search of the graph or reverse graph. */
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
     * @param methodNode a method whose graph should be created
     * @return a control flow graph for the specified method
     */
    public static ControlFlowGraph analyze(final MethodNode methodNode) {
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints = createExceptionHandlerEntryPoints(methodNode.tryCatchBlocks);
        int[] leaderIndices = calculateLeaders(methodNode.instructions, exceptionHandlerEntryPoints);
        BasicBlock[] basicBlocks = createBasicBlocks(instructions, leaderIndices);
        Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(basicBlocks);
        EntryPoint entryPoint = new EntryPoint();
        ExitPoint exitPoint = new ExitPoint();
        addControlFlowEdges(labelBlockMap, entryPoint, exitPoint, basicBlocks, exceptionHandlerEntryPoints);
        SinglyLinkedList<BasicBlock> basicBlockList = new SinglyLinkedList<>();
        for(BasicBlock basicBlock : basicBlocks) {
            basicBlockList.enqueue(basicBlock);
        }
        return new ControlFlowGraph(entryPoint, exitPoint, basicBlockList, exceptionHandlerEntryPoints);
    }

    /**
     * @param tryCatchBlocks the try-catch blocks for a method
     * @return exception handler entry points for the method
     */
    private static ExceptionHandlerEntryPoint[] createExceptionHandlerEntryPoints(List<TryCatchBlockNode> tryCatchBlocks) {
        ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints = new ExceptionHandlerEntryPoint[tryCatchBlocks.size()];
        Iterator<TryCatchBlockNode> itr = tryCatchBlocks.iterator();
        for(int i = 0; i < exceptionHandlerEntryPoints.length; i++) {
            exceptionHandlerEntryPoints[i] = new ExceptionHandlerEntryPoint(itr.next());
        }
        return exceptionHandlerEntryPoints;
    }

    /**
     * @param instructions a sequence of instructions in a method
     * @param exceptionHandlerEntryPoints exception handler entry points for the method
     * @return a list in ascending order of the indices of instructions that are the first instruction of some basic
     *              block for the method
     */
    private static int[] calculateLeaders(InsnList instructions, ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints) {
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
        // Add the start labels for exception handlers as leaders
        for(ExceptionHandlerEntryPoint handlerEntry : exceptionHandlerEntryPoints) {
            leaders.add(handlerEntry.getHandlerStart());
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
    static Map<LabelNode, BasicBlock> createLabelBlockMapping(BasicBlock[] blocks) {
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
     * Calculates and sets the successors and predecessors of the specified nodes.
     *
     * @param labelBlockMap a mapping from LabelNodes to the basic block that they start
     * @param entryPoint special node used to represent the single entry point of the instruction sequence
     * @param exitPoint special node used to represent the single exit point of the instruction sequence
     * @param basicBlocks a list of basic blocks for the instruction sequence whose successors and predecessors are empty
     *               before this method executes and set after this method executes
     * @param exceptionHandlerEntryPoints nodes that represent the entry points into exception handlers
     */
    private static void addControlFlowEdges(Map<LabelNode, BasicBlock> labelBlockMap, EntryPoint entryPoint, ExitPoint exitPoint,
                                            BasicBlock[] basicBlocks, ExceptionHandlerEntryPoint[] exceptionHandlerEntryPoints) {
        addEdge(entryPoint, basicBlocks[0]);
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
                addEdge(basicBlocks[i], exitPoint);
            } else if(i < basicBlocks.length - 1) {
                addEdge(basicBlocks[i], basicBlocks[i + 1]);
            }
        }
        for(ExceptionHandlerEntryPoint handlerEntryPoint : exceptionHandlerEntryPoints) {
            addEdge(handlerEntryPoint, labelBlockMap.get(handlerEntryPoint.getHandlerStart()));
        }
    }

    /**
     * @param instruction an instruction to be checked
     * @return true if he specified instruction node triggers a method exit.
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
     * Sets the marked field of the specified nodes to false
     * @param first a non-null node whose marked field will be set to false
     * @param second a non-null node whose marked field will be set to false
     * @param nodes non-null nodes whose marked fields will be set to false
     */
    private static void clearMarks(ControlFlowNode first, ControlFlowNode second, SinglyLinkedList<? extends ControlFlowNode> nodes) {
        first.unmark();
        second.unmark();
        for(ControlFlowNode node : nodes) {
            node.unmark();
        }
    }
}
