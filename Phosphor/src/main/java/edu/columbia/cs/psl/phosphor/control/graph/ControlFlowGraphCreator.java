package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.Iterator;

import static org.objectweb.asm.Opcodes.GOTO;

/**
 * Creates control flow graphs that represents all of the possible execution paths through a method.
 */
public abstract class ControlFlowGraphCreator {

    /**
     * Returns a flow graph that represents all of the possible execution paths through the specified method.
     *
     * @param methodNode the method whose flow graph is being constructed
     * @return a flow graph that represents all of the possible execution paths through the specified method
     */
    public final FlowGraph<BasicBlock> createControlFlowGraph(MethodNode methodNode) {
        addEntryPoint();
        addExitPoint();
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        if(instructions.length == 0) {
            addEntryExitEdge();
        } else {
            int[] leaderIndices = calculateLeaders(methodNode.instructions, methodNode.tryCatchBlocks);
            // Add a basic block for each leader
            List<BasicBlock> basicBlocks = new ArrayList<>();
            int l = 0; // Current index into leaderIndices
            int start = leaderIndices[l++];
            for(int i = 0; i < leaderIndices.length; i++) {
                int end = (l < leaderIndices.length) ? leaderIndices[l++] : instructions.length;
                basicBlocks.add(addBasicBlock(Arrays.copyOfRange(instructions, start, end), i));
                start = end;
            }
            addControlFlowEdges(basicBlocks, methodNode.tryCatchBlocks);
        }
        return buildGraph();
    }

    /**
     * Adds the single point of entry to the graph being created.
     */
    protected abstract void addEntryPoint();

    /**
     * Adds the single point of exit to the graph being created.
     */
    protected abstract void addExitPoint();

    /**
     * Creates a new basic block that represents the specified instruction sequence and adds it as a vertex to the graph
     * being created. Returns the newly created basic block.
     *
     * @param instructions the sequence of instructions in the basic block being added
     * @param index        the sequential index of the basic block being added in the list of all basic blocks in increasing
     *                     order by the index of their leader
     * @return a newly created basic block that represents the specified sequence of instructions
     */
    protected abstract BasicBlock addBasicBlock(AbstractInsnNode[] instructions, int index);

    /**
     * Adds a directed edge from the entry vertex to the exit vertex of the graph being created
     */
    protected abstract void addEntryExitEdge();

    /**
     * Adds a directed edge from the entry vertex of the graph being created to the specified basic block as a result of
     * the basic block containing the first instruction in the method.
     *
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addStandardEdgeFromEntryPoint(BasicBlock target);

    /**
     * Adds a directed edge from the entry vertex of the graph being created to the specified basic block as a result of
     * the basic block containing the start label for the specified try-catch block.
     *
     * @param target            the target vertex of the directed edge being added
     * @param tryCatchBlockNode the try-catch block for which the specified target block contains the start label
     */
    protected abstract void addExceptionalEdgeFromEntryPoint(BasicBlock target, TryCatchBlockNode tryCatchBlockNode);

    /**
     * Adds a directed edge from the specified basic block to the exit vertex of the graph as result of the specified
     * block ending in a return instruction.
     *
     * @param source the source vertex of the directed edge being added
     */
    protected abstract void addStandardEdgeToExitPoint(BasicBlock source);

