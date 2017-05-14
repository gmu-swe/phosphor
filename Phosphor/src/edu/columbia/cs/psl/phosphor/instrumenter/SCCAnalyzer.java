package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer.BasicBlock;


/**
 * From https://sites.google.com/site/indy256/algo/scc_tarjan
 * 
 */
public class SCCAnalyzer {
	BasicBlock[] graph;
	boolean[] visited;
	Stack<Integer> stack;
	int time;
	int[] lowlink;
	List<List<BasicBlock>> components;

	public List<List<BasicBlock>> scc(BasicBlock[] graph) {
		int n = graph.length;
		this.graph = graph;
		visited = new boolean[n];
		stack = new Stack<>();
		time = 0;
		lowlink = new int[n];
		components = new ArrayList<>();

		for (int u = 0; u < n; u++)
			if (!visited[u])
				dfs(u);

		return components;
	}

	void dfs(int u) {
		lowlink[u] = time++;
		visited[u] = true;
		if(graph[u] == null)
			return;
		stack.add(u);
		boolean isComponentRoot = true;

		for (BasicBlock v : graph[u].successors) {
			if (!visited[v.idx])
				dfs(v.idx);
			if (lowlink[u] > lowlink[v.idx]) {
				lowlink[u] = lowlink[v.idx];
				isComponentRoot = false;
			}
		}

		if (isComponentRoot) {
			List<BasicBlock> component = new ArrayList<>();
			while (true) {
				int x = stack.pop();
				component.add(graph[x]);
				lowlink[x] = Integer.MAX_VALUE;
				if (x == u)
					break;
			}
			components.add(component);
		}
	}
}