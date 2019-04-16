package edu.columbia.cs.psl.phosphor.struct;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class GarbageCollectedArrayList<T> {
	T[] array;
	IntArrayList free;
	ArrayListReference[] referents;
	ReferenceQueue referenceQueue = new ReferenceQueue();

	int max = 0;
	@SuppressWarnings("unchecked")
	public GarbageCollectedArrayList() {
		array = (T[]) new Object[200];
		free = new IntArrayList();
		referents = new ArrayListReference[200];
	}
	public T get(int i)
	{
		if(i >= array.length)
		{
			System.out.println("Asking for invalid idx: " + i +" in " + array.length);
		}
		return array[i];
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

		ArrayListReference[] referencesTmp = referents;
		referents = new ArrayListReference[newCapacity];
		System.arraycopy(referencesTmp, 0, referents, 0, referencesTmp.length);
	}

	private final int growOrGC(Object referent, T obj){
		int ret = max;
		for(Reference ref; (ref = referenceQueue.poll())!= null;)
		{
			int freed = ((ArrayListReference) ref).idx;
			free.add(freed);
			array[freed] = null;
		}
		if (free.size() > 0) {
			ret = free.pop();
		}
		else{
			grow(max + 1);
			max++;
		}
		if(referent != null)
			referents[ret] = new ArrayListReference(referent, ret, referenceQueue);
		array[ret] = obj;
		return ret;
	}
	private int addSlow(Object referent, T obj){
		if(free.size() > 0)
		{
			int ret = free.pop();
			array[ret] = obj;
			if (referent != null)
				referents[ret] = new ArrayListReference(referent, ret, referenceQueue);
			return ret;
		}
		else {
			return growOrGC(referent, obj);
		}
	}
	public int add(Object referent, T obj) {
		if (max >= array.length - 1) {
			return addSlow(referent, obj);
		}
		else {
			int ret = max;
			array[max] = obj;
			if (referent != null)
				referents[max] = new ArrayListReference(referent, max, referenceQueue);
			max++;
			return ret;
		}
	}

	public void toArray(T[] dest) {
		System.arraycopy(array, 0, dest, 0, max);
	}
	static class ArrayListReference extends PhantomReference<Object>{

		int idx;
		/**
		 * Creates a new phantom reference that refers to the given object and
		 * is registered with the given queue.
		 *
		 * <p> It is possible to create a phantom reference with a <tt>null</tt>
		 * queue, but such a reference is completely useless: Its <tt>get</tt>
		 * method will always return null and, since it does not have a queue, it
		 * will never be enqueued.
		 *
		 * @param referent the object the new phantom reference will refer to
		 * @param q        the queue with which the reference is to be registered,
		 */
		public ArrayListReference(Object referent, int idx, ReferenceQueue q) {
			super(referent, q);
			this.idx = idx;
		}
	}
}
