package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntSinglyLinkedListTest {

    @Test
    public void testSize() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        int size = 5;
        for(int i = 0; i < size; i++) {
            list.enqueue(i);
        }
        assertEquals(size, list.size());
    }

    @Test
    public void testSizeEmptyList() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        assertEquals(0, list.size());
    }

    @Test
    public void testContains() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.enqueue(4);
        list.enqueue(77);
        list.enqueue(8);
        assertTrue(list.contains(77));
        assertFalse(list.contains(-4));
    }

    @Test
    public void testContainsEmptyList() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        assertFalse(list.contains(0));
    }

    @Test
    public void testAddLast() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.addLast(0);
        list.addLast(1);
        assertEquals(0, list.peek());
    }

    @Test
    public void testAddFirst() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.addFirst(0);
        list.addFirst(1);
        assertEquals(1, list.peek());
    }

    @Test
    public void testIsEmpty() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.enqueue(0);
        list.enqueue(1);
        list.pop();
        assertFalse(list.isEmpty());
    }

    @Test
    public void testIsEmptyEmptyList() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.enqueue(0);
        list.pop();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        for(int i = 0; i < 5; i++) {
            list.enqueue(i);
        }
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testPop() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        for(int i = 0; i < 5; i++) {
            list.enqueue(i);
        }
        for(int i = 0; i < 5; i++) {
            list.pop();
        }
        assertTrue(list.isEmpty());
    }

    @Test
    public void peek() {
        IntSinglyLinkedList list = new IntSinglyLinkedList();
        list.enqueue(45);
        assertEquals(45, list.peek());
        assertFalse(list.isEmpty());
    }
}