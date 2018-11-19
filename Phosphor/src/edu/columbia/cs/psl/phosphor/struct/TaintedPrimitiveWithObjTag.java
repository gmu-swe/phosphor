package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class TaintedPrimitiveWithObjTag {
	public Taint taint;
	public abstract Object getValue();
	public Object toPrimitiveType()
	{
		Object ret = getValue();
		try{
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").set(ret, taint);
		}catch(Exception ex)
		{
			
		}
		return ret;
	}
}
