package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;

public class ControlTaintTagStackPool {
	private static final int POOL_SIZE = 100;
	private static ControlTaintTagStack idle;
	private static int pool_pointer;

//	static {
//		idle = new ControlTaintTagStack();
//		for (int i = 1; i < POOL_SIZE; i++) {
//			ControlTaintTagStack n = new ControlTaintTagStack();
//			n.nextEntry = idle;
//			idle = n;
//		}
//	}

	public static ControlTaintTagStack instance() {
//		if (idle == null)
			return new ControlTaintTagStack();
//		ControlTaintTagStack ret = idle;
//		idle = ret.nextEntry;

//		return ret;
	}

	public static void release(ControlTaintTagStack c) {
//		c.reset();
//		c.nextEntry = idle;
//		idle = c;
	}

}
