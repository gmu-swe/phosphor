package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.Serializable;
import java.lang.reflect.Array;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public abstract class Taint<T> implements Serializable {

    // Singleton instance of PowerSetTree used to create new SetNodes
    private static PowerSetTree setTree = PowerSetTree.getInstance();
    public static boolean IGNORE_TAINTING = false;

    @SuppressWarnings("unused")
    public TaintedReferenceWithObjTag getLabels$$PHOSPHORTAGGED(Taint referenceTaint, TaintedReferenceWithObjTag ret, Object[] erasedReturn) {
        ret.taint = Taint.emptyTaint();
        ret.val = new LazyReferenceArrayObjTags(getLabels());
        return ret;
    }

    /* Constructs a new taint object whose label set is the union of the label sets of this and the specified taint object. */
    public abstract Taint<T> union(Taint<T> other);

    /* Returns an array containing this taint's labels or label indices if the BitSet representation is used. */
    public abstract Object[] getLabels();

    @SuppressWarnings("unused")
    public TaintedReferenceWithObjTag getLabels$$PHOSPHORTAGGED(T[] arr, Taint tag, TaintedReferenceWithObjTag ret, Object[] erasedReturn) {
        ret.val = new LazyReferenceArrayObjTags(getLabels(arr));
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> Taint<T> withLabel(T label) {
        return setTree.makeSingletonSet(label);
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(Taint referenceTaint, TaintedBooleanWithObjTag ret) {
        ret.val = isEmpty();
        ret.taint = null;
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <E> Taint<E> emptyTaint() {
        return (Taint<E>) setTree.emptySet();
    }

    /* Returns an array containing this taint's labels or label indices if the BitSet representation is used. The runtime
     * type of the returned array is that of the specified array. */
    @SuppressWarnings("unchecked")
    public T[] getLabels(T[] arr) {
        Object[] labels = getLabels();
        if(labels == null) {
            return null;
        } else {
            if(arr.length < labels.length) {
                arr = (T[]) Array.newInstance(arr.getClass().getComponentType(), labels.length);
            }
            int i = 0;
            for(Object label : labels) {
                arr[i++] = (T) label;
            }
            return arr;
        }
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(Taint referenceTaint, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        ret.val = isEmpty();
        ret.taint = null;
        return ret;
    }

    /* Returns whether this taint object's label set is the empty. */
    public abstract boolean isEmpty();

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag isSuperset$$PHOSPHORTAGGED(Taint myRefTaint, Taint<T> that, Taint thatTaint, TaintedBooleanWithObjTag ret) {
        ret.taint = null;
        ret.val = isSuperset(that);
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag isSuperset$$PHOSPHORTAGGED(Taint myRefTaint, Taint<T> that, Taint thatTaint, TaintedBooleanWithObjTag ret, ControlFlowStack ctrl) {
        ret.taint = null;
        ret.val = isSuperset(that);
        return ret;
    }

    /* Returns whether the set of labels for the specified taint object is a subset of the set of labels for this taint
     * object. */
    public abstract boolean isSuperset(Taint<T> other);

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Taint referenceTaint, LazyReferenceArrayObjTags labels, Taint tag, TaintedBooleanWithObjTag ret, Object[] unused) {
        ret.taint = null;
        ret.val = containsOnlyLabels(labels.val);
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Taint referenceTaint, LazyReferenceArrayObjTags labels, Taint tag, TaintedBooleanWithObjTag ret, ControlFlowStack ctrl, Object[] unused) {
        ret.taint = null;
        ret.val = containsOnlyLabels(labels.val);
        return ret;
    }

    /* Returns whether the set of labels for this taint object contains only the specified unique labels. */
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
    public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Taint referenceTaint, Object label, Taint tag, TaintedBooleanWithObjTag ret) {
        ret.taint = null;
        ret.val = containsLabel(label);
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Taint referenceTaint, Object label, Taint tag, TaintedBooleanWithObjTag ret, ControlFlowStack ctrl) {
        ret.taint = null;
        ret.val = containsLabel(label);
        return ret;
    }

    /* Returns whether the set of labels for this taint object contains the specified label. */
    public abstract boolean containsLabel(Object label);

    static <T> Taint<T> copy(Taint<T> in) {
        return in;
    }

    public static <T> TaintedReferenceWithObjTag withLabel$$PHOSPHORTAGGED(T label, Taint tag, TaintedReferenceWithObjTag ret, Taint erasedReturn) {
        ret.taint = Taint.emptyTaint();
        ret.val = withLabel(label);
        return ret;
    }

    @InvokedViaInstrumentation(record = COMBINE_TAGS)
    public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2) {
        if(t1 == Taint.emptyTaint() && t2 == Taint.emptyTaint()) {
            return Taint.emptyTaint();
        } else if(t2 == null || t2.isEmpty()) {
            return t1;
        } else if(t1 == null || t1.isEmpty()) {
            return t2;
        } else if(t1.equals(t2) || IGNORE_TAINTING) {
            return t1;
        } else {
            Taint<T> r = t1.union(t2);
            if(Configuration.derivedTaintListener != null) {
                Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
            }
            return r;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Taint<T> _combineTagsInternal(Taint<T> t1, ControlFlowStack tags) {
        Taint tagsTaint = tags.copyTag();
        if(t1 == null || t1.isEmpty()) {
            return tagsTaint;
        } else if(tagsTaint == null || tagsTaint.isEmpty()) {
            return t1;
        } else if(t1 == tagsTaint) {
            return t1;
        } else if(IGNORE_TAINTING) {
            return t1;
        }
        return tagsTaint.union(t1);
    }

    @InvokedViaInstrumentation(record = COMBINE_TAGS_CONTROL)
    public static <T> Taint<T> combineTags(Taint<T> t1, ControlFlowStack tags) {
        if(tags == null) {
            return t1;
        }
        return _combineTagsInternal(t1, tags);
    }

    /* Returns a new Taint with a label set that is the union of the label sets of the specified taints. */
    public static <T> Taint<T> combineTaintArray(Taint<T>[] taints) {
        if(taints == null) {
            return null;
        } else {
            // SetNode representation is being used
            PowerSetTree.SetNode result = setTree.emptySet();
            // The last label set union'd into result's label set
            PowerSetTree.SetNode prevLabelSet = setTree.emptySet();
            for(Taint taint : taints) {
                PowerSetTree.SetNode node = (PowerSetTree.SetNode) taint;
                if(node != null && node != prevLabelSet) {
                    result = result.union(node);
                    prevLabelSet = node;
                }
            }
            return result;
        }
    }

    @SuppressWarnings("rawtypes")
    @InvokedViaInstrumentation(record = COMBINE_TAGS_ON_OBJECT_CONTROL)
    public static void combineTagsOnObject(Object o, ControlFlowStack tags) {
        if(tags.copyTag().isEmpty() || IGNORE_TAINTING) {
            return;
        }
        if(Configuration.derivedTaintListener != null) {
            Configuration.derivedTaintListener.controlApplied(o, tags);
        }
        if(o instanceof String) {
            combineTagsOnString((String) o, tags);
        } else if(o instanceof TaintedWithObjTag) {
            ((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG(), tags));
        }
    }

    private static void combineTagsOnString(String str, ControlFlowStack ctrl) {
        Taint existing = str.PHOSPHOR_TAG;
        str.PHOSPHOR_TAG = combineTags(existing, ctrl);

        LazyCharArrayObjTags tags = str.valuePHOSPHOR_WRAPPER;
        if (tags == null) {
            str.valuePHOSPHOR_WRAPPER = new LazyCharArrayObjTags(str.value);
            tags = str.valuePHOSPHOR_WRAPPER;
        }
        if (tags.taints == null) {
            tags.taints = new Taint[str.length()];
        }
        // SetNode representation is being used
        Taint originalPreviousTaint = null;
        for(int i = 0; i < tags.taints.length; i++) {
            if(originalPreviousTaint != null && originalPreviousTaint.equals(tags.taints[i])) {
                tags.taints[i] = tags.taints[i - 1];
            } else {
                originalPreviousTaint = tags.taints[i];
                tags.taints[i] = combineTags(tags.taints[i], ctrl);
            }
        }
    }

    public static boolean isEmpty(Taint in) {
        return in == null || in.isEmpty();
    }

}
