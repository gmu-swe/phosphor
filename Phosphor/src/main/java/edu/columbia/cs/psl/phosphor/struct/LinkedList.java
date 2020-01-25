package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;


public class LinkedList<T> implements Cloneable, Serializable {

    private static final long serialVersionUID = -5225392158190086269L;
    private Node<T> first;
    private Node<T> last;

    public LinkedList() {
        clear();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public synchronized boolean addUnique(T o) {
        Node<T> i = first.next;
        while(i != null) {
            if(i.entry == o) {
                return false;
            }
            i = i.next;
        }
        Node<T> n = new Node<T>();
        n.entry = o;
        last.next = n;
        last = n;
        return true;
    }

    public int size() {
        Node<T> i = first.next;
        int c = 0;
        while(i != null) {
            c++;
            i = i.next;
        }
        return c;
    }

    public synchronized boolean addUniqueObjEquals(T o) {
        Node<T> i = first.next;
        while(i != null) {
            if(i.entry.equals(o)) {
                return false;
            }
            i = i.next;
        }
        Node<T> n = new Node<T>();
        n.entry = o;
        last.next = n;
        last = n;
        return true;
    }

    public synchronized boolean addAll(LinkedList<T> o) {
        boolean added = false;
        Node<T> i = o.getFirst();
        while(i != null) {
            if(i.entry != null) {
                added |= addUnique(i.entry);
            }
            i = i.next;
        }
        return added;
    }

    /* Adds the specified element to the front of the list. */
    public synchronized void addFast(T o) {
        Node<T> n = new Node<T>();
        n.entry = o;
        if(first.next == null) {
            first.next = n;
            last = n;
        } else {
            n.next = first.next;
            first.next = n;
        }
    }

    public synchronized boolean isEmpty() {
        return first.next == null;
    }

    public synchronized boolean add(T o) {
        return addUnique(o);
    }

    public Node<T> getFirst() {
        return first.next;
    }

    public T pop() {
        Node<T> f = first.next;
        if(f == null) {
            return null;
        }
        first.next = f.next;
        if(first.next == null) {
            last = first;
        }
        return f.entry;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("[");
        Node<T> e = getFirst();
        while(e != null) {
            ret.append(e.entry);
            ret.append(",");
            e = e.next;
        }
        ret.append("]");
        return ret.toString();
    }

    public void clear() {
        first = new Node<>();
        last = first;
    }

    public static class Node<Z> implements Cloneable, Serializable {
        private static final long serialVersionUID = -7510843924447531965L;
        public Z entry;
        public Node<Z> next;
    }
}