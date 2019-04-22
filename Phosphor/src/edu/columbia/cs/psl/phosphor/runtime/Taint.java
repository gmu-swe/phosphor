package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.*;
import java.lang.reflect.Array;

public class Taint<T> implements Serializable {

	private static final long serialVersionUID = -2367127733023881176L;
	public static boolean IGNORE_TAINTING = false;

	// Singleton instance of PowerSetTree used to create new SetNodes
	private static final PowerSetTree setTree = PowerSetTree.getInstance();
	// SetNode representation of the set of labels for this taint object. May be the node representing the empty set.
	private transient PowerSetTree.SetNode labelSet = null;

	// The maximum number of unique labels stored in any given sets if BitSets are used to store the set of labels.
	// If this value is greater than 0 then BitSets are used to store the set of labels for taint instances, otherwise
	// SetNodes are used.
	public static int BIT_SET_CAPACITY = -1;
	// BitSet representation of the set of labels for this taint object.
	private transient BitSet labelBitSet = null;

	/* Constructs a new taint object with an empty label set. */
	public Taint() {
		if(BIT_SET_CAPACITY > 0) {
			this.labelBitSet = new BitSet(BIT_SET_CAPACITY);
		} else {
			this.labelSet = setTree.emptySet();
		}
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
		if(BIT_SET_CAPACITY > 0) {
			this.labelBitSet = (t1 == null) ? new BitSet(BIT_SET_CAPACITY) : t1.labelBitSet.copy();
		} else {
			this.labelSet = (t1 == null) ? setTree.emptySet() : t1.labelSet;
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.singleDepCreated(t1, this);
		}
	}

	/* Constructs a new taint object whose label set is the union of the label sets of the two specified taint objects. */
	public Taint(Taint<T> t1, Taint<T> t2) {
		if(BIT_SET_CAPACITY > 0) {
			this.labelBitSet = (t1 == null) ? new BitSet(BIT_SET_CAPACITY) : t1.labelBitSet.copy();
			this.labelBitSet = (t2 == null) ? this.labelBitSet : BitSet.union(this.labelBitSet, t2.labelBitSet);
		} else {
			this.labelSet = (t1 == null) ? setTree.emptySet() : t1.labelSet;
			this.labelSet = (t2 == null) ? this.labelSet : this.labelSet.union(t2.labelSet);
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
			return new Taint<>(this);
		}
	}

	/* Provides a formatted string representation of this taint's labels or label indices if the BitSet representation is
	 * used. */
	@Override
	public String toString() {
		if(labelSet != null) {
			return "Taint [Labels = [" + labelSet.toList() + "]";
		} else if(labelBitSet != null){
			return "Taint [Label indices = [" + labelBitSet.toList() + "]";
		} else {
			return "Taint []";
		}
	}

