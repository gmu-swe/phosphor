package edu.columbia.cs.psl.phosphor.struct;

public class GarbageCollectedArrayList<T> {
	T[] array;
	int[] free;
	int lastEntryInFree = -1;

	int max = 0;
	@SuppressWarnings("unchecked")
	public GarbageCollectedArrayList() {
		array = (T[]) new Object[200];
	}
	public T get(int i)
	{
		if(i >= array.length)
		{
			System.out.println("Asking for invalid idx: " + i +" in " + array.length);
		}
		return array[i];
	}
	public void free(int idx){
		if(free == null){
			free = new int[array.length];
		}
		array[idx] = null;
		free[lastEntryInFree] = idx;
		lastEntryInFree++;
	}
	private void grow(int minCapacity){
		int oldCapacity = array.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);
		if (newCapacity - minCapacity < 0)
			newCapacity = minCapacity;
		if (newCapacity - (Integer.MAX_VALUE - 8) > 0)
			newCapacity = Integer.MAX_VALUE - 8;

		T[] tmp = array;
		array = (T[]) new Object[newCapacity];
		System.arraycopy(tmp, 0, array, 0, tmp.length);

		if(free != null) {
			int[] tmp2 = free;
			free = new int[newCapacity];
			System.arraycopy(tmp2, 0, free, 0, tmp2.length);
		}
	}

	private final int growOrGC(T obj){
		int ret = max;
		if (lastEntryInFree >= 0) {
			ret = free[lastEntryInFree];
			array[ret] = obj;
			lastEntryInFree--;
			return ret;
		}
		grow(max + 1);
		array[ret] = obj;
		max++;
		return ret;
	}
	public int add(T obj) {
		int ret = max;
		if (ret >= array.length - 1) {
			return growOrGC(obj);
		}
		array[ret] = obj;
		max++;
		return ret;
	}

	public void toArray(T[] dest) {
		System.arraycopy(array, 0, dest, 0, max);
	}
}
