package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.NoSuchElementException;


/* A special case of linked list for ints to help avoid the cost of boxing primitive int to Integers. */
public class IntSinglyLinkedList implements Serializable {

    private static final long serialVersionUID = 2804910377808863384L;
    // The first node in the list
    private IntNode head;
    // The last node in the list
    private IntNode tail;
    // The number of items in the list
    private int size;

    /* Constructs a new empty list. */
    public IntSinglyLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /* Returns the number of items in the list. */
    public int size() {
        return size;
    }

    /* Returns whether the list contains an item that is equal to the specified item. */
    public boolean contains(int item) {
        if(head == null) {
            return false;
        } else {
            for(IntNode cur = head; cur != null; cur = cur.next) {
                if(cur.item == item) {
                    return true;
                }
            }
            return false;
        }
    }

    /* Adds the specified item to the tail of the list. */
    public void addLast(int item) {
        IntNode n = new IntNode(item);
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
    public void addFirst(int item) {
        IntNode n = new IntNode(item, head);
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
    public int pop() {
        if(head == null) {
            // The list is empty
            throw new NoSuchElementException();
        } else {
            int item = head.item;
            head = head.next;
            size--;
            return item;
        }
    }

    /* Returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public int peek() {
        if(head == null) {
            // The list is empty
            throw new NoSuchElementException();
        } else {
            return head.item;
        }
    }

    /* Adds the specified item to the head of the list. */
    public void push(int item) {
        addFirst(item);
    }

    /* Removes and returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public int dequeue() {
        return pop();
    }

    /* Returns the first item in the list. Throws a NoSuchElementException if the list is empty. */
    public int getFirst() {
        return peek();
    }

    /* Adds the specified item to the tail of the list. */
    public void enqueue(int item) {
        addLast(item);
    }

    /* Returns an array containing the elements of this list. */
    public int[] toArray() {
        int[] arr = new int[size];
        IntNode cur = head;
        for(int i = 0; i < size; i++) {
            arr[i] = cur.item;
            cur = cur.next;
        }
        return arr;
    }

    /* Returns an array containing the boxed elements of this list. */
    public Integer[] toObjArray() {
        Integer[] arr = new Integer[size];
        IntNode cur = head;
        for(int i = 0; i < size; i++) {
            arr[i] = cur.item;
            cur = cur.next;
        }
        return arr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for(IntNode cur = head; cur != null; cur = cur.next) {
            builder.append(cur.item);
            if(cur.next != null) {
                builder.append(", ");
            }
        }
        return builder.append("]").toString();
    }

    /* Stores the in item in the list and a pointer to the next item. */
    private static class IntNode implements Serializable {
        private static final long serialVersionUID = -4640096704981960035L;
        int item;
        IntNode next;

        IntNode(int item) {
            this(item, null);
        }

        IntNode(int item, IntNode next) {
            this.item = item;
            this.next = next;
        }
    }
}
