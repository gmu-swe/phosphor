package edu.columbia.cs.psl.phosphor.runtime;


import edu.columbia.cs.psl.phosphor.struct.ArrayList;


public class HardcodedBypassStore {
	static ArrayList<Object> vals = new ArrayList<Object>();
	
	static{
		vals.add(null);
	}
	public static final Object get(int i)
	{
		if(i == -1 || i == 0)
			return null;
		synchronized (vals) {
			return vals.get(i);
		}
	}
	
	public static final int add(Object a)
	{
		if(a == null)
			return -1;
		synchronized (vals) {
			vals.add(a);
			return vals.size() - 1;
		}
	}
}
