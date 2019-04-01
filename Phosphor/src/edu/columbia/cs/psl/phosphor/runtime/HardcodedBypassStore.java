package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.GarbageCollectedArrayList;

public class HardcodedBypassStore {
	static GarbageCollectedArrayList<Object> vals = new GarbageCollectedArrayList<>();

	static {
		vals.add(null);
	}

	public static final Object get(int i) {
		if (i == -1 || i == 0)
			return null;
		synchronized (vals) {
			return vals.get(i);
		}
	}

	public static final int add(Object a) {
		if (a == null)
			return -1;
		synchronized (vals) {
			return vals.add(a);
		}
	}

	public static final void free(int i) {
		if (i >= 0) {
			synchronized (vals) {
				vals.free(i);
			}
		}
	}
}
