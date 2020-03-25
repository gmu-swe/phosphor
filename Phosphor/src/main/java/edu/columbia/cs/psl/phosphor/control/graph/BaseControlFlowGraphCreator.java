package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Comparator;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

import static edu.columbia.cs.psl.phosphor.control.graph.BasicBlock.getNumericLabelNames;

public class BaseControlFlowGraphCreator extends ControlFlowGraphCreator<BasicBlock> {

    protected FlowGraphBuilder<BasicBlock> builder = new FlowGraphBuilder<>();

    public BaseControlFlowGraphCreator(boolean addExceptionalEdges) {
        super(addExceptionalEdges);
    }

    public BaseControlFlowGraphCreator() {
        this(false);
    }

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
        BasicBlock basicBlock = new SimpleBasicBlock(instructions, index);
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
    protected void addExceptionalEdge(BasicBlock source, BasicBlock target) {
        builder.addEdge(source, target);
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

    public static void main(String[] args) throws Exception {
        File classFile;
        File outputDirectory;
        boolean includeExceptionalEdges;
        try {
            classFile = new File(args[0]);
            outputDirectory = new File(args[1]);
            includeExceptionalEdges = Boolean.parseBoolean(args[2]);
            if(!classFile.isFile() || !(outputDirectory.isDirectory() || outputDirectory.mkdirs())) {
                throw new IllegalArgumentException();
            }
        } catch(Exception e) {
            throw new IllegalArgumentException("Usage: class-file output-directory include-exceptional-edges", e);
        }
        ClassNode classNode = new ClassNode();
        new ClassReader(new FileInputStream(classFile)).accept(classNode, ClassReader.EXPAND_FRAMES);
        int methodNum = 0;
        Comparator<BasicBlock> comparator = (b1, b2) -> {
            if(b1 instanceof EntryPoint || b2 instanceof ExitPoint) {
                return -1;
            } else if(b1 instanceof ExitPoint || b2 instanceof EntryPoint) {
                return 1;
            } else if(b1 instanceof SimpleBasicBlock && b2 instanceof SimpleBasicBlock) {
                return Integer.compare(((SimpleBasicBlock) b1).getIdentifier(), ((SimpleBasicBlock) b2).getIdentifier());
            } else {
                return 0;
            }
        };
        String[] classNameParts = classNode.name.split("/");
        String className = classNameParts[classNameParts.length - 1];
        for(MethodNode mn : classNode.methods) {
            FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator(includeExceptionalEdges)
                    .createControlFlowGraph(mn);
            Map<Label, String> labelNames = getNumericLabelNames(mn.instructions);
            try(PrintWriter writer = new PrintWriter(new File(outputDirectory, String.format("%s%d.gv", mn.name, methodNum)))) {
                cfg.write(writer, String.format("\"%s.%s%s\"", className, mn.name, mn.desc), comparator, (b) -> b.toDotString(labelNames), 20);
                methodNum++;
            }
        }
    }
}
