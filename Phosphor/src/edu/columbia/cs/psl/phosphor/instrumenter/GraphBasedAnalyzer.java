package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
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
	public static void doGraphAnalysis(MethodNode mn, HashMap<Integer, PrimitiveArrayAnalyzer.BasicBlock> implicitAnalysisblocks){
		Graph<PrimitiveArrayAnalyzer.BasicBlock, DefaultEdge> graph = new DefaultDirectedGraph<PrimitiveArrayAnalyzer.BasicBlock, DefaultEdge>(DefaultEdge.class);
		boolean hasJumps = false;
		for(PrimitiveArrayAnalyzer.BasicBlock b : implicitAnalysisblocks.values())
		{
			if(b.insn instanceof JumpInsnNode)
				hasJumps = true;
			if(b.successors.size() > 0)
				graph.addVertex(b);
			for(PrimitiveArrayAnalyzer.BasicBlock c : b.successors) {
				graph.addVertex(c);
				graph.addEdge(b, c);
			}
		}
		boolean hadChanges =hasJumps;
		while(hadChanges) {
			hadChanges = false;
			CycleDetector<PrimitiveArrayAnalyzer.BasicBlock, DefaultEdge> detector = new CycleDetector<>(graph);
			for (PrimitiveArrayAnalyzer.BasicBlock b : implicitAnalysisblocks.values()) {
				if (!graph.containsVertex(b))
					continue;
				Set<PrimitiveArrayAnalyzer.BasicBlock> cycle = detector.findCyclesContainingVertex(b);
				if (b.successors.size() > 1 && !cycle.containsAll(b.successors)) {
					graph.removeVertex(b);
					mn.instructions.insertBefore(b.insn, new InsnNode(TaintUtils.LOOP_HEADER));
					hadChanges = true;
				}
			}
		}
	}
}
