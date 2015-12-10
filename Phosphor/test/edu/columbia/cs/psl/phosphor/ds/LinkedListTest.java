package edu.columbia.cs.psl.phosphor.ds;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.struct.DoubleLinkedList;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;

public class LinkedListTest {
	@Test
	public void testDoubleLinkedAdd() throws Exception {
		String a = "a";
		String b = "b";
		DoubleLinkedList<String> l = new DoubleLinkedList<String>();
		l.add(a);
		l.add(b);
		DoubleLinkedList.Node<String> e = l.getFirst();

		while(e != null)
		{
			System.out.println(e.entry);
			e = e.next;
		}
		System.out.println(l);
		e = l.getFirst();
		while(e != null)
		{
			if(e.entry == a)
			{
				//Remove from controltag's list
				DoubleLinkedList.Node<String> pl = e;
				if(pl.prev != null)
					pl.prev.next = pl.next;
				if(pl.next != null)
					pl.next.prev  = pl.prev;
				//Remove from tag's list
			}
			e = e.next;
		}
		e = l.getFirst();

		while(e != null)
		{
			System.out.println(e.entry);
			e = e.next;
		}
		System.out.println(l);

	}
}
