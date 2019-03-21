package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.Serializable;
import java.util.Arrays;

public class Taint<T> implements Serializable {
	public static boolean IGNORE_TAINTING;

	public static final <T> Taint<T> copyTaint(Taint<T> in) {
		return in == null ? null : in.copy();
	}

	public Taint<T> copy() {
		if(IGNORE_TAINTING)
			return this;
		Taint<T> ret = new Taint<T>();
		ret.lbl = lbl;
		if(dependencies != null)
			ret.dependencies = dependencies.copy();
		if(tags != null)
			ret.tags = tags.clone();
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
		String depStr = " deps = [" + (dependencies != null ? dependencies.toString() : "") + "]";
		return "Taint [lbl=" + lbl + " " + depStr + "]";
	}

	public transient Object debug;
	public T lbl;
	public SimpleHashSet<T> dependencies;
	public int[] tags;

	public Taint(int startingTag) {
		tags = new int[TAINT_ARRAY_SIZE];
		setBit(startingTag);
	}

	public static int TAINT_ARRAY_SIZE = -1;

	public void setBit(int tag) {
		int bits = tag % 31;
		int key = tag / 31;
		tags[key] |= 1 << bits;
	}

	public boolean hasBitSet(int tag) {
		int bits = tag % 31;
		int key = tag / 31;
		return (tags[key] & (1 << bits)) != 0;
	}

	public void setBits(int[] otherTags) {
		for(int i = 0; i < otherTags.length; i++) {
			tags[i] |= otherTags[i];
		}
	}

	private boolean setBitsIfNeeded(int[] otherTags) {
		boolean changed = false;
		for (int i = 0; i < otherTags.length; i++) {
			if ((tags[i] | otherTags[i]) != tags[i]) {
				tags[i] |= otherTags[i];
				changed = true;
			}
		}
		return changed;
	}

	public Taint(T lbl) {
		this.lbl = lbl;
		dependencies = new SimpleHashSet<T>();
	}

	public T getLabel() {
		return lbl;
	}

	public int[] getTags() {
		return tags;
	}

	@SuppressWarnings("unused")
	public LazyIntArrayObjTags getTags$$PHOSPHORTAGGED() {
		return new LazyIntArrayObjTags(tags);
	}

	@SuppressWarnings("unused")
	public LazyIntArrayObjTags getTags$$PHOSPHORTAGGED(ControlTaintTagStack ctrl) {
		return new LazyIntArrayObjTags(tags);
	}

	public SimpleHashSet<T> getDependencies() {
		return dependencies;
	}

	@SuppressWarnings("unused")
	public SimpleHashSet<T> getDependencies$$PHOSPHORTAGGED() {
		return getDependencies();
	}

