package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;


/* A singly linked list. */
public class SimpleLinkedList<E> implements Iterable<E>, Serializable {

    private static final long serialVersionUID = -6504649129625066964L;
    // The first node in the list
    private Node<E> head;
    // The last node in the list
    private Node<E> tail;
    // The number of items in the list
    private int size;

    /* Constructs a new empty list. */
    public SimpleLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /* Returns the number of items in the list. */
    public int size() {
        return size;
    }

    /* Returns whether the list contains an item that is equal to the specified item with respect to the equals method. */
    public boolean contains(E item) {
        if(head == null) {
            return false;
        } else {
            for(Node<E> cur = head; cur != null; cur = cur.next) {
                if(cur.item.equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* Returns whether the list contains an item that is referentially equal to the specified item. */
    public boolean identityContains(E item) {
        if(head == null) {
            return false;
        } else {
            for(Node<E> cur = head; cur != null; cur = cur.next) {
                if(cur.item == item) {
                    return true;
                }
            }
            return false;
        }
    }

    /* Adds the specified item to the tail of the list. */
    public void addLast(E item) {
        Node<E> n = new Node<E>(item);
        size++;
        if(tail == null) {
            // The list was empty
            head = tail = n;
        } else {
            tail.next = n;
            tail = n;
        }
    }

    /* Adds the specified item to the head of the list. */
    public void addFirst(E item) {
        Node<E> n = new Node<E>(item, head);
        size++;
        if(head == null) {
            // The list was empty
            head = tail = n;
        } else {
            head = n;
        }
    }

    /* If the list contains an item that is referentially equal to the specified item returns false. Otherwise, adds
     * the specified item to the tail of the list and returns true. */
    public boolean addIdentityUnique(E item) {
        if(identityContains(item)) {
            return false;
        } else {
            addLast(item);
            return true;
        }
    }

    /* If the list contains an item that is equal to the specified item returns false. Otherwise, adds
     * the specified item to the tail of the list and returns true. */
    public boolean addUnique(E item) {
        if(contains(item)) {
            return false;
        } else {
            addLast(item);
            return true;
        }
    }

    /* Returns whether the list is empty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /* Removes all of the items in the list. */
    public void clear() {
        head = tail = null;
        size = 0;
    }

    /* Removes and returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public E pop() {
        if(head == null) {
            // The list is empty
            throw new NoSuchElementException();
        } else {
            E item = head.item;
            head = head.next;
            size--;
            return item;
        }
    }

    /* Returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public E peek() {
        if(head == null) {
            // The list is empty
            throw new NoSuchElementException();
        } else {
            return head.item;
        }
    }

    /* Adds the specified item to the head of the list. */
    public void push(E item) {
        addFirst(item);
    }

    /* Removes and returns the first item in the list or null if the list is empty. */
    public E dequeue() {
        return pop();
    }

    /* Returns the first item in the list or null if the list is empty. */
    public E getFirst() {
        return peek();
    }

    /* Adds the specified item to the tail of the list. */
    public void enqueue(E item) {
        addLast(item);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for(Node<E> cur = head; cur != null; cur = cur.next) {
            builder.append(cur.item);
            if(cur.next != null) {
                builder.append(", ");
            }
        }
        return builder.append("]").toString();
    }

    @Override
    public Iterator<E> iterator() {
        return new SimpleListIterator();
    }

    /* Stores the value of a single item in the list and a pointer to the next item. */
    private static class Node<E> implements Serializable {
        private static final long serialVersionUID = -4640096704981960035L;
        E item;
        Node<E> next;


        Node(E item) {
            this(item, null);
        }

        Node(E item, Node<E> next) {
            this.item = item;
            this.next = next;
        }
    }

    private class SimpleListIterator implements Iterator<E>, Serializable {

        private static final long serialVersionUID = 2719802043259437539L;
        // The node whose item will be returned next
        Node<E> current;

        SimpleListIterator() {
            current = head;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public E next() {
            if(current == null) {
                throw new NoSuchElementException();
            } else {
                E item = current.item;
                current = current.next;
                return item;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
