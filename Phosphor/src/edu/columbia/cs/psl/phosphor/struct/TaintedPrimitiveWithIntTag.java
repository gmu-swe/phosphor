package edu.columbia.cs.psl.phosphor.struct;

public abstract class TaintedPrimitiveWithIntTag {
	public int taint;
	public abstract Object getValue();
	public final Object toPrimitiveType()
	{
		Object ret = getValue();
		try{
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setInt(ret, taint);
		}catch(Exception ex)
		{
			
		}
		return ret;
	}
}
