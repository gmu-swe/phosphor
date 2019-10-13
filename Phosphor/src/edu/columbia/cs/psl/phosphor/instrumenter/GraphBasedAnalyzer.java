package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer.AnnotatedInstruction;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Set;

public class GraphBasedAnalyzer {

    public static void doGraphAnalysis(MethodNode mn, HashMap<Integer, AnnotatedInstruction> implicitAnalysisBlocks) {
        Graph<AnnotatedInstruction, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        boolean hasJumps = false;
        for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
            if(b.insn instanceof JumpInsnNode) {
                hasJumps = true;
            }
            if(!b.successors.isEmpty()) {
                graph.addVertex(b);
            }
            for(AnnotatedInstruction c : b.successors) {
                graph.addVertex(c);
                graph.addEdge(b, c);
            }
        }
        boolean hadChanges = hasJumps;
        while(hadChanges) {
            hadChanges = false;
            CycleDetector<AnnotatedInstruction, DefaultEdge> detector = new CycleDetector<>(graph);
            for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                if(graph.containsVertex(b)) {
                    Set<AnnotatedInstruction> cycle = detector.findCyclesContainingVertex(b);
                    if(!cycle.isEmpty() && b.successors.size() > 1 && !cycle.containsAll(b.successors)) {
                        graph.removeVertex(b);
                        mn.instructions.insertBefore(b.insn, new InsnNode(TaintUtils.LOOP_HEADER));
                        hadChanges = true;
                    }
                }
            }
        }
    }
}
