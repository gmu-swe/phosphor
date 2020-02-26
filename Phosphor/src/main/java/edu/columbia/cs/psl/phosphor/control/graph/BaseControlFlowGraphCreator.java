package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.instrumenter.PhosphorTextifier;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class BaseControlFlowGraphCreator extends ControlFlowGraphCreator {

    protected FlowGraphBuilder<BasicBlock> builder = new FlowGraphBuilder<>();

    public BaseControlFlowGraphCreator(boolean addExceptionalEdges) {
        super(addExceptionalEdges);
    }

    public BaseControlFlowGraphCreator() {
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
        if(args.length != 2) {
            throw new IllegalArgumentException("Usage: class_file output_file");
        }
        ClassReader cr = new ClassReader(new FileInputStream(args[0]));
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        Printer printer = new PhosphorTextifier();
        TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
        try(PrintWriter writer = new PrintWriter(args[1])) {
            writer.println("[");
            int methodNum = 0;
            for(MethodNode mn : classNode.methods) {
                writer.println("\t{");
                writer.println("\t\t\"name\": \"" + mn.name + "\",");
                writer.println("\t\t\"desc\": \"" + mn.desc + "\",");
                writer.println("\t\t\"vertices\": [");
                FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator().createControlFlowGraph(mn);
                Map<BasicBlock, Integer> blockNumbers = new HashMap<>();
                int nextBlockNum = 0;
                List<BasicBlock> blocks = new LinkedList<>(cfg.getVertices());
                Collections.sort(blocks, (b1, b2) -> {
                    if(b1 instanceof EntryPoint || b2 instanceof ExitPoint) {
                        return -1;
                    } else if(b1 instanceof ExitPoint || b2 instanceof EntryPoint) {
                        return 1;
                    }
                    AbstractInsnNode insn1 = b1.getFirstInsn();
                    AbstractInsnNode insn2 = b2.getFirstInsn();
                    if(insn1 == null && insn2 == null) {
                        return 0;
                    } else if(insn1 == null) {
                        return 1;
                    } else if(insn2 == null) {
                        return -1;
                    } else {
                        return Integer.compare(mn.instructions.indexOf(insn1), mn.instructions.indexOf(insn2));
                    }
                });
                for(BasicBlock vertex : blocks) {
                    writer.println("\t\t\t{");
                    writer.println("\t\t\t\t\"id\": \"" + nextBlockNum + "\",");
                    StringBuilder label = new StringBuilder();
                    if(vertex instanceof EntryPoint) {
                        label = new StringBuilder("ENTRY");
                    } else if(vertex instanceof ExitPoint) {
                        label = new StringBuilder("EXIT");
                    } else if(vertex.getFirstInsn() != null) {
                        AbstractInsnNode insn = vertex.getFirstInsn();
                        do {
                            label.append(insnToString(tmv, printer, insn).replace("\"", "\\\""));
                            if(insn == vertex.getLastInsn()) {
                                break;
                            }
                            insn = insn.getNext();
                        } while(insn != null);
                    }
                    blockNumbers.put(vertex, nextBlockNum++);
                    writer.println("\t\t\t\t\"label\": \"" + label.toString().replace("\n\n", "\\\\n")
                            .replace("\n", "\\\\n") + "\"");
                    if(nextBlockNum == cfg.getVertices().size()) {
                        writer.println("\t\t\t}");
                    } else {
                        writer.println("\t\t\t},");

                    }
                }
                writer.println("\t\t],");
                writer.println("\t\t\"edges\": [");
                List<String> edges = new LinkedList<>();
                for(BasicBlock vertex : cfg.getVertices()) {
                    for(BasicBlock successor : cfg.getSuccessors(vertex)) {
                        edges.add("\t\t\t[" + "\n\t\t\t\t\"" + blockNumbers.get(vertex) + "\"," + "\n\t\t\t\t\""
                                + blockNumbers.get(successor) + "\"" + "\n\t\t\t]");
                    }
                }
                writer.println(String.join(",\n", edges));
                writer.println("\t\t]");
                if(++methodNum == classNode.methods.size()) {
                    writer.println("\t}");
                } else {
                    writer.println("\t},");
                }
            }
            writer.println("]");
        }
    }

    private static String insnToString(TraceMethodVisitor tmv, Printer printer, AbstractInsnNode insn) {
        insn.accept(tmv);
        StringWriter stringWriter = new StringWriter();
        printer.print(new PrintWriter(stringWriter));
        printer.getText().clear();
        return stringWriter.toString();
    }
}
