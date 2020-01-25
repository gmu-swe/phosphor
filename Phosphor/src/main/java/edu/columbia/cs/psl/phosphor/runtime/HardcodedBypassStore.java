package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.GarbageCollectedArrayList;

public class HardcodedBypassStore {

    private static final GarbageCollectedArrayList<Object> values = new GarbageCollectedArrayList<>();

    static {
        synchronized(values) {
            values.add(null, null);
        }
    }

    private HardcodedBypassStore() {
        // Prevents this class from being instantiated
    }

    public static Object get(int i) {
        // if(i == -1 || i == 0) {
        //     return null;
        // }
        // synchronized(values) {
        //     return values.get(i);
        // }
        return null;
    }

    public static int add(Object taintObjectToPointTo, Object referent) {
        return -1;
        // if(taintObjectToPointTo == null) {
        //     return -1;
        // }
        // synchronized(values) {
        //     return values.add(referent, taintObjectToPointTo);
        // }
    }
}
