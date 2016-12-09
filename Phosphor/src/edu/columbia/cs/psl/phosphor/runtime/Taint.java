package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class Taint<T> {
	public static boolean IGNORE_TAINTING;

	public static final <T> Taint<T> copyTaint(Taint<T> in)
	{
		if(in == null)
			return null;			
		Taint<T> ret = new Taint<T>();
		ret.copyFrom(in);
		return ret;
	}

	protected void copyFrom(Taint<T> in) {
		if (in.dependencies == null || in.dependencies.getFirst() == null)
			lbl = in.lbl;
		else
			addDependency(in);
	}
	public Taint<T> copy()
	{
		if(IGNORE_TAINTING)
			return this;
		Taint<T> ret = new Taint<T>();
		ret.lbl = lbl;
		ret.dependencies.addAll(dependencies);
		return ret;
	}
//	public Object clone()  {
//		try {
//			Object ret = super.clone();
//			Taint r = (Taint) ret;
//			r.dependencies = (LinkedList<Taint>) dependencies.clone();
//			return ret;
//		} catch (CloneNotSupportedException e) {
//			e.printStackTrace();
//
//			return null;
//		}
//	}
	@Override
	public String toString() {
		String depStr=" deps = [";
		if(dependencies != null)
		{
			Node<T> dep = dependencies.getFirst();
			while(dep != null)
			{
				if(dep.entry != null)
				depStr += dep.entry + " ";
				dep = dep.next;
			}
		}
		depStr += "]";
		return "Taint [lbl=" + lbl + " "+depStr+"]";
	}
	public Object debug;
	public T lbl;
	public LinkedList<T> dependencies;
	public LinkedList<EnqueuedTaint> enqueuedInControlFlow;
	public Taint(T lbl) {
		this.lbl = lbl;
		dependencies = new LinkedList<T>();
	}
	public T getLabel() {
		return lbl;
	}
	public LinkedList<T> getDependencies() {
		return dependencies;
	}
	public Taint(Taint<T> t1)
	{
		dependencies = new LinkedList<T>();
		if(t1 == null)
			return;
		lbl = t1.lbl;
		if(t1.dependencies != null)
			dependencies.addAll(t1.dependencies);
		if(Configuration.derivedTaintListener != null)
			Configuration.derivedTaintListener.singleDepCreated(t1, this);
	}
	public Taint(Taint<T> t1, Taint<T> t2)
	{
		dependencies = new LinkedList<T>();
		if(t1 != null)
		{
			if(t1.lbl != null)
			dependencies.addUnique(t1.lbl);
			dependencies.addAll(t1.dependencies);
		}
		if(t2 != null)
		{
			if(t2.lbl != null)
			dependencies.addUnique(t2.lbl);
			dependencies.addAll(t2.dependencies);
		}
		if(Configuration.derivedTaintListener != null)
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
	}
	public Taint() {
		dependencies = new LinkedList<T>();
	}
	public boolean addDependency(Taint<T> d)
	{
		if(d == null)
			return false;
		boolean added = false;
		if(d.lbl != null)
			added = dependencies.addUnique(d.lbl);
		added |= dependencies.addAll(d.dependencies);
		return added;
	}
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret)
	{
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret)
	{
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}
	public boolean hasNoDependencies() {
		return dependencies.getFirst() == null || dependencies.getFirst().entry == null;
	}
	public static <T> void combineTagsInPlace(Object obj, Taint<T> t1)
	{
		if(obj == null || t1 == null || IGNORE_TAINTING)
			return;
		@SuppressWarnings("unchecked")
		Taint<T> t = (Taint<T>) TaintUtils.getTaintObj(obj);
		if(t == null)
		{
			MultiTainter.taintedObject(obj, t1);
		}
		else
			t.addDependency(t1);
	}
	public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2)
	{
		if(t1 == null && t2 == null)
			return null;
		if(t2 == null)
			return t1;
		if(t1 == null)
			return t2;
		if(t1 == t2)
			return t1;
		if(t1.lbl == null && t1.hasNoDependencies())
			return t2;
		if(t2.lbl == null && t2.hasNoDependencies())
			return t1;
		if(IGNORE_TAINTING)
			return t1;
		Taint<T> r = new Taint<T>(t1,t2);
		if(Configuration.derivedTaintListener != null)
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
		return r;
	}
	@SuppressWarnings("unchecked")
	public static <T> Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags){
		if(t1 == null && tags.isEmpty())
			return null;
		else if(t1 == null)
		{
//			if(tags.isEmpty())
//				return null;
			return tags.taint;
		}
		else if(tags.isEmpty())
		{
//			if(t1.lbl == null && t1.hasNoDependencies())
//				return null;
			return t1;
		}
		else if(t1 == tags.taint)
			return t1;
		if(IGNORE_TAINTING)
			return t1;
		return new Taint<T>((Taint<T>) t1, tags.taint);
	}
	@SuppressWarnings("rawtypes")
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags)
	{
		if(tags.isEmpty() || IGNORE_TAINTING)
			return;
		if(o instanceof TaintedWithObjTag)
		{
			if(o instanceof String)
			{
				Taint onObj  = (Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG();
				((String) o).PHOSPHOR_TAG = Taint.combineTags(onObj, tags);
//				for(int i = 0; i < ((String) o).length(); i++)
//				{
//					((String)o).valuePHOSPHOR_TAG[i] = Taint.combineTags(((String)o).valuePHOSPHOR_TAG[i], tags);
//				}
			}
			else
				((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));
	
		}
	}

}