    /**
     * Adds a directed edge from the specified basic block to the exit vertex of the graph as result of the specified
     * block ending in an exceptional return instruction.
     *
     * @param source the source vertex of the directed edge being added
     */
    protected abstract void addExceptionalEdgeToExitPoint(BasicBlock source);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * the sequential instruction execution i.e., the specified source block does not end in a jump and the specified
     * target block follows it in sequential order.
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addSequentialEdge(BasicBlock source, BasicBlock target);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * of the target block being the unconditional jump target of the specified source block.
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addUnconditionalJumpEdge(BasicBlock source, BasicBlock target);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * of the target block containing the conditional jump target of the specified source block.
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addBranchTakenEdge(BasicBlock source, BasicBlock target);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * of the target block following the specified source block in the sequential execution and the specified source
     * block ending with a conditional jump.
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addBranchNotTakenEdge(BasicBlock source, BasicBlock target);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * of the target block containing a non-default case label for the specified source block's switch statement
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addNonDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target);

    /**
     * Adds a directed edge from the specified source basic block to the specified target basic block as a result of
     * of the target block containing the default case label for the specified source block's switch statement
     *
     * @param source the source vertex of the directed edge being added
     * @param target the target vertex of the directed edge being added
     */
    protected abstract void addDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target);

    /**
     * Builds a flow graph representing the vertices and edges added to this creator. Then, resets this creator and
     * returns the built graph.
     *
     * @return a flow graph representing the vertices and edges added to this creator
     */
    protected abstract FlowGraph<BasicBlock> buildGraph();

    /**
     * Adds edges to the graph being created for the based on the specified basic blocks and try-catch blocks
     *
     * @param basicBlocks    a list containing all of the basic blocks for a method in increasing order by the index of the
     *                       first instruction in the block.
     * @param tryCatchBlocks the try catch blocks for the method
     */
    private void addControlFlowEdges(List<BasicBlock> basicBlocks, java.util.List<TryCatchBlockNode> tryCatchBlocks) {
        Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(basicBlocks);
        addStandardEdgeFromEntryPoint(basicBlocks.get(0));
        for(int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock currentBasicBlock = basicBlocks.get(i);
            BasicBlock nextBasicBlock = i < (basicBlocks.size() - 1) ? basicBlocks.get(i + 1) : null;
            AbstractInsnNode lastInsn = currentBasicBlock.getLastInsn();
            if(lastInsn instanceof JumpInsnNode && lastInsn.getOpcode() == GOTO) {
                addUnconditionalJumpEdge(currentBasicBlock, labelBlockMap.get(((JumpInsnNode) lastInsn).label));
            } else if(lastInsn instanceof JumpInsnNode) {
                addBranchTakenEdge(currentBasicBlock, labelBlockMap.get(((JumpInsnNode) lastInsn).label));
                addBranchNotTakenEdge(currentBasicBlock, nextBasicBlock);
            } else if(lastInsn instanceof TableSwitchInsnNode) {
                addDefaultCaseSwitchEdge(currentBasicBlock, labelBlockMap.get(((TableSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((TableSwitchInsnNode) lastInsn).labels) {
                    addNonDefaultCaseSwitchEdge(currentBasicBlock, labelBlockMap.get(label));
                }
            } else if(lastInsn instanceof LookupSwitchInsnNode) {
                addDefaultCaseSwitchEdge(currentBasicBlock, labelBlockMap.get(((LookupSwitchInsnNode) lastInsn).dflt));
                for(LabelNode label : ((LookupSwitchInsnNode) lastInsn).labels) {
                    addNonDefaultCaseSwitchEdge(currentBasicBlock, labelBlockMap.get(label));
                }
            } else if(TaintUtils.isReturnOpcode(lastInsn.getOpcode())) {
                addStandardEdgeToExitPoint(currentBasicBlock);
            } else if(lastInsn.getOpcode() == Opcodes.ATHROW) {
                addExceptionalEdgeToExitPoint(currentBasicBlock);
            } else if(nextBasicBlock != null) {
                addSequentialEdge(currentBasicBlock, nextBasicBlock);
            }
        }
        for(TryCatchBlockNode tryCatch : tryCatchBlocks) {
            addExceptionalEdgeFromEntryPoint(labelBlockMap.get(tryCatch.handler), tryCatch);
        }
    }

    /**
     * @param instructions   a sequence of instructions that form a method
     * @param tryCatchBlocks the try catch blocks for the method
     * @return a list in ascending order of the indices of instructions that are the first instruction of some basic
     * block for the method
     */
    private static int[] calculateLeaders(InsnList instructions, java.util.List<TryCatchBlockNode> tryCatchBlocks) {
        Set<AbstractInsnNode> leaders = new HashSet<>();
        leaders.add(instructions.getFirst()); // First instruction is the leader for the first block
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(insn instanceof JumpInsnNode) {
                leaders.add(((JumpInsnNode) insn).label); // Mark the target of the jump as a leader
                if(insn.getOpcode() != GOTO) {
                    leaders.add(insn.getNext()); // Mark the instruction following the jump as a leader
                }
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
                leaders.add(insn.getNext()); // Mark the instruction following the return as a leader
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
     * @param instruction the instruction to be checked
     * @return true if the specified instruction triggers a method exit
     */
    private static boolean isExitInstruction(AbstractInsnNode instruction) {
        return instruction.getOpcode() == Opcodes.ATHROW || TaintUtils.isReturnOpcode(instruction.getOpcode());
    }

    /**
     * @param basicBlocks a list containing all of the basic blocks for a method in increasing order by the index of the
     *                    first instruction in the block
     * @return a mapping from LabelNodes to the basic block that they start
     */
    private static Map<LabelNode, BasicBlock> createLabelBlockMapping(List<BasicBlock> basicBlocks) {
        Map<LabelNode, BasicBlock> labelBlockMap = new HashMap<>();
        for(BasicBlock block : basicBlocks) {
            AbstractInsnNode insn = block.getFirstInsn();
            if(insn instanceof LabelNode) {
                labelBlockMap.put((LabelNode) insn, block);
            }
        }
        return Collections.unmodifiableMap(labelBlockMap);
    }
}
