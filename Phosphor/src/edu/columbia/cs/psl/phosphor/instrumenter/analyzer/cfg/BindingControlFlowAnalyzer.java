package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.struct.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import org.objectweb.asm.tree.*;

import java.util.List;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphCreator.getBasicBlocks;
import static org.objectweb.asm.Opcodes.*;

public class BindingControlFlowAnalyzer {

    static FlowGraph<ControlFlowNode> cfg;
    public BindingControlFlowAnalyzer() {
        // TODO FIX
    }

    public static int analyze(MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        if(instructions.size() == 0) {
            return 0;
        }
        cfg = ControlFlowGraphCreator.createControlFlowGraph(methodNode);
        Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(getBasicBlocks(cfg));
        Set<BasicBlock> targetedBranchBlocks = identifyTargetedBranchBlocks(getBasicBlocks(cfg));
        if(targetedBranchBlocks.isEmpty()) {
            return -1; // There are no branches that potentially need to be propagated along
        }
        convertTargetedBranchEdges(targetedBranchBlocks, labelBlockMap);
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.getDominanceFrontiers();
        Map<BasicBlock, Set<ConvertedEdge>> bindingEdgesMap = getBindingEdgesMap(targetedBranchBlocks, dominanceFrontiers);
        int branchID = 0;
        for(BasicBlock basicBlock : bindingEdgesMap.keySet()) {
            // Add branch start
            instructions.insertBefore(basicBlock.getLastInsn(), new VarInsnNode(TaintUtils.BRANCH_START, branchID));
            // Add branch ends
            Set<BasicBlock> popPoints = getPopPoints(basicBlock, bindingEdgesMap.get(basicBlock), dominanceFrontiers);
            for(BasicBlock popPoint : popPoints) {
                AbstractInsnNode insn = popPoint.getFirstInsn();
                while(insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE
                        || insn.getType() == AbstractInsnNode.LABEL || insn.getOpcode() > 200) {
                    insn  = insn .getNext();
                }
                instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, branchID));
            }
            branchID++;
        }
        if(!bindingEdgesMap.isEmpty()) {
            // Add pop all before exiting if there was at least one push added
            for(BasicBlock basicBlock : getBasicBlocks(cfg)) {
                if(cfg.getSuccessors(basicBlock).contains(cfg.getExitPoint())) {
                    instructions.insertBefore(basicBlock.getLastInsn(), new VarInsnNode(TaintUtils.BRANCH_END, -1));
                }
            }
        }
        return branchID - 1;
    }
    
    /**
     * @param basicBlocks blocks to be checked for targeted branch instructions
     * @return set containing the basic blocks from the specified array which end with a targeted branch instruction
     */
    private static Set<BasicBlock> identifyTargetedBranchBlocks(ArrayList<BasicBlock> basicBlocks) {
        Set<BasicBlock> targetedBlocks = new HashSet<>();
        for(BasicBlock basicBlock : basicBlocks) {
            switch(basicBlock.getLastInsn().getOpcode()) {
                case IFEQ:
                case IF_ICMPEQ:
                case IF_ACMPEQ:
                case IFNE:
                case IF_ICMPNE:
                case IF_ACMPNE:
                case TABLESWITCH:
                case LOOKUPSWITCH:
                    targetedBlocks.add(basicBlock);
            }
        }
        return targetedBlocks;
    }

    /**
     *  For each targeted block's branch instruction converted, determines which edges from the branch instruction are 
     *  taken as a result of two values being equal. For IFEQ, IF_ICMPEQ, and IF_ACMPEQ this is the "true" edge. For
     *  IFNE, IF_ICMPNE, and IF_ACMPNE this is the "false" edge. For TABLESWITCH and LOOKUPSWITCH this is every 
     *  non-default edge.
     *  
     *  Each such edge (u, v) is converted into a node by removing the edge from the graph, creating a new node w, and 
     *  adding the edges (u, w) and (w, v) to the graph. Sets of these new nodes are gathered for each of the specified
     *  blocks and a mapping between the blocks and these sets is return.
     * 
     * @param targetedBranchBlocks set containing basic blocks in the graph which end with a targeted branch instruction
     * @param labelBlockMap maps labels to the basic blocks that they start                            
     */
    private static void convertTargetedBranchEdges(Set<BasicBlock> targetedBranchBlocks, Map<LabelNode, BasicBlock> labelBlockMap ) {
        for(BasicBlock basicBlock : targetedBranchBlocks) {
            AbstractInsnNode insn = basicBlock.getLastInsn();
            if(insn instanceof JumpInsnNode) {
                boolean branchTaken = insn.getOpcode() == IFEQ || insn.getOpcode() == IF_ICMPEQ || insn.getOpcode() == IF_ACMPEQ;
                convertEdgeToNode(basicBlock, getBinaryBranchSuccessor(basicBlock, labelBlockMap, branchTaken));
            } else if(insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                List<LabelNode> labels = insn instanceof TableSwitchInsnNode ? ((TableSwitchInsnNode) insn).labels : ((LookupSwitchInsnNode) insn).labels;
                // Make a set of labels to handle multiple cases going to the same branch
                Set<LabelNode> visited = new HashSet<>();
                for(LabelNode label : labels) {
                    if(visited.add(label)) {
                        convertEdgeToNode(basicBlock, labelBlockMap.get(label));
                    }
                }
            }
        }
    }

    /**
     * @param basicBlock basic block that ends with a binary branch instruction whose successor is to be returned
     * @param labelBlockMap maps labels to the basic blocks that they start
     * @param branchTaken whether the successor returns should be the one that follows the specified block if the branch is taken
     * @return return a successor of the specified basic block with two successor, if branchTaken is true return the successor
     *              that follows if the branch ending the specified block is taken otherwise returns the successor that
     *              follows if the branch is not taken
     */
    private static ControlFlowNode getBinaryBranchSuccessor(BasicBlock basicBlock, Map<LabelNode, BasicBlock> labelBlockMap, boolean branchTaken) {
        if(cfg.getSuccessors(basicBlock).size() != 2) {
            throw new IllegalArgumentException("Cannot find binary branch target for basic block that does not have two successors");
        } else {
            AbstractInsnNode insn = basicBlock.getLastInsn();
            if(insn instanceof JumpInsnNode) {
                LabelNode label = ((JumpInsnNode) insn).label;
                if(branchTaken) {
                    return labelBlockMap.get(label);
                } else {
                    ControlFlowNode[] successors = cfg.getSuccessors(basicBlock).toArray(new ControlFlowNode[0]);
                    return successors[0] == labelBlockMap.get(label) ? successors[1] : successors[0];
                }
            } else {
                throw new IllegalArgumentException("Cannot get branch target for a basic block that does not end with a jump insn");
            }
        }
    }

    /**
     * Removes the edge (source, destination) from the graph. Adds a new node w, the edge (source, w) and the edge
     * (w, destination) to the graph.
     *
     * @param source the source node of the edge to be converted
     * @param destination the destination node of the edge to be converted
     */
    private static void convertEdgeToNode(ControlFlowNode source, ControlFlowNode destination) {
//        if(isConnected(source, destination)) {
//            removeEdge(source, destination);
//        }
//        ConvertedEdge convert = new ConvertedEdge(destination);
//        addEdge(source, convert);
//        addEdge(convert, destination);
    }

    /**
     * @param targetedBranchBlocks set containing basic blocks in the graph which end with a targeted branch instruction
     * @param dominanceFrontiers maps nodes in the graph to their dominance frontiers
     * @return a mapping from basic blocks that end with a targeted branch instruction to non-empty sets of converted
     *              edges from the block that correspond to edges in the original graph which are "binding" for at least
     *              one statement in the original graph.
     */
    private static Map<BasicBlock, Set<ConvertedEdge>> getBindingEdgesMap(Set<BasicBlock> targetedBranchBlocks,
                                                                          Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers) {
        Map<BasicBlock, Set<ConvertedEdge>> bindingEdgesMap = new HashMap<>();
        for(BasicBlock basicBlock : targetedBranchBlocks) {
            Set<ConvertedEdge> bindingEdges = new HashSet<>();
            for(ControlFlowNode successor : cfg.getSuccessors(basicBlock)) {
                if(isBindingConvertedEdge(successor, dominanceFrontiers)) {
                    bindingEdges.add((ConvertedEdge) successor);
                }
            }
            if(!bindingEdges.isEmpty()) {
                bindingEdgesMap.put(basicBlock, bindingEdges);
            }
        }
        return bindingEdgesMap;
    }

    /**
     * @param node the node to be tested
     * @param dominanceFrontiers maps nodes in the graph to their dominance frontiers
     * @return true if the specified node is a converted edge that corresponds to an edge in the original graph which
     *              is "binding" for at least one statement in the original graph
     */
    private static boolean isBindingConvertedEdge(ControlFlowNode node, Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers) {
        if(node instanceof ConvertedEdge && dominanceFrontiers.containsKey(node)) {
            ConvertedEdge convertedEdge = (ConvertedEdge) node;
            Set<ControlFlowNode> dominanceFrontier = dominanceFrontiers.get(convertedEdge);
            return !dominanceFrontier.contains(convertedEdge.destination);
        }
        return false;
    }

    /**
     * @param bindingBranch a basic block that end with a target branch instruction that has at least one edge that is
     *                      "binding" for at least one statement in the original graph
     * @param bindingEdges non-empty set of converted edges that correspond to edges in the original graph which are
     *                      "binding" for at least one statement in the original graph
     * @param dominanceFrontiers maps nodes in the graph to their dominance frontiers
     * @return a set of BasicBlocks that should have a pop instruction inserted at the beginning for pushes associated
     *                      with the specified branch
     */
    private static Set<BasicBlock> getPopPoints(BasicBlock bindingBranch, Set<ConvertedEdge> bindingEdges, Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers) {
        Set<BasicBlock> popPoints = new HashSet<>();
        // Add pops along dominance frontiers of each of the binding branches for the branch
        for(ConvertedEdge convertedEdge : bindingEdges) {
            for(ControlFlowNode node : dominanceFrontiers.get(convertedEdge)) {
                if(node instanceof BasicBlock) {
                    popPoints.add((BasicBlock) node);
                }
            }
        }
        // Add pops before all of the non-binding routes out of the branch
        for(ControlFlowNode node : cfg.getSuccessors(bindingBranch)) {
            if(node instanceof BasicBlock) {
                popPoints.add((BasicBlock) node);
            } else if(node instanceof ConvertedEdge && !bindingEdges.contains(node)) {
                ControlFlowNode target = ((ConvertedEdge) node).destination;
                if(target instanceof  BasicBlock) {
                    popPoints.add((BasicBlock) target);
                }
            }
        }
        return popPoints;
    }

    /**
     * @param blocks a list of basic blocks for an instruction sequence
     * @return a mapping from LabelNodes to the basic block that they start
     */
    private static Map<LabelNode, BasicBlock> createLabelBlockMapping(ArrayList<BasicBlock> blocks) {
        Map<LabelNode, BasicBlock> labelBlockMap = new HashMap<>();
        for(BasicBlock block : blocks) {
            AbstractInsnNode insn = block.getFirstInsn();
            if(insn instanceof LabelNode) {
                labelBlockMap.put((LabelNode) insn, block);
            }
        }
        return labelBlockMap;
    }

    private static class ConvertedEdge extends ControlFlowNode {
        final ControlFlowNode destination;

        ConvertedEdge(ControlFlowNode destination) {
            this.destination = destination;
        }
    }
}
