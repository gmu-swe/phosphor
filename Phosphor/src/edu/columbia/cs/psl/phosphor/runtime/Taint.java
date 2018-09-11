package edu.columbia.cs.psl.phosphor.runtime;

import java.io.Serializable;
import java.util.Objects;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;

public class Taint<T> implements Serializable {
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
		if(in == null)
			return;
		else if (in.dependencies == null || in.dependencies.isEmpty())
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
			depStr += dependencies.toString();
		}
		depStr += "]";
		return "Taint [lbl=" + lbl + " "+depStr+"]";
	}
	public transient Object debug;
	public T lbl;
	public SimpleHashSet<T> dependencies;
	
	public Taint(T lbl) {
		this.lbl = lbl;
		dependencies = new SimpleHashSet<T>();
	}
	public T getLabel() {
		return lbl;
	}
	public SimpleHashSet<T> getDependencies() {
		return dependencies;
	}
	public Taint(Taint<T> t1)
	{
		dependencies = new SimpleHashSet<>();
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
		dependencies = new SimpleHashSet<>();
		if(t2 == null && t1 != null)
		{
			lbl = t1.lbl;
			if(t1.dependencies != null)
				dependencies.addAll(t1.dependencies);
		}
		else if(t1 == null && t2 != null)
		{
			lbl = t2.lbl;
			if(t2.dependencies != null)
				dependencies.addAll(t2.dependencies);
		} else {
			if (t1 != null)

			{
				if (t1.lbl != null)
					dependencies.add(t1.lbl);
				dependencies.addAll(t1.dependencies);
			}
			if (t2 != null) {
				if (t2.lbl != null)
					dependencies.add(t2.lbl);
				dependencies.addAll(t2.dependencies);
			}
		}
		if(Configuration.derivedTaintListener != null)
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
	}
	public Taint() {
		dependencies = new SimpleHashSet<>();
	}
	public boolean addDependency(Taint<T> d)
	{
		if(d == null)
			return false;
		boolean added = false;
		if(d.lbl != null)
			added = dependencies.add(d.lbl);
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
		return dependencies.isEmpty();
	}
	public static <T> void combineTagsOnArrayInPlace(Object[] ar, Taint<T>[] t1, int dims)
	{
		combineTagsInPlace(ar, t1[dims-1]);
		if(dims == 1)
		{
			for(Object  o : ar)
			{
				combineTagsInPlace(o, t1[dims-1]);
			}
		}
		else
		{
			for(Object o : ar)
				combineTagsOnArrayInPlace((Object[]) o, t1, dims-1);
		}
	}
	public static <T> void combineTagsInPlace(Object obj, Taint<T> t1)
	{
		if(obj == null || t1 == null || IGNORE_TAINTING)
			return;
		@SuppressWarnings("unchecked")
		Taint<T> t = (Taint<T>) TaintUtils.getTaintObj(obj);
		if(t == null)
		{
			MultiTainter.taintedObject(obj, new Taint(t1));
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Taint<?> taint = (Taint<?>) o;

		if (debug != null ? !debug.equals(taint.debug) : taint.debug != null) return false;
		if (lbl != null ? !lbl.equals(taint.lbl) : taint.lbl != null) return false;
		return dependencies != null ? dependencies.equals(taint.dependencies) : taint.dependencies == null;
	}

	@Override
	public int hashCode() {
		int result = debug != null ? debug.hashCode() : 0;
		result = 31 * result + (lbl != null ? lbl.hashCode() : 0);
		result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
		return result;
	}

	public static <T> Taint<T> _combineTagsInternal(Taint<T> t1, ControlTaintTagStack tags){
		if(t1 == null && tags.isEmpty())
			return null;
		else if(t1 == null || (t1.lbl == null && t1.dependencies.isEmpty()))
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
		Taint ret = t1.copy();
		ret.addDependency(tags.taint);
		return ret;
	}
	@SuppressWarnings("unchecked")
	public static <T> Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags){
		if(t1 == null && tags.taint == null)
			return null;
		return _combineTagsInternal(t1,tags);
	}
	@SuppressWarnings("rawtypes")
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags)
	{
		if(tags.isEmpty() || IGNORE_TAINTING)
			return;
		if(Configuration.derivedTaintListener != null)
			Configuration.derivedTaintListener.controlApplied(o, tags);
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
