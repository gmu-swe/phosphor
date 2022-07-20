package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.Serializable;
import java.lang.reflect.Array;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public abstract class Taint<T> implements Serializable {

    public static boolean IGNORE_TAINTING = false;

    /* Constructs a new taint object whose label set is the union of the label sets of this and the specified taint object. */
    public abstract Taint<T> union(Taint<T> other);

    /* Returns an array containing this taint's labels or label indices if the BitSet representation is used. */
    public Object[] getLabels(T[] arr, PhosphorStackFrame phosphorStackFrame) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(getLabels(arr));
        phosphorStackFrame.setWrappedReturn(ret);
        return ret.val;
    }

    @SuppressWarnings("unchecked")
    public static <T> Taint<T> withLabel(T label) {
        return PowerSetTree.getInstance().makeSingletonSet(label);
    }

    @SuppressWarnings("unused")
    public boolean isEmpty(PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        return isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static <E> Taint<E> emptyTaint() {
        return (Taint<E>) PowerSetTree.getInstance().emptySet();
    }

    /* Returns an array containing this taint's labels or label indices if the BitSet representation is used. The runtime
     * type of the returned array is that of the specified array. */
    public abstract Object[] getLabels();

    public Object[] getLabels(PhosphorStackFrame stackFrame){
        stackFrame.returnTaint = Taint.emptyTaint();
        return getLabels();
    }

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
    /* Returns whether this taint object's label set is the empty. */
    public abstract boolean isEmpty();

    @SuppressWarnings("unused")
    public boolean isSuperset(Taint<T> that, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        return isSuperset(that);
    }

    /* Returns whether the set of labels for the specified taint object is a subset of the set of labels for this taint
     * object. */
    public abstract boolean isSuperset(Taint<T> other);

    @SuppressWarnings("unused")
    public boolean containsOnlyLabels(Object[] labels, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        return containsOnlyLabels(labels);
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
    public boolean containsLabel(Object label, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        return containsLabel(label);
    }

    /* Returns whether the set of labels for this taint object contains the specified label. */
    public abstract boolean containsLabel(Object label);

    static <T> Taint<T> copy(Taint<T> in) {
        return in;
    }

    public static <T> Taint withLabel(T label, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        return withLabel(label);
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
    public static <T> Taint<T> combineTags(Taint<T> t1, PhosphorStackFrame stackFrame) {
        if(stackFrame == null || stackFrame.controlFlowTags == null) {
            return t1;
        }
        return _combineTagsInternal(t1, stackFrame.controlFlowTags);
    }

    /* Returns a new Taint with a label set that is the union of the label sets of the specified taints. */
    public static <T> Taint<T> combineTaintArray(Taint<T>[] taints) {
        if(taints == null) {
            return null;
        } else {
            // SetNode representation is being used
            PowerSetTree.SetNode result = PowerSetTree.getInstance().emptySet();
            // The last label set union'd into result's label set
            PowerSetTree.SetNode prevLabelSet = result;
            for(Taint<T> taint : taints) {
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
    public static void combineTagsOnObject(Object o, PhosphorStackFrame frame) {
        if(frame == null){
            return;
        }
        ControlFlowStack tags = frame.controlFlowTags;
        if(tags.copyTag().isEmpty() || IGNORE_TAINTING) {
            return;
        }
        if(Configuration.derivedTaintListener != null) {
            Configuration.derivedTaintListener.controlApplied(o, tags);
        }
        if(o instanceof String) {
            combineTagsOnString((String) o, frame);
        } else if(o instanceof TaintedWithObjTag) {
            ((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG(), frame));
        }
    }

    private static void combineTagsOnString(String str, PhosphorStackFrame stackFrame) {
        Taint existing = InstrumentedJREFieldHelper.getPHOSPHOR_TAG(str);
        InstrumentedJREFieldHelper.setPHOSPHOR_TAG(str, combineTags(existing, stackFrame));

        LazyArrayObjTags tags;
        if(Configuration.IS_JAVA_8){
            tags = InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER(str);
            if (tags == null) {
                LazyCharArrayObjTags newWrapper = new LazyCharArrayObjTags(InstrumentedJREFieldHelper.JAVA_8getvalue(str));
                InstrumentedJREFieldHelper.JAVA_8setvaluePHOSPHOR_WRAPPER(str, newWrapper);
                tags = newWrapper;
            }
        } else{
            tags = InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER(str);
            if (tags == null) {
                LazyByteArrayObjTags newWrapper = new LazyByteArrayObjTags(InstrumentedJREFieldHelper.getvalue(str));
                InstrumentedJREFieldHelper.setvaluePHOSPHOR_WRAPPER(str, newWrapper);
                tags = newWrapper;
            }
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
                tags.taints[i] = combineTags(tags.taints[i], stackFrame);
            }
        }
    }

    public static boolean isEmpty(Taint in) {
        return in == null || in.isEmpty();
    }

}
