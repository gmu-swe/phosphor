package edu.columbia.cs.psl.phosphor.struct;


public class LinkedList<T> implements Cloneable {
	public static class Node<Z> implements Cloneable{
		public Z entry;
		public Node<Z> next;
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
		last = n;
		return true;
	}
	public synchronized void addAll(LinkedList<T> o)
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
	public synchronized void add(T o)
	{
		addUnique(o);
	}
	public Node<T> getFirst()
	{
		return first.next;
	}
	public LinkedList()
	{
		clear();
	}
	
	public void clear() {
		first = new Node<T>();
		last = first;
	}
}