package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class BaseControlFlowGraphCreator extends ControlFlowGraphCreator {

    protected FlowGraphBuilder<BasicBlock> builder = new FlowGraphBuilder<>();

    @Override
    protected void addEntryPoint() {
        builder.addEntryPoint(new EntryPoint());
    }

    @Override
    protected void addExitPoint() {
        builder.addExitPoint(new ExitPoint());
    }

    @Override
    protected BasicBlock addBasicBlock(AbstractInsnNode[] instructions, int index) {
        BasicBlock basicBlock = new SimpleBasicBlock(instructions);
        builder.addVertex(basicBlock);
        return basicBlock;
    }

    @Override
    protected void addEntryExitEdge() {
        builder.addEdge(builder.getEntryPoint(), builder.getExitPoint());
    }

    @Override
    protected void addStandardEdgeFromEntryPoint(BasicBlock target) {
        builder.addEdge(builder.getEntryPoint(), target);
    }

    @Override
    protected void addExceptionalEdgeFromEntryPoint(BasicBlock target, TryCatchBlockNode tryCatchBlockNode) {
        builder.addEdge(builder.getEntryPoint(), target);
    }

    @Override
    protected void addStandardEdgeToExitPoint(BasicBlock source) {
        builder.addEdge(source, builder.getExitPoint());
    }

    @Override
    protected void addExceptionalEdgeToExitPoint(BasicBlock source) {
        builder.addEdge(source, builder.getExitPoint());
    }

    @Override
    protected void addSequentialEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected void addUnconditionalJumpEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected void addBranchTakenEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected void addBranchNotTakenEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected void addNonDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected void addDefaultCaseSwitchEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
    }

    @Override
    protected FlowGraph<BasicBlock> buildGraph() {
        FlowGraph<BasicBlock> graph = builder.build();
        builder = new FlowGraphBuilder<>();
        return graph;
    }
}
