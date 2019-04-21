package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.*;
import java.lang.reflect.Array;

public class Taint<T> implements Serializable {

	private static final long serialVersionUID = -2367127733023881176L;
	public static boolean IGNORE_TAINTING;
	// Singleton instance of PowerSetTree used to create new SetNodes
	private static final PowerSetTree setTree = PowerSetTree.getInstance();
	/* Represents the set of labels for this taint object. May be the node representing the empty set. */
	private transient PowerSetTree.SetNode labelSet;

	/* Constructs a new taint object with a null label set. */
	public Taint() {
		this.labelSet = setTree.emptySet();
	}

	/* Constructs a new taint object with only the specified label in its label set. */
	public Taint(T initialLabel) {
		if(initialLabel == null) {
			this.labelSet = setTree.emptySet();
		} else {
			this.labelSet = setTree.makeSingletonSet(initialLabel);
		}
	}

	/* Constructs a new taint object with the same labels as the specified taint object. */
	public Taint(Taint<T> t1) {
		if(t1 != null) {
			this.labelSet = t1.labelSet;
		} else {
			this.labelSet = setTree.emptySet();
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.singleDepCreated(t1, this);
		}
	}

	/* Constructs a new taint object whose label set is the union of the label sets of the two specified taint objects. */
	public Taint(Taint<T> t1, Taint<T> t2) {
		if(t1 != null && t2 != null) {
			this.labelSet = t1.labelSet.union(t2.labelSet);
		} else if(t1 != null) {
			this.labelSet = t1.labelSet;
		} else if(t2.labelSet != null) {
			this.labelSet = t2.labelSet;
		} else {
			this.labelSet = setTree.emptySet();
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
		}
	}

	/* Returns a copy of this taint instance. */
	public Taint<T> copy() {
		if(IGNORE_TAINTING) {
			return this;
		} else {
			Taint<T> ret = new Taint<>();
			ret.labelSet = this.labelSet;
			return ret;
		}
	}

	/* Provides a formatted string representation of this taint's labels. */
	@Override
	public String toString() {
		return "Taint [Labels = [" + labelSet.toList() + "]";
	}

	/* Returns an arr containing this taint's labels. */
	public Object[] getLabels() {
		return labelSet.toList().toArray();
	}

	@SuppressWarnings("unused")
	public Object[] getLabels$$PHOSPHORTAGGED() {
		return getLabels();
	}

	/* Returns an arr containing this taint's labels. The runtime type of the returned array is that of the specified array. */
	@SuppressWarnings("unchecked")
	public T[] getLabels(T[] arr) {
		SimpleLinkedList<Object> list = labelSet.toList();
		if (arr.length < list.size()) {
			arr = (T[]) Array.newInstance(arr.getClass().getComponentType(), list.size());
		}
		int i = 0;
		for(Object label : list) {
			arr[i++] = (T)label;
		}
		return arr;
	}

	@SuppressWarnings("unused")
	public T[] getLabels$$PHOSPHORTAGGED(T[] arr) {
		return getLabels(arr);
	}

	/* Sets this taint object's label set to be the union between this taint object's label set and the specified other
	 * taint object's label set. Returns whether this taint object's label set changed. */
	public boolean addDependency(Taint<T> other) {
		if(other == null) {
			return false;
		}
		PowerSetTree.SetNode union = this.labelSet.union(other.labelSet);
		boolean changed = (this.labelSet != union);
		this.labelSet = union;
		return changed;
	}

	/* Returns whether this taint object's label set is the empty set. */
	public boolean isEmpty() {
		return labelSet.isEmpty();
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret) {
		ret.val = isEmpty();
		ret.taint = null;
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		ret.val = isEmpty();
		ret.taint = null;
		return ret;
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
		if(t == null && t1 != null) {
			MultiTainter.taintedObject(obj, t1.copy());
		} else if(t != null && t1 != null) {
			t.addDependency(t1);
		}
	}

	public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2) {
		if(t1 == null && t2 == null) {
			return null;
		} else if(t2 == null || t2.isEmpty()) {
			return t1;
		} else if(t1 == null || t1.isEmpty()) {
			return t2;
		} else if(t1.equals(t2) || IGNORE_TAINTING) {
			return t1;
		} else if(t1.contains(t2)) {
			return t1;
		} else if(t2.contains(t1)) {
			return t2;
		} else {
			Taint<T> r = t1.copy();
			r.addDependency(t2);
			if(Configuration.derivedTaintListener != null) {
				Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
			}
			return r;
		}
	}

	/* Returns whether the set of labels for the specified taint object is a subset of the set of labels for this taint
	 * object. */
	public boolean contains(Taint<T> that) {
		return that == null || this.labelSet.isSuperset(that.labelSet);
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

	/* Returns whether the set of labels for this taint object contains only the specified labels. */
	public boolean containsOnlyLabels(Object[] labels) {
		if(labels.length != getLabels().length) {
			return false;
		}
		for(Object label : labels) {
			if(!containsLabel(label)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	/* Returns whether the set of labels for this taint object contains the specified label. */
	public boolean containsLabel(Object label) {
		return labelSet.contains(label);
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = containsLabel(label);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = containsLabel(label);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = containsLabel(label);
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		} else {
			Taint<?> taint = (Taint<?>) o;
			return taint.labelSet == this.labelSet;
		}
	}

	@Override
	public int hashCode() {
		return labelSet.hashCode();
	}

	/* Returns a copy of the specified taint object. */
	public static <T> Taint<T> copyTaint(Taint<T> in) {
		return (in == null) ? null : in.copy();
	}

	@SuppressWarnings("unchecked")
	public static <T>  Taint<T> _combineTagsInternal(Taint<T> t1, ControlTaintTagStack tags) {
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
		if(t1 == null || t1.isEmpty()) {
			return tagsTaint;
		} else if(tagsTaint == null || tagsTaint.isEmpty()) {
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

	public static <T>  Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags) {
		if(t1 == null && tags.taint == null && (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty())) {
			return null;
		}
		return _combineTagsInternal(t1,tags);
	}

	/* Returns a new Taint with a label set that is the union of the label sets of the specified taints. */
	public static <T> Taint<T> combineTaintArray(Taint<T>[] taints) {
		if(taints == null) {
			return null;
		}
		Taint<T> result = new Taint<>();
		// The last label set unioned into result's label set
		PowerSetTree.SetNode prevLabelSet = setTree.emptySet();
		for(Taint taint : taints) {
			if(taint != null && taint.labelSet != prevLabelSet) {
				result.labelSet = result.labelSet.union(taint.labelSet);
				prevLabelSet = taint.labelSet;
			}
		}
		return result;
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
		Taint originalPreviousTaint = null;
		for (int i = 0; i < tags.taints.length; i++) {
			if(originalPreviousTaint != null && originalPreviousTaint.equals(tags.taints[i])) {
				tags.taints[i].labelSet = tags.taints[i-1].labelSet;
			} else {
				originalPreviousTaint = tags.taints[i];
				tags.taints[i] = combineTags(tags.taints[i], ctrl);
			}

		}
	}

	/* Saves the Taint instance to the specified stream. */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(this.labelSet.toList());
	}

	/* Rebuilds a Taint instance from the specified stream. */
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.labelSet = setTree.emptySet();
		SimpleLinkedList<Object> list = (SimpleLinkedList<Object>)in.readObject();
		for(Object obj : list) {
			this.labelSet.add(obj);
		}
	}
}