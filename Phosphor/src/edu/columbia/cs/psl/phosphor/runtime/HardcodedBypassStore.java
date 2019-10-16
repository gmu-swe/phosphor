package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.GarbageCollectedArrayList;

public class HardcodedBypassStore {
    private static final GarbageCollectedArrayList<Object> vals = new GarbageCollectedArrayList<>();

    static {
        vals.add(null, null);
    }

    public static Object get(int i) {
		if(i == -1 || i == 0) {
			return null;
		}
        synchronized(vals) {
            return vals.get(i);
        }
    }

    public static int add(Object taintObjectToPointTo, Object referent) {
		if(taintObjectToPointTo == null) {
			return -1;
		}
        synchronized(vals) {
            return vals.add(referent, taintObjectToPointTo);
        }
    }
}
