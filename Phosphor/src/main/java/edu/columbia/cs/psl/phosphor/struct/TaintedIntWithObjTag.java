package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedIntWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    static final int cache_low = -127;
    static final int cache_high = 128;
    static final TaintedIntWithObjTag[] cache;
    private static final long serialVersionUID = -7831608795570286818L;

    static {
        cache = new TaintedIntWithObjTag[1 + cache_high - cache_low];
        int j = cache_low;
        for(int k = 0; k < cache.length; k++) {
            cache[k] = new TaintedIntWithObjTag(null, j++);
        }
    }

    public int val;

    public TaintedIntWithObjTag(Taint taint, int val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedIntWithObjTag() {

    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeInt(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readInt();
        taint = (Taint) stream.readObject();
    }

    @Override
    public Object getValue() {
        return val;
    }

    public static final TaintedIntWithObjTag valueOf(Taint taint, int val) {
        if(taint == null && val >= cache_low && val <= cache_high) {
            return cache[val + (-cache_low)];
        }
        return new TaintedIntWithObjTag(taint, val);
    }
}
