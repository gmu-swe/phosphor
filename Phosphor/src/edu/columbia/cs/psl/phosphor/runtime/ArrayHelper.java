package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

public class ArrayHelper {
	private static int engaged = 0;
	private static final native void _setTags(Object obj, int[] tags);
	private static final native int[] _getTags(Object obj);
	
	public static final int[] getTags(Object obj)
	{
		if(obj == null)
			return null;
		int[] ret = null;
		if(engaged == 1)
			ret = _getTags(obj);
		else
			throw new IllegalStateException("JVMTI Agent not loaded");
		if(ret != null)
			return ret;
		//Might need to do somethign on the fly here!
		ret = new int[Array.getLength(obj)];
		_setTags(obj, ret);
		return ret;
	}
	public static final void setTags(Object obj, int[] exp)
	{
		if(engaged == 1)
			_setTags(obj, exp);
		else
			return;
	}

}
