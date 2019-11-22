package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

/* A basic singly linked list implementation. */
public class SinglyLinkedList<E> implements Iterable<E>, Serializable {

    private static final long serialVersionUID = -6504649129625066964L;
    // The first node in the list
    private Node<E> head;
    // The last node in the list
    private Node<E> tail;
    // The number of items in the list
    private int size;

    /* Constructs a new empty list. */
    public SinglyLinkedList() {
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

    /* Adds the specified item to the tail of the list. */
    public void addLast(E item) {
        Node<E> n = new Node<>(item);
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
        Node<E> n = new Node<>(item, head);
        size++;
        if(head == null) {
            // The list was empty
            head = tail = n;
        } else {
            head = n;
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
        } else if(head == tail) {
            // The list was of size one
            E item = head.item;
            head = tail = null;
            size = 0;
            return item;
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

    /* Removes and returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public E removeFirst() {
        return pop();
    }

    /* Adds the specified item to the tail of the list. */
    public void enqueue(E item) {
        addLast(item);
    }

    /* Returns an array containing the elements of this list. */
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node<E> cur = head;
        for(int i = 0; i < size; i++) {
            arr[i] = cur.item;
            cur = cur.next;
        }
        return arr;
    }

    /* Returns an array containing the elements of this list. The runtime type of the returned array is that of the specified array. */
    @SuppressWarnings("unchecked")
    public E[] toArray(E[] arr) {
        if(arr.length < size) {
            arr = (E[]) Array.newInstance(arr.getClass().getComponentType(), size);
        }
        Node<E> cur = head;
        for(int i = 0; i < size; i++) {
            arr[i] = cur.item;
            cur = cur.next;
        }
        return arr;
    }

    /* Returns a shallow copy of the list. */
    public SinglyLinkedList<E> copy() {
        SinglyLinkedList<E> copy = new SinglyLinkedList<>();
        for(Node<E> cur = head; cur != null; cur = cur.next) {
            copy.addLast(cur.item);
        }
        return copy;
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
    public boolean equals(Object other) {
        if(this == other) {
            return true;
        } else if(other == null || getClass() != other.getClass()) {
            return false;
        } else {
            SinglyLinkedList otherList = (SinglyLinkedList) other;
            if(this.size != otherList.size) {
                return false;
            }
            for(Node cur1 = this.head, cur2 = otherList.head; cur1 != null && cur2 != null; cur1 = cur1.next, cur2 = cur2.next) {
                if((cur1.item != cur2.item) && (cur1.item == null || !cur1.item.equals(cur2.item))) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        int result = 1;
        for(Node cur = head; cur != null; cur = cur.next) {
            result = 31 * result + (cur.item == null ? 0 : cur.item.hashCode());
        }
        return result;
    }

    @Override
    public Iterator<E> iterator() {
        return new SimpleListIterator();
    }

    private class SimpleListIterator implements Iterator<E>, Serializable {

        private static final long serialVersionUID = 2719802043259437539L;
        // The node whose item will be returned next
        Node<E> current;
        // The node before the last node returned
        Node<E> prev;

        SimpleListIterator() {
            current = head;
            prev = null;
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
                if(prev == null && current != head) {
                    prev = head;
                } else if(prev != null) {
                    prev = prev.next;
                }
                E item = current.item;
                current = current.next;
                return item;
            }
        }

        @Override
        public void remove() {
            if(prev == null && current == head) {
                // No items have been returned yet
                throw new IllegalStateException();
            } else if(prev == null) {
                // Removing the head
                pop();
            } else if(current == null) {
                // Removing the tail
                tail = prev;
                prev.next = null;
                size--;
            } else {
                // Removing node in the middle
                prev.next = current;
                size--;
            }
        }
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
}
