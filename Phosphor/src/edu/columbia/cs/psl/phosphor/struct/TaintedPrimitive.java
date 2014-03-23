package edu.columbia.cs.psl.phosphor.struct;

public abstract class TaintedPrimitive {
	public int taint;
	public abstract Object getValue();
	public final Object toPrimitiveType()
	{
		Object ret = getValue();
		try{
			ret.getClass().getDeclaredField("valueINVIVO_PC_TAINT").setAccessible(true);
			ret.getClass().getDeclaredField("valueINVIVO_PC_TAINT").setInt(ret, taint);
		}catch(Exception ex)
		{
			
		}
		return ret;
	}
}