	public Taint(Taint<T> t1) {
		if(t1 == null) {
			return;
		}
		if(t1.dependencies != null) {
			dependencies = new SimpleHashSet<>();
			dependencies.addAll(t1.dependencies);
		}
		if(t1.tags != null) {
			tags = new int[t1.tags.length];
			System.arraycopy(t1.tags,0,tags,0,tags.length);
		}
		lbl = t1.lbl;
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.singleDepCreated(t1, this);
		}
	}

	public Taint(Taint<T> t1, Taint<T> t2) {
		if(t1 != null) {
			lbl = t1.lbl;
			if(t1.dependencies != null) {
				dependencies = new SimpleHashSet<>();
				dependencies.addAll(t1.dependencies);
			}
			if(t1.tags != null) {
				tags = new int[t1.tags.length];
				System.arraycopy(t1.tags,0,tags,0,tags.length);
			}
		}
		if(t2 != null) {
			if(t2.lbl != null) {
				lbl = lbl == null ? t2.lbl : lbl; // Set the label to be the second taints label if it was null
				if(!lbl.equals(t2.lbl)) {
					// Create dependencies if it does not already exist
					dependencies = (dependencies == null) ? new SimpleHashSet<T>() : dependencies;
					dependencies.add(t2.lbl);
				}
			}
			if(t2.dependencies != null) {
				// Create dependencies if it does not already exist
				dependencies = (dependencies == null) ? new SimpleHashSet<T>() : dependencies;
				dependencies.addAll(t2.dependencies);
			}
			if(t2.tags != null) {
				if(tags == null) {
					tags = new int[t2.tags.length];
					System.arraycopy(t2.tags, 0, tags, 0, tags.length);
				} else {
					setBits(t2.tags);
				}
			}
		}
		// Make sure that lbl is not also in the dependencies
		if(lbl != null && dependencies != null) {
			dependencies.remove(lbl);
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
		}
	}

	public Taint() {
		if(TAINT_ARRAY_SIZE > 0)
			tags = new int[TAINT_ARRAY_SIZE];
	}

	public boolean addDependency(Taint<T> d) {
		if(d == null) {
			return false;
		}
		if(d.dependencies == null && d.lbl == null && Taint.TAINT_ARRAY_SIZE > 0) {
			// accumulate tags
			if(d.tags == null) {
				return false;
			}
			// Create tags if it does not already exist
			tags = (tags == null) ? new int[Taint.TAINT_ARRAY_SIZE] : tags;
			return setBitsIfNeeded(d.tags);
		}
		// Create dependencies if it does not already exist
		dependencies = (dependencies == null) ? new SimpleHashSet<T>() : dependencies;
		boolean added = false;
		if(d.lbl != null && !d.lbl.equals(lbl)) {
			added = dependencies.add(d.lbl);
		}
		if(!d.hasNoDependencies()) {
			added |= dependencies.addAll(d.dependencies);
		}
		return added;
	}

	public boolean hasNoDependencies() {
		return (dependencies == null || dependencies.isEmpty()) && (tags == null || isEmpty(tags));
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret) {
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}

	private boolean isEmpty(int[] tags) {
		for(int i : tags) {
			if (i != 0) {
				return false;
			}
		}

		return true;
	}

	public static <T> void combineTagsOnArrayInPlace(Object[] ar, Taint<T>[] t1, int dims) {
		combineTagsInPlace(ar, t1[dims-1]);
		if(dims == 1) {
			for(Object o : ar) {
				combineTagsInPlace(o, t1[dims-1]);
			}
		}
		else {
			for(Object o : ar) {
				combineTagsOnArrayInPlace((Object[]) o, t1, dims-1);
			}
		}
	}

	public static <T> void combineTagsInPlace(Object obj, Taint<T> t1) {
		if(obj == null || t1 == null || IGNORE_TAINTING) {
			return;
		}
		_combineTagsInPlace(obj, t1);
	}

	@SuppressWarnings("unchecked")
	public static <T> void _combineTagsInPlace(Object obj, Taint<T> t1) {
		Taint<T> t = (Taint<T>) TaintUtils.getTaintObj(obj);
		if(t == null) {
			MultiTainter.taintedObject(obj, new Taint(t1));
		}
		else {
			t.addDependency(t1);
		}
	}

	public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2) {
		if(t1 == null && t2 == null) {
			return null;
		} else if(t2 == null || (t2.lbl == null && t2.hasNoDependencies())) {
			return t1;
		} else if(t1 == null || (t1.lbl == null && t1.hasNoDependencies())) {
			return t2;
		} else if(t1.equals(t2) || IGNORE_TAINTING) {
			return t1;
		} else if(t1.contains(t2)) {
			return t1;
		} else if(t2.contains(t1)) {
			return t2;
		} else {
			Taint<T> r = new Taint<T>(t1,t2);
			if(Configuration.derivedTaintListener != null) {
				Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
			}
			return r;
		}
	}

	public boolean contains(Taint<T> that) {
		if(that == null) {
			return true;
		}
		if(that.tags != null) {
			for(int i = 0; i < that.tags.length; i++) {
				if((tags[i] | that.tags[i]) != tags[i]) {
					return false;
				}
			}
			return true;
		}
		if(that.lbl != null && (lbl == null || !lbl.equals(that.lbl)) && !dependencies.contains(that.lbl)) {
			return false;
		}
		for(T obj : that.dependencies) {
			if((lbl == null || !lbl.equals(obj)) && !dependencies.contains(obj)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = contains(that);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = contains(that);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = contains(that);
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Taint<?> taint = (Taint<?>) o;

		if (lbl != null ? !lbl.equals(taint.lbl) : taint.lbl != null) return false;
		if (tags != null) {
			return Arrays.equals(tags, taint.tags);
		}
		return dependencies != null ? dependencies.equals(taint.dependencies) : taint.dependencies == null;
	}

	@Override
	public int hashCode() {
		int result = lbl != null ? lbl.hashCode() : 0;
		result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(tags);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> Taint<T> _combineTagsInternal(Taint<T> t1, ControlTaintTagStack tags) {
		if(t1 == null && tags.taint == null && (!Configuration.IMPLICIT_EXCEPTION_FLOW || (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty()))) {
			return null;
		}
		Taint tagsTaint;
		if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
			if((tags.influenceExceptions == null || tags.influenceExceptions.isEmpty())) {
				//Can do a direct check of taint subsumption, no exception data to look at
				if(tags.getTag() == null)
					return t1;
				if(t1 == null)
					return tags.copyTag();
				if(t1.contains(tags.getTag()))
					return t1;
				if(tags.getTag().contains(t1))
					return tags.copyTag();
			}
			tagsTaint = tags.copyTagExceptions();
		} else {
			tagsTaint = tags.copyTag();
		}
		if(t1 == null || (t1.lbl == null && t1.hasNoDependencies())) {
//			if(tags.isEmpty())
//				return null;
			return tagsTaint;
		} else if(tagsTaint == null || (tagsTaint.lbl == null && tagsTaint.hasNoDependencies())) {
//			if(t1.lbl == null && t1.hasNoDependencies())
//				return null;
			return t1;
		} else if(t1 == tagsTaint) {
			return t1;
		}
		if(IGNORE_TAINTING) {
			return t1;
		}
		tagsTaint.addDependency(t1);
		return tagsTaint;
	}

	public static <T> Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags) {
		if(t1 == null && tags.taint == null && (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty())) {
			return null;
		}
		return _combineTagsInternal(t1,tags);
	}

	@SuppressWarnings("rawtypes")
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags) {
		if((tags.isEmpty() || IGNORE_TAINTING) && (!Configuration.IMPLICIT_EXCEPTION_FLOW || (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty()))) {
			return;
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.controlApplied(o, tags);
		}
		if(o instanceof String) {
			combineTagsOnString((String) o, tags);
		} else if(o instanceof TaintedWithObjTag) {
			((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));
		}
	}

	private static void combineTagsOnString(String str, ControlTaintTagStack ctrl) {
		Taint existing = str.PHOSPHOR_TAG;
		str.PHOSPHOR_TAG = combineTags(existing, ctrl);

		LazyCharArrayObjTags tags = str.valuePHOSPHOR_TAG;
		if (tags == null) {
			str.valuePHOSPHOR_TAG = new LazyCharArrayObjTags(str.value);
			tags = str.valuePHOSPHOR_TAG;
		}
		if (tags.taints == null) {
			tags.taints = new Taint[str.length()];
		}
		for (int i = 0; i < tags.taints.length; i++) {
			tags.taints[i] = combineTags(tags.taints[i], ctrl);
		}
	}

}
