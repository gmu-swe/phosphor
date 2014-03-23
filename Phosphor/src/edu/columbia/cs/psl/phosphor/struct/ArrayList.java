package edu.columbia.cs.psl.phosphor.struct;

public class ArrayList<T> {
	T[] array;
	int max = 0;
	@SuppressWarnings("unchecked")
	public ArrayList()
	{
		array = (T[]) new Object[200];
	}
	public T get(int i)
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
	public void add(T obj)
	{
		if(max >= array.length - 1)
		{
			T[] tmp = array;
			array = (T[]) new Object[array.length + 100];
			System.arraycopy(tmp, 0, array, 0, tmp.length);
		}
		array[max] = obj;
		max++;
	}
	public void toArray(T[] dest) {
		System.arraycopy(array, 0, dest, 0, max);
	}
}