	/* Returns an array containing this taint's labels or label indices if the BitSet representation is used. */
	public Object[] getLabels() {
		if(labelSet != null) {
			return labelSet.toList().toArray();
		} else if(labelBitSet != null) {
			return labelBitSet.toList().toArray();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unused")
	public Object[] getLabels$$PHOSPHORTAGGED() {
		return getLabels();
	}

	/* Returns an array containing this taint's labels or label indices if the BitSet representation is used. The runtime
	 * type of the returned array is that of the specified array. */
	@SuppressWarnings("unchecked")
	public T[] getLabels(T[] arr) {
		Object[] labels = getLabels();
		if(labels == null) {
			return null;
		} else {
			if (arr.length < labels.length) {
				arr = (T[]) Array.newInstance(arr.getClass().getComponentType(), labels.length);
			}
			int i = 0;
			for(Object label : labels) {
				arr[i++] = (T)label;
			}
			return arr;
		}
	}

	@SuppressWarnings("unused")
	public T[] getLabels$$PHOSPHORTAGGED(T[] arr) {
		return getLabels(arr);
	}

	/* Sets this taint's label set to be the union between this taint's label set and the specified other
	 * taint's label set. Returns whether this taint's label set changed. */
	public boolean addDependency(Taint<T> other) {
		if(other == null) {
			return false;
		} else if(BIT_SET_CAPACITY > 0) {
			BitSet prev = this.labelBitSet;
			this.labelBitSet = BitSet.union(this.labelBitSet, other.labelBitSet);
			return (prev == null && this.labelBitSet != null) || (prev != null && !prev.equals(this.labelBitSet));
		} else {
			PowerSetTree.SetNode union = this.labelSet.union(other.labelSet);
			boolean changed = (this.labelSet != union);
			this.labelSet = union;
			return changed;
		}
	}

	/* Returns whether this taint object's label set is the empty. */
	public boolean isEmpty() {
		if(labelSet != null) {
			return labelSet.isEmpty();
		} else if(labelBitSet != null) {
			return labelBitSet.isEmpty();
		} else {
			return true;
		}
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
		if(that == null) {
			return true;
		} else if(that.labelSet != null && this.labelSet != null) {
			return this.labelSet.isSuperset(that.labelSet);
		} else if(that.labelBitSet != null && this.labelBitSet != null) {
			return labelBitSet.isSuperset(this.labelBitSet);
		} else {
			return false;
		}
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
		if(label instanceof Integer && labelBitSet != null) {
			return labelBitSet.contains((int)label);
		} else if(labelSet != null) {
			return labelSet.contains(label);
		} else {
			return false;
		}
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
			return taint.labelSet == this.labelSet && ((this.labelBitSet == null && taint.labelBitSet == null) ||
					(this.labelBitSet != null && this.labelBitSet.equals(taint.labelBitSet)));
		}
	}

	@Override
	public int hashCode() {
		int result = (labelSet == null) ? 0 : labelSet.hashCode();
		result = 31 * result + ((labelBitSet == null) ? 0 : labelBitSet.hashCode());
		return result;
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
		} else if(BIT_SET_CAPACITY > 0) {
			Taint<T> result = new Taint<>();
			for(Taint<T> taint : taints) {
				result.addDependency(taint);
			}
			return result;
		} else {
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
		if(BIT_SET_CAPACITY > 0) {
			for (int i = 0; i < tags.taints.length; i++) {
				tags.taints[i] = combineTags(tags.taints[i], ctrl);
			}
		} else {
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
	}

	/* Saves the Taint instance to the specified stream. */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		if(labelSet != null) {
			out.writeObject(labelSet.toList());
		} else if(labelBitSet != null) {
			out.writeObject(labelBitSet.toList());
		} else {
			out.writeObject(new SimpleHashSet<Integer>());
		}
	}

	/* Rebuilds a Taint instance from the specified stream. */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(BIT_SET_CAPACITY > 0) {
			this.labelBitSet = new BitSet(BIT_SET_CAPACITY);
			SimpleLinkedList<?> list = (SimpleLinkedList<?>)in.readObject();
			for(Object obj : list) {
				if(obj instanceof Integer) {
					labelBitSet.add((int) obj);
				}
			}
		} else {
			this.labelSet = setTree.emptySet();
			SimpleLinkedList<?> list = (SimpleLinkedList<?>)in.readObject();
			for(Object obj : list) {
				this.labelSet.add(obj);
			}
		}
	}

	/* Returns the first label in the set. */
	@Deprecated
	@SuppressWarnings("unchecked")
	public T getLabel() {
		return getLabels((T[]) new Object[0])[0];
	}

	/* Returns every label except the first label in the set. */
	@Deprecated
	@SuppressWarnings("unchecked")
	public SimpleHashSet<T> getDependencies() {
		SimpleHashSet<T> set = new SimpleHashSet<>();
		T[] labels = getLabels((T[]) new Object[0]);
		for(int i = 1; i < labels.length; i++) {
			set.add(labels[i]);
		}
		return set;
	}

	@Deprecated
	public SimpleHashSet<T> getDependencies$$PHOSPHORTAGGED() {
		return getDependencies();
	}

	/* Returns whether the label set contains 0 or 1 elements. */
	@Deprecated
	public boolean hasNoDependencies() {
		return isEmpty() || getLabels().length == 1;
	}

	@Deprecated
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret) {
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}

	@Deprecated
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}

	@Deprecated
	public void setBit(int bitIndex) {
		if(labelBitSet != null) {
			labelBitSet.add(bitIndex);
		}
	}

	@Deprecated
	public boolean hasBitSet(int bitIndex) {
		return labelBitSet != null && labelBitSet.contains(bitIndex);
	}

	@Deprecated
	public void setBits(long[] otherPackets) {
		if(labelBitSet != null) {
			labelBitSet.union(new BitSet(otherPackets));
		}
	}

	@Deprecated
	public void setBits(BitSet other) {
		if(labelBitSet != null) {
			labelBitSet.union(new BitSet(other));
		}
	}

	@Deprecated
	public long[] getTags() {
		return (labelBitSet == null) ? null : labelBitSet.getPackets();
	}

	@Deprecated
	public LazyLongArrayObjTags getTags$$PHOSPHORTAGGED() {
		return (labelBitSet == null) ? null : new LazyLongArrayObjTags(labelBitSet.getPackets());
	}

	@Deprecated
	public LazyLongArrayObjTags getTags$$PHOSPHORTAGGED(ControlTaintTagStack ctrl) {
		return (labelBitSet == null) ? null : new LazyLongArrayObjTags(labelBitSet.getPackets());
	}
}