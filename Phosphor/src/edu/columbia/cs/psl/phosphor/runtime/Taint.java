package edu.columbia.cs.psl.phosphor.runtime;

import sun.misc.VM;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;

public class Taint implements Cloneable{


	public Object clone()  {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();

			return null;
		}
	}
	@Override
	public String toString() {
		String depStr=" deps = [";
		if(dependencies != null)
		{
			Node<Taint> dep = dependencies.getFirst();
			while(dep != null)
			{
				if(dep.entry.lbl != null)
				depStr += dep.entry.lbl+ " ";
				dep = dep.next;
			}
		}
		depStr += "]";
		return "Taint [lbl=" + lbl + " "+depStr+"]";
	}
	public Object lbl;
	public LinkedList<Taint> dependencies;
	public Taint(Object lbl) {
		this.lbl = lbl;
		dependencies = new LinkedList<Taint>();

		System.out.println("Set taint " + this.lbl);
	}
	public Taint(Taint t1)
	{
		dependencies = new LinkedList<Taint>();
		if(t1 != null && t1.dependencies != null)
			dependencies.addAll(t1.dependencies);
	}
	public Taint(Taint t1, Taint t2)
	{
		dependencies = new LinkedList<Taint>();
		if(t1 != null)
		{
			dependencies.add(t1);
			dependencies.addAll(t1.dependencies);
		}
		if(t2 != null)
		{
			dependencies.add(t2);
			dependencies.addAll(t2.dependencies);
		}
	}
	public Taint() {
		dependencies = new LinkedList<Taint>();

	}
	public boolean addDependency(Taint d)
	{
		return dependencies.addUnique(d);
	}
	public boolean hasNoDependencies() {
		return dependencies.getFirst() == null;
	}
}
