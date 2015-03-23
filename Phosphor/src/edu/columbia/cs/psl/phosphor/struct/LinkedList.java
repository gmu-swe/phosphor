package edu.columbia.cs.psl.phosphor.struct;


public class LinkedList<T> {
	public static class Node<Z> {
		public Z entry;
		public Node<Z> next;
	}
	private Node<T> first;
	private Node<T> last;
	public synchronized void add(T o)
	{
		Node<T> n = new Node<T>();
		n.entry = o;
		last.next=n;
		last = n;
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