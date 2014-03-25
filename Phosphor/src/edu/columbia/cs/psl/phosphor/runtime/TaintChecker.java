package edu.columbia.cs.psl.phosphor.runtime;

public class TaintChecker {
	public static boolean hasTaints(int[] tags)
	{
		for(int i : tags)
		{
			if(i != 0)
				return true;
		}
		return false;
	}
	public static void setTaints(int[] array, int tag) {
		for (int i = 0; i < array.length; i++)
			array[i] = tag;
	}
}
