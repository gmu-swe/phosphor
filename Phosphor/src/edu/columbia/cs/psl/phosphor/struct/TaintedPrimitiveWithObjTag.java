package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class TaintedPrimitiveWithObjTag {

	public Taint taint;
	// Used to mark an instance of this class as visited
	public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

	public abstract Object getValue();

	public Object toPrimitiveType() {
		Object ret = getValue();
		try {
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").set(ret, taint);
		} catch(Exception ex) {
			//
		}
		return ret;
	}
}
