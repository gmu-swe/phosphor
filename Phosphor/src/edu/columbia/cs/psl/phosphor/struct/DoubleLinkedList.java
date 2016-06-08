package edu.columbia.cs.psl.phosphor.struct;


public class DoubleLinkedList<T> implements Cloneable {
	public static class Node<Z> implements Cloneable{
		public Z entry;
		public Node<Z> next;
		public Node<Z> prev;
	}
	private Node<T> first;
	private Node<T> last;
	public synchronized boolean addUnique(T o)
	{
		Node<T> i = first.next;
		while(i != null)
		{
			if(i.entry == o)
				return false;
			i = i.next;
		}
		Node<T> n = new Node<T>();
		n.entry = o;
		last.next=n;
		last.prev = i;
		last = n;
		return true;
	}
	public synchronized void addAll(DoubleLinkedList<T> o)
	{
		Node<T> i = o.getFirst();
		while(i != null)
		{
			addUnique(i.entry);
//			Node<T> n = new Node<T>();
//			n.entry = i.entry;
//			last.next=n;
//			last = n;
			i = i.next;
		}
	}
	public synchronized Node<T> add(T o)
	{
		Node<T> n = new Node<T>();
		n.entry = o;
		Node<T> f = first.next;
		first.next = n;
		n.prev = first;
		n.next = f;
		if(f != null)
			f.prev = n;
		return n;
	}
	public Node<T> getFirst()
	{
		return first.next;
	}
	public DoubleLinkedList()
	{
		clear();
	}
	@Override
	public String toString() {
	    StringBuilder ret = new StringBuilder();
		ret.append("[");
		Node<T> e = getFirst();
		while(e != null)
		{
			ret.append(e.entry);
			ret.append(",");
			e = e.next;
		}
		ret.append("]");
		return ret.toString();
	}
	public void clear() {
		first = new Node<T>();
		last = first;
	}
}