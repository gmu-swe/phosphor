package edu.columbia.cs.psl.phosphor.runtime;


import java.lang.reflect.Array;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.ArrayList;


public class HardcodedBypassStore {
	static ArrayList<Object> vals = new ArrayList<Object>();
	
	public static final Object get(int i)
	{
		synchronized (vals) {
			return vals.get(i);
		}
	}
	public static final Object[] get(int[] i)
	{
		if(i == null)
			return null;
		Object[] ret = (Object[]) Array.newInstance(Configuration.TAINT_TAG_OBJ_CLASS, i.length);
		synchronized (vals) {
			for(int j = 0; j < i.length; j++)
			{
				if(i[j] > 0)
					ret[j] = vals.get(j);
				else
					ret[j] = null;
			}
		}
		return ret;
	}
	public static final int add(Object a)
	{
		synchronized (vals) {
			vals.add(a);
			return vals.size() - 1;
		}
	}
	public static final int[] add(Object a[])
	{
		if(a == null)
			return null;
		int[] ret = new int[a.length];
		synchronized (vals) {
			for(int j = 0; j < a.length; j++)
			{
				if(a[j] != null)
				{
					vals.add(a[j]);
					ret[j] = vals.size()-1;
				}
				else
					ret[j] = 0;
			}
		}
		return ret;
	}
}
