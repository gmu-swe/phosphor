package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStoreWithObjTags;
import edu.columbia.cs.psl.phosphor.runtime.HardcodedBypassStore;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class TaintedPrimitiveWithObjTag {

    public Taint taint;
    // Used to mark this object as visited when searching
    public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

    public abstract Object getValue();

    public Object toPrimitiveType() {
        Object val = getValue();
        if(taint == null) {
            return val;
        } else if(val instanceof Boolean) {
            return BoxedPrimitiveStoreWithObjTags.valueOf(taint, (boolean) val);
        } else if(val instanceof Byte) {
            return BoxedPrimitiveStoreWithObjTags.valueOf(taint, (byte) val);
        } else if(val instanceof Character) {
            return BoxedPrimitiveStoreWithObjTags.valueOf(taint, (char) val);
        } else if(val instanceof Short) {
            return BoxedPrimitiveStoreWithObjTags.valueOf(taint, (short) val);
        } else {
            int tag = -1;
            if(val == null) {
                return null;
            }
            try {
                // Set the PHOSPHOR_TAG field if possible
                java.lang.reflect.Field taintField = val.getClass().getDeclaredField("PHOSPHOR_TAG");
                taintField.setAccessible(true);
                if(taintField.getType().equals(Integer.TYPE)) {
                    tag = HardcodedBypassStore.add(taint, val);
                    taintField.setInt(val, tag);
                } else {
                    taintField.set(val, taint);
                }
            } catch(Exception e) {
                //
            }
            try {
                // Set the valuePHOSPHOR_TAG field if possible
                java.lang.reflect.Field valueTaintField = val.getClass().getDeclaredField("valuePHOSPHOR_TAG");
                valueTaintField.setAccessible(true);
                if(valueTaintField.getType().equals(Integer.TYPE)) {
                    if(tag == -1) {
                        tag = HardcodedBypassStore.add(taint, val);
                    }
                    valueTaintField.setInt(val, tag);
                } else {
                    valueTaintField.set(val, taint);
                }
            } catch(Exception e) {
                //
            }
            return val;
        }
    }
}
