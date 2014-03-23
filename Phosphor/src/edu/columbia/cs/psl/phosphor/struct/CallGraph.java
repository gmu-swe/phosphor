package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class CallGraph implements Serializable {
	private static final long serialVersionUID = 7871097822917675195L;
	java.util.HashMap<String, MethodInformation> graph = new java.util.HashMap<String, MethodInformation>();
	java.util.HashMap<String, MiniClassNode> classInfo = new HashMap<String, MiniClassNode>();

	public void addAll(CallGraph other) {
		this.graph.putAll(other.graph);
		this.classInfo.putAll(other.classInfo);
	}

	public MiniClassNode getClassNode(String name) {
		if (!classInfo.containsKey(name)) {
			MiniClassNode n = new MiniClassNode();
			n.name = name;
			classInfo.put(name, n);
		}
		return classInfo.get(name);
	}

	public MethodInformation getMethodNode(String owner, String name, String desc) {
		if (!graph.containsKey(owner + "." + name + desc)) {
			graph.put(owner + "." + name + desc, new MethodInformation(owner, name, desc));
		}
		return graph.get(owner + "." + name + desc);
	}

	public MethodInformation getMethodNodeIfExists(String owner, String name, String desc) {
		return graph.get(owner + "." + name + desc);
	}

	public void addEdge(String callerOwner, String callerName, String callerDesc, String calleeOwner, String calleeName, String calleeDesc) {
		getMethodNode(callerOwner, callerName, callerDesc).methodsCalled.add(getMethodNode(calleeOwner, calleeName, calleeDesc));
	}

	public Collection<MethodInformation> getMethods() {
		return graph.values();
	}

	public MethodInformation getMethodNodeIfExistsInHierarchy(String owner, String name, String desc) {
		return getMethodNodeIfExistsInHierarchy(owner, name, desc, new HashSet<String>());
	}

	public MethodInformation getMethodNodeIfExistsInHierarchy(String owner, String name, String desc, HashSet<String> tried)
	{
		if(tried.contains(owner))
			return null;
		tried.add(owner);
		if(owner.startsWith("["))
			owner = "java/lang/Object";
		MethodInformation ret = getMethodNodeIfExists(owner, name, desc);
		if (ret != null && ret.isVisited())
			return ret;

		MiniClassNode cn = classInfo.get(owner);
		if (cn == null)
			return null;
		if (cn.superName != null) {
			ret = getMethodNodeIfExistsInHierarchy(cn.superName, name, desc,tried);
			if (ret != null)
				return ret;
		}
		if (cn.interfaces != null)
			for (String s : cn.interfaces) {
				ret = getMethodNodeIfExistsInHierarchy(s, name, desc,tried);
				if (ret != null)
					return ret;
			}
		return null;
	}
	public boolean containsClass(String className) {
		return classInfo.containsKey(className);
	}
}
