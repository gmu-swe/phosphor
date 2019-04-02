package edu.columbia.cs.psl.phosphor.struct;

public class IntArrayList {
	int[] array;
	int max = 0;
	@SuppressWarnings("unchecked")
	public IntArrayList()
	{
		array = new int[200];
	}
	public int get(int i)
	{
		if(i >= array.length)
		{
			System.out.println("Asking for invalid idx: " + i +" in " + array.length);
//			return null;
		}
		return array[i];
	}
	public int size()
	{
		return max;
	}
	public void add(int obj)
	{
		if(max >= array.length - 1)
		{
			int minCapacity = max;
			int oldCapacity = array.length;
			int newCapacity = oldCapacity + (oldCapacity >> 1);
			if (newCapacity - minCapacity < 0)
				newCapacity = minCapacity;
			if (newCapacity - (Integer.MAX_VALUE - 8) > 0)
				newCapacity = Integer.MAX_VALUE - 8;
			int[] tmp = array;
			array = new int[newCapacity];
			System.arraycopy(tmp, 0, array, 0, tmp.length);
		}
		array[max] = obj;
		max++;
	}

	public int pop() {
		max--;
		return array[max];
	}
}
