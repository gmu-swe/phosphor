package edu.columbia.cs.psl.phosphor.struct;

import java.util.Iterator;

public class ArrayList<T> implements Iterable<T> {

	// The maximum length of the element array
	private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;
	// The initial length of the element array
	private static final int INITIAL_CAPACITY = 200;
	// The stores the elements contained in the list
	private T[] elements;
	// The number of elements in the list
	private int size;

	/* Initializes an empty list with a starting capacity of INITIAL_CAPACITY. */
	@SuppressWarnings("unchecked")
	public ArrayList() {
		this.elements = (T[]) new Object[INITIAL_CAPACITY];
		this.size = 0;
	}

	/* Initializes an empty list with the specified starting capacity. */
	@SuppressWarnings("unchecked")
	public ArrayList(int capacity) {
		this.elements = (T[]) new Object[capacity];
		this.size = 0;
	}

	/* Returns the element at the specified index. If the specified index is larger than the index of the last element
	 * in the list throws an IndexOutOfBoundsException. */
	public T get(int index) {
	 	if(index < 0 || index >= size) {
			throw new IndexOutOfBoundsException();
		} else {
	 		return elements[index];
		}
	}

	/* Returns the number of elements in this list. */
	public int size() {
		return size;
	}

	/* Returns the current length of the element array. */
	public int getCapacity() {
		return elements.length;
	}

	/* Returns true is this list does not contain any elements. */
	public boolean isEmpty() {
		return size == 0;
	}

	/* Increases the size of the elements array so that it can hold at least the specified minimum number of elements. */
	@SuppressWarnings("unchecked")
	public void ensureCapacity(int minCapacity) {
		int newCapacity = elements.length + (elements.length >> 1); // Increase capacity by 50%
		newCapacity = (newCapacity - minCapacity < 0) ? minCapacity : newCapacity;
		newCapacity = (newCapacity - MAX_CAPACITY > 0) ? MAX_CAPACITY : newCapacity;
		T[] temp = elements;
		elements = (T[]) new Object[newCapacity];
		System.arraycopy(temp, 0, elements, 0, size);
	}

	/* Adds the specified object to the end of this list. */
	public void add(T obj) {
		if(size >= elements.length) {
			ensureCapacity(size+1);
		}
		elements[size++] = obj;
	}

	/* Replaces the element at the specified position in this list with the specified object. */
	public void replace(int index, T obj) {
		if(index < 0 || index >= size) {
			throw new IndexOutOfBoundsException();
		} else {
			elements[index] = obj;
		}
	}

	/* Copies the elements of this list into the specified array. */
	public void toArray(T[] dest) {
		System.arraycopy(elements, 0, dest, 0, size);
	}

	@Override
	public Iterator<T> iterator() {
		return new ArrayListIterator();
	}

	private class ArrayListIterator implements Iterator<T> {

		// Index of the next element to be returned
		int index;

		ArrayListIterator() {
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public T next() {
			return get(index++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}
}
