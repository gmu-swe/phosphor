package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraph.createLabelBlockMapping;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowNode.*;

public class BindingControlFlowAnalyzer {

    public static void analyze(MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        ControlFlowGraph cfg = ControlFlowGraph.analyze(methodNode);
        cfg.prepareForModification();
        Set<ConvertedEdge> convertedEdges = convertTargetedEdges(cfg);
        if(convertedEdges.isEmpty()) {
            return;
        }
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.calculateDominanceFrontiers();
        Set<ConvertedEdge> bindingEdges = new HashSet<>();
        for(ConvertedEdge convertedEdge : convertedEdges) {
            Set<ControlFlowNode> dominanceFrontier = dominanceFrontiers.get(convertedEdge);
            if(!dominanceFrontier.contains(convertedEdge.destination)) {
                bindingEdges.add(convertedEdge);
            }
        }
        for(ConvertedEdge bindingEdge : bindingEdges) {
            Set<ControlFlowNode> dominanceFrontier = dominanceFrontiers.get(bindingEdge);
            // Add a label and a push to the ControlTaintTagStack
            AbstractInsnNode lastNode = new VarInsnNode(TaintUtils.BRANCH_START, bindingEdge.edgeID);
            instructions.insertBefore(bindingEdge.destination.getFirstInsn(), lastNode);
            if(edgeIsFromTwoOperandBranch(bindingEdge)) {
                instructions.insertBefore(bindingEdge.destination.getFirstInsn(), new VarInsnNode(TaintUtils.BRANCH_START, bindingEdge.edgeID));
            }
            if(bindingEdge.targetLabel != null) {
                LabelNode newLabel = new LabelNode(new Label());
                instructions.insertBefore(lastNode, newLabel);
                replaceLabel(bindingEdge.source.getLastInsn(), bindingEdge.targetLabel, newLabel);
            }
            // Add pops along dominance frontier
            for(ControlFlowNode node : dominanceFrontier) {
                if(node instanceof BasicBlock) {
                    AbstractInsnNode popPoint = ((BasicBlock) node).getFirstInsn();
                    while(popPoint instanceof LabelNode) {
                        popPoint = popPoint.getNext();
                    }
                    instructions.insertBefore(popPoint, new VarInsnNode(TaintUtils.BRANCH_END, bindingEdge.edgeID));
                }
            }
        }

        if(!bindingEdges.isEmpty()) {
            // Add pop all before exiting
            for(BasicBlock basicBlock : cfg.getBasicBlocks()) {
                if(basicBlock.successors.contains(cfg.getExitPoint())) {
                    instructions.insertBefore(basicBlock.getLastInsn(), new VarInsnNode(TaintUtils.BRANCH_END, -1));
                }
            }
        }
    }

    private static void replaceLabel(AbstractInsnNode insn, LabelNode targetLabel, LabelNode replacementLabel) {
        if(insn instanceof JumpInsnNode) {
            JumpInsnNode jInsn = ((JumpInsnNode) insn);
            if(jInsn.label.equals(targetLabel)) {
                jInsn.label = replacementLabel;
            } else {
                throw new IllegalStateException();
            }
        } else if(insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
            List<LabelNode> labels = insn instanceof LookupSwitchInsnNode ? ((LookupSwitchInsnNode) insn).labels : ((TableSwitchInsnNode) insn).labels;
            int targetIndex = 0;
            for(LabelNode label : labels) {
                if(label.equals(targetLabel)) {
                    break;
                }
                targetIndex++;
            }
            if(labels.get(targetIndex).equals(targetLabel)) {
                labels.set(targetIndex, replacementLabel);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static boolean edgeIsFromTwoOperandBranch(ConvertedEdge edge) {
        AbstractInsnNode node = edge.source.getLastInsn();
        switch(node.getOpcode()) {
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ACMPNE:
                return true;
            default:
                return false;
        }
    }

    private static Set<ConvertedEdge> convertTargetedEdges(ControlFlowGraph cfg) {
        Set<ConvertedEdge> convertedEdges = new HashSet<>();
        int edgeID = 0;
        Map<LabelNode, BasicBlock> labelBlockMap = createLabelBlockMapping(cfg.getBasicBlocks());
        for(BasicBlock basicBlock : cfg.getBasicBlocks()) {
            AbstractInsnNode insn = basicBlock.getLastInsn();
            if(insn instanceof JumpInsnNode) {
                LabelNode label = ((JumpInsnNode) insn).label;
                switch(insn.getOpcode()) {
                    case Opcodes.IFEQ:
                    case Opcodes.IF_ICMPEQ:
                    case Opcodes.IF_ACMPEQ:
                        convertedEdges.add(convertEdgeToNode(basicBlock, labelBlockMap.get(label), edgeID++, label));
                        break;
                    case Opcodes.IFNE:
                    case Opcodes.IF_ICMPNE:
                    case Opcodes.IF_ACMPNE:
                        BasicBlock falseBranch = null;
                        for(ControlFlowNode successor : basicBlock.successors) {
                            if(successor instanceof BasicBlock && successor != labelBlockMap.get(label)) {
                                falseBranch = (BasicBlock) successor;
                                break;
                            }
                        }
                        if(falseBranch != null) {
                            convertedEdges.add(convertEdgeToNode(basicBlock, falseBranch, edgeID++, null));
                        }
                }
            } else if(insn instanceof TableSwitchInsnNode) {
                for(LabelNode label : ((TableSwitchInsnNode) insn).labels) {
                    convertedEdges.add(convertEdgeToNode(basicBlock, labelBlockMap.get(label), edgeID++, label));
                }
            } else if(insn instanceof LookupSwitchInsnNode) {
                for(LabelNode label : ((LookupSwitchInsnNode) insn).labels) {
                    convertedEdges.add(convertEdgeToNode(basicBlock, labelBlockMap.get(label), edgeID++, label));
                }
            }
        }
        return convertedEdges;
    }

    /**
     * Replaces the edge from the specified source node to the specified destination node with a new node that connect
     * edges to connect the new node to the source and destination nodes.
     * @param source the source node of the edge to be converted
     * @param destination the destination node of the edge to be converted
     * @param edgeID  the identification number assigned to the new node
     * @param targetLabel if edge is from a jump, the jump target otherwise null
     * @return the new node converted from the specified edge
     */
    private static ConvertedEdge convertEdgeToNode(BasicBlock source, BasicBlock destination, int edgeID, LabelNode targetLabel) {
        if(!isConnected(source, destination)) {
            throw new IllegalArgumentException();
        }
        ConvertedEdge convert = new ConvertedEdge(edgeID, source, destination, targetLabel);
        addEdge(source, convert);
        addEdge(convert, destination);
        removeEdge(source, destination);
        return convert;
    }

    private static class ConvertedEdge extends ControlFlowNode {
        final int edgeID;
        final BasicBlock source;
        final BasicBlock destination;
        final LabelNode targetLabel;

        ConvertedEdge(int edgeID, BasicBlock source, BasicBlock destination, LabelNode targetLabel) {
            this.edgeID = edgeID;
            this.source = source;
            this.destination = destination;
            this.targetLabel = targetLabel;
        }

        @Override
        public String toString() {
            return "ConvertedEdge #" + edgeID;
        }
    }
}
