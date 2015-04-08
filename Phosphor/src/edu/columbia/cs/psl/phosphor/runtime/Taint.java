package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

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
	}
	public Object getLabel() {
		return lbl;
	}
	public LinkedList<Taint> getDependencies() {
		return dependencies;
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
	public static Taint combineTags(Taint t1, Taint t2)
	{
		if(t1 == null && t2 == null)
			return null;
		if(t2 == null)
			return t1;
		if(t1 == null)
			return t2;
		if(t1.lbl == null && t1.hasNoDependencies())
			return t2;
		if(t2.lbl == null && t2.hasNoDependencies())
			return t1;
		Taint r = new Taint(t1);
		r.addDependency(t2);
		return r;
	}
	public static Taint combineTags(Taint t1, ControlTaintTagStack tags){
		if(t1 == null && tags.isEmpty())
			return null;
		else if(t1 == null)
		{
			return tags.taint;
		}
		else if(tags.isEmpty())
			return t1;
		return new Taint((Taint) t1, tags.taint);
	}
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags)
	{
		if(o instanceof TaintedWithObjTag)
		{
			((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));
	
		}
	}

}
