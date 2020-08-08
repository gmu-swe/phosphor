package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_REFERENCE_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_REFERENCE_ARRAY_SET;

public final class LazyReferenceArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -4189650314277328488L;

    public Object[] val;

    public LazyReferenceArrayObjTags(int len) {
        // val = new Object[len];
        throw new UnsupportedOperationException();
    }

    public LazyReferenceArrayObjTags(Object[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyReferenceArrayObjTags(Object[] array) {
        this.val = array;
    }

    public LazyReferenceArrayObjTags(Taint lenTaint, Object[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyReferenceArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    @InvokedViaInstrumentation(record = TAINTED_REFERENCE_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, Object val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    public void setUninst(int idx, Object val) {
        this.val[idx] = MultiDTaintedArray.boxOnly1D(val);
    }

    public void set(int idx, Object val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_REFERENCE_ARRAY_GET)
    public TaintedReferenceWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedReferenceWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    //"Uninstrumented" code is allowed to see LazyReferenceArrays, so uses this to retrieve items.
    public Object get(int idx) {
        if(val[idx] instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) val[idx]).getVal();
        }
        return val[idx];
    }

    public TaintedReferenceWithObjTag get(int idx, TaintedReferenceWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? Taint.emptyTaint() : taints[idx];
        return ret;
    }

    public int getLength() {
        return val.length;
    }

    @Override
    public Object getVal() {
        return val;
    }

    public void ensureVal(Object[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(Object el : val) {
                stream.writeObject(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new Object[len]; //TODO probably should serialize the array type then make it correctly?
            TaintedReferenceWithObjTag ret = new TaintedReferenceWithObjTag();
            if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                ControlFlowStack dummy = Configuration.controlFlowManager.getStack(false);
                dummy.disable();
                for(int i = 0; i < len; i++) {
                    val[i] = stream.readObject$$PHOSPHORTAGGED(Taint.emptyTaint(), dummy, ret, null).val; //Need to ensure that this doesn't get unwrapped!
                }
            } else {
                for(int i = 0; i < len; i++) {
                    val[i] = stream.readObject$$PHOSPHORTAGGED(Taint.emptyTaint(), ret, null).val; //Need to ensure that this doesn't get unwrapped!
                }
            }
        }
    }

    public Class getUnderlyingClass() {
        if(val instanceof LazyByteArrayObjTags[]) {
            return byte[][].class;
        }
        if(val instanceof LazyBooleanArrayObjTags[]) {
            return boolean[][].class;
        }
        if(val instanceof LazyCharArrayObjTags[]) {
            return char[][].class;
        }
        if(val instanceof LazyFloatArrayObjTags[]) {
            return float[][].class;
        }
        if(val instanceof LazyShortArrayObjTags[]) {
            return short[][].class;
        }
        if(val instanceof LazyDoubleArrayObjTags[]) {
            return double[][].class;
        }
        if(val instanceof LazyLongArrayObjTags[]) {
            return long[][].class;
        }
        if(val instanceof LazyIntArrayObjTags[]) {
            return int[][].class;
        }
        if(val instanceof LazyReferenceArrayObjTags[]) {
            if(val.length > 0) {
                return Array.newInstance(((LazyReferenceArrayObjTags) val[0]).getUnderlyingClass(), 0).getClass();
            }
            return Object[][].class;
        }
        return val.getClass();
    }

    public static LazyReferenceArrayObjTags factory(Taint referenceTaint, Object[] array) {
        if(array == null) {
            return null;
        }
        return new LazyReferenceArrayObjTags(referenceTaint, array);
    }

    public static Object[] unwrap(LazyReferenceArrayObjTags obj) {
        if(obj != null) {
            return obj.val;
        }
        return null;
    }
}
