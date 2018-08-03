package edu.columbia.cs.psl.phosphor.struct;

/**
 * from: https://github.com/robertovormittag/algorithms/blob/master/src/main/java/net/robertovormittag/idealab/structures/hash/SimpleHashSet.java
 * from: package net.robertovormittag.idealab.structures.hash;
 */


import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SimpleIdentityHashSet<T> implements Iterable<T>, Serializable {


	private static class Entry<T> implements Serializable {
		T key;
		Entry<T> next;
	}
	private Entry<T>[] buckets;

	private int size;
	public int size() {
		return size;
	}

	public SimpleIdentityHashSet(){
		this(16);
	}
	/**
	 *
	 * @param capacity
	 */
	public SimpleIdentityHashSet(int capacity) {

		buckets = new Entry[capacity];
		size = 0;
	}

	public boolean isEmpty(){
		return size() == 0;
	}
	/**
	 *
	 * @param hashCode
	 * @return
	 */
	private int hashFunction(int hashCode) {

		int index = hashCode;
		if (index < 0) { index = -index; }
		return index % buckets.length;
	}

	/**
	 *
	 * @param element
	 * @return
	 */
	public boolean contains(T element) {

		if(element == null)
			return false;
		int index = hashFunction(System.identityHashCode(element));
		Entry current = buckets[index];

		while (current != null) {
			// check if node contains element
			if (current.key == element) { return true; }
			// otherwise visit next node in the bucket
			current = current.next;
		}
		// no element found
		return false;
	}

	public boolean addAll(Iterable<T> col){
		boolean ret = false;
		for(T o : col)
			ret = ret | add(o);
		return ret;
	}

	/**
	 *
	 * @param element
	 * @return
	 */
	public boolean add(T element) {

		int index = hashFunction(System.identityHashCode(element));
		//log.info(element.toString() + " hashCode=" + element.hashCode() + " index=" + index);
		Entry<T> current = buckets[index];

		while (current != null) {
			// element is already in set
			if (current.key == element) { return false; }
			// otherwise visit next entry in the bucket
			current = current.next;
		}
		// no element found so add new entry
		Entry<T> entry = new Entry();
		entry.key = element;
		// current Entry is null if bucket is empty
		// if it is not null it becomes next Entry
		entry.next  = buckets[index];
		buckets[index] = entry;
		size++;
		return true;
	}

	/**
	 *
	 * @param element
	 * @return
	 */
	public boolean remove(T element) {

		int index = hashFunction(System.identityHashCode(element));
		Entry<T> current = buckets[index];
		Entry<T> previous = null;

		while (current != null) {
			// element found so remove it
			if (current.key == element) {

				if (previous == null) {
					buckets[index] = current.next;
				} else {
					previous.next = current.next;
				}
				size--;
				return true;
			}

			previous = current;
			current = current.next;
		}
		// no element found nothing to remove
		return false;
	}


	@Override
	public String toString() {

		Entry currentEntry = null;
		StringBuffer sb = new StringBuffer();

		// loop through the array
		for (int index=0; index < buckets.length; index++) {
			// we have an entry
			if (buckets[index] != null) {
				currentEntry = buckets[index];
				sb.append("[" + index + "]");
				sb.append(" " + currentEntry.key.toString());
				while (currentEntry.next != null) {
					currentEntry = currentEntry.next;
					sb.append(" -> " + currentEntry.key.toString());
				}
				sb.append('\n');
			}
		}

		return sb.toString();
	}

	/**
	 *
	 * @return
	 */
	public Iterator<T> iterator() {
		return new SimpleHashSetIterator();
	}

	public Iterator<T> iterator$$PHOSPHORTAGGED(ControlTaintTagStack ctrl){
		return iterator();
	}


	/**
	 * A simple iterator for this Set
	 */
	class SimpleHashSetIterator implements Iterator {

		private int currentBucket;
		private int previousBucket;
		private Entry<T> currentEntry;
		private Entry<T> previousEntry;


		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		public SimpleHashSetIterator() {

			currentEntry = null;
			previousEntry = null;
			currentBucket = -1;
			previousBucket = -1;

		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {

			// currentEntry node has next
			if (currentEntry != null && currentEntry.next != null) { return true; }

			// there are still nodes
			for (int index = currentBucket+1; index < buckets.length; index++) {
				if (buckets[index] != null) { return true; }
			}

			// nothing left
			return false;
		}

		public TaintedBooleanWithObjTag hasNext$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret){
			ret.taint = null;
			ret.val = hasNext();
			return ret;
		}
		public TaintedBooleanWithObjTag hasNext$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret){
			ret.taint = null;
			ret.val = hasNext();
			return ret;
		}
		public TaintedBooleanWithIntTag hasNext$$PHOSPHORTAGGED(TaintedBooleanWithIntTag ret){
			ret.taint = 0;
			ret.val = hasNext();
			return ret;
		}

		public T next$$PHOSPHORTAGGED(ControlTaintTagStack ctrl){
			return next();
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public T next() {

			previousEntry = currentEntry;
			previousBucket = currentBucket;

			// if either the current or next node are null
			if (currentEntry == null || currentEntry.next == null) {

				// go to next bucket
				currentBucket++;

				// keep going until you find a bucket with a node
				while (currentBucket < buckets.length &&
						buckets[currentBucket] == null) {
					// go to next bucket
					currentBucket++;
				}

				// if bucket array index still in bounds
				// make it the current node
				if (currentBucket < buckets.length) {
					currentEntry = buckets[currentBucket];
				}
				// otherwise there are no more elements
				else {
					throw new NoSuchElementException();
				}
			}
			// go to the next element in bucket
			else {

				currentEntry = currentEntry.next;
			}

			// return the element in the current node
			return currentEntry.key;

		}

	}

}
