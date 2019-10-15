package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.*;

public class BindingControlFlowAnalyzer {

    /**
     * @param methodNode the method to be analyzed and possibly modified
     * @return the number of unique branch ids used in added instructions
     */
    public static int analyzeAndModify(MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        if(instructions.size() == 0) {
            return 0;
        }
        BindingControlFlowGraphCreator creator = new BindingControlFlowGraphCreator();
        FlowGraph<BasicBlock> controlFlowGraph = creator.createControlFlowGraph(methodNode);
        Set<BasicBlock> targetedBranchBlocks = creator.getTargetedBranchBlocks();
        if(targetedBranchBlocks.isEmpty()) {
            return 0; // There are no branches that potentially need to be propagated along
        }
        Map<BasicBlock, Set<ConvertedEdge>> bindingEdgesMap = getBindingEdgesMap(targetedBranchBlocks, controlFlowGraph);
        int branchID = 0;
        for(BasicBlock basicBlock : bindingEdgesMap.keySet()) {
            // Add branch start
            instructions.insertBefore(basicBlock.getLastInsn(), new VarInsnNode(TaintUtils.BRANCH_START, branchID));
            // Add branch ends
            Set<BasicBlock> popPoints = getPopPoints(basicBlock, bindingEdgesMap.get(basicBlock), controlFlowGraph);
            for(BasicBlock popPoint : popPoints) {
                AbstractInsnNode insn = popPoint.getFirstInsn();
                while(insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE
                        || insn.getType() == AbstractInsnNode.LABEL || insn.getOpcode() > 200) {
                    insn = insn.getNext();
                }
                instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, branchID));
            }
            branchID++;
        }
        if(!bindingEdgesMap.isEmpty()) {
            // Add pop all before exiting if there was at least one push added
            for(BasicBlock basicBlock : controlFlowGraph.getVertices()) {
                if(controlFlowGraph.getSuccessors(basicBlock).contains(controlFlowGraph.getExitPoint())) {
                    instructions.insertBefore(basicBlock.getLastInsn(), new VarInsnNode(TaintUtils.BRANCH_END, -1));
                }
            }
        }
        return branchID;
    }

    /**
     * @param targetedBranchBlocks set containing basic blocks in the graph which end with a targeted branch instruction
     * @param controlFlowGraph     the control flow graph for an analyzed method
     * @return a mapping from basic blocks that end with a targeted branch instruction to non-empty sets of converted
     * edges from the block that correspond to edges in the original graph which are "binding" for at least
     * one statement in the original graph.
     */
    private static Map<BasicBlock, Set<ConvertedEdge>> getBindingEdgesMap(Set<BasicBlock> targetedBranchBlocks, FlowGraph<BasicBlock> controlFlowGraph) {
        Map<BasicBlock, Set<ConvertedEdge>> bindingEdgesMap = new HashMap<>();
        for(BasicBlock basicBlock : targetedBranchBlocks) {
            Set<ConvertedEdge> bindingEdges = new HashSet<>();
            for(BasicBlock successor : controlFlowGraph.getSuccessors(basicBlock)) {
                if(isBindingConvertedEdge(successor, controlFlowGraph.getDominanceFrontiers())) {
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
     * @param node               the node to be tested
     * @param dominanceFrontiers maps nodes in the graph to their dominance frontiers
     * @return true if the specified node is a converted edge that corresponds to an edge in the original graph which
     * is "binding" for at least one statement in the original graph
     */
    private static boolean isBindingConvertedEdge(BasicBlock node, Map<BasicBlock, Set<BasicBlock>> dominanceFrontiers) {
        if(node instanceof ConvertedEdge && dominanceFrontiers.containsKey(node)) {
            ConvertedEdge convertedEdge = (ConvertedEdge) node;
            Set<BasicBlock> dominanceFrontier = dominanceFrontiers.get(convertedEdge);
            return !dominanceFrontier.contains(convertedEdge.destination);
        }
        return false;
    }

    /**
     * @param bindingBranch    a basic block that end with a target branch instruction that has at least one edge that is
     *                         "binding" for at least one statement in the original graph
     * @param bindingEdges     non-empty set of converted edges that correspond to edges in the original graph which are
     *                         "binding" for at least one statement in the original graph
     * @param controlFlowGraph the control flow graph for an analyzed method
     * @return a set of BasicBlocks that should have a pop instruction inserted at the beginning for pushes associated
     * with the specified branch
     */
    private static Set<BasicBlock> getPopPoints(BasicBlock bindingBranch, Set<ConvertedEdge> bindingEdges, FlowGraph<BasicBlock> controlFlowGraph) {
        Set<BasicBlock> popPoints = new HashSet<>();
        // Add pops along dominance frontiers of each of the binding branches for the branch
        for(ConvertedEdge convertedEdge : bindingEdges) {
            for(BasicBlock node : controlFlowGraph.getDominanceFrontiers().get(convertedEdge)) {
                if(!(node instanceof DummyBasicBlock)) {
                    popPoints.add(node);
                }
            }
        }
        // Add pops before all of the non-binding routes out of the branch
        for(BasicBlock node : controlFlowGraph.getSuccessors(bindingBranch)) {
            if(!(node instanceof DummyBasicBlock)) {
                popPoints.add(node);
            } else if(node instanceof ConvertedEdge && !bindingEdges.contains(node)) {
                BasicBlock target = ((ConvertedEdge) node).destination;
                if(!(target instanceof DummyBasicBlock)) {
                    popPoints.add(target);
                }
            }
        }
        return popPoints;
    }

    private static class ConvertedEdge extends DummyBasicBlock {

        final BasicBlock destination;

        ConvertedEdge(BasicBlock destination) {
            this.destination = destination;
        }
    }

    /**
     * Builds a control flow graph with the following edges (u, v) replaced with a node w, an edge (u, w) and an edge
     * (w, v):
     * <ol>
     *     <li>Any edge from a TABLESWITCH or LOOKUPSWITCH instruction and any of its jump targets</li>
     *     <li>Any edge from a IFEQ, IF_ICMPEQ, or IF_ACMPEQ instruction and its jump target</li>
     *     <li>Any edge from a IFNE, IF_ICMPNE, or IF_ACMPNE instruction and the instruction that follows it in
     *     sequential execution</li>
     * </ol>
     */
    private static class BindingControlFlowGraphCreator extends BaseControlFlowGraphCreator {

        Map<BasicBlock, Set<BasicBlock>> targetedBranchEdges = new HashMap<>();

        @Override
        protected void addBranchTakenEdge(BasicBlock source, BasicBlock target) {
            switch(source.getLastInsn().getOpcode()) {
                case IFEQ:
                case IF_ICMPEQ:
                case IF_ACMPEQ:
                    addTargetedEdge(source, target);
            }
            super.addBranchTakenEdge(source, target);
        }

        @Override
        protected void addBranchNotTakenEdge(BasicBlock source, BasicBlock target) {
            switch(source.getLastInsn().getOpcode()) {
                case IFNE:
                case IF_ICMPNE:
                case IF_ACMPNE:
                    addTargetedEdge(source, target);
            }
            super.addBranchNotTakenEdge(source, target);
        }

        @Override
        protected void addNonDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target) {
            addTargetedEdge(source, target);
            super.addNonDefaultCaseSwitchEdge(source, target);
        }

        @Override
        protected void addDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target) {
            addTargetedEdge(source, target);
            super.addDefaultCaseSwitchEdge(source, target);
        }

        private void addTargetedEdge(BasicBlock source, BasicBlock target) {
            if(!targetedBranchEdges.containsKey(source)) {
                targetedBranchEdges.put(source, new HashSet<BasicBlock>());
            }
            targetedBranchEdges.get(source).add(target);
        }

        @Override
        protected FlowGraph<BasicBlock> buildGraph() {
            // Replace targeted edges
            for(BasicBlock source : targetedBranchEdges.keySet()) {
                for(BasicBlock target : targetedBranchEdges.get(source)) {
                    convertEdgeToNode(source, target);
                }
            }
            return super.buildGraph();
        }

        /**
         * Removes the edge (source, target) from the graph. Adds a new node w, the edge (source, w) and the edge
         * (w, target) to the graph.
         *
         * @param source the source node of the edge to be converted
         * @param target the target node of the edge to be converted
         */
        private void convertEdgeToNode(BasicBlock source, BasicBlock target) {
            builder.removeEdge(source, target);
            ConvertedEdge convert = new ConvertedEdge(target);
            builder.addEdge(source, convert);
            builder.addEdge(convert, target);
        }

        /**
         * @return set of basic blocks that were the source of an edge replaced by this creator
         */
        private Set<BasicBlock> getTargetedBranchBlocks() {
            return targetedBranchEdges.keySet();
        }
    }
}
