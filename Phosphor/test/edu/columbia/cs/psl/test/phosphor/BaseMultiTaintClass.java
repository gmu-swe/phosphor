package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class BaseMultiTaintClass {

	public static void assertNullOrEmpty(Taint taint)
	{
		if(taint != null)
		{
			if(taint.lbl == null && taint.hasNoDependencies())
				return;
			fail("Expected null taint. Got: " + taint);
		}
	}
	public static void assertNoTaint(String obj)
	{
		Taint taint = MultiTainter.getTaint(obj.toCharArray()[0]);
		if(taint == null)
		{
			return;
		}
		if(taint.lbl == null && taint.hasNoDependencies())
			return;
		fail("Expected null taint. Got: " + taint);
	}
	
	public static void assertNonNullTaint(Object obj)
	{
		Taint t = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		assertNotNull(obj);
		if(t == null || (t.lbl == null && t.hasNoDependencies()))
			fail("Expected non-null taint - got: "  + t);
	}

	public static void assertNonNullTaint(Taint obj)
	{
		assertNotNull(obj);
		if(obj.lbl == null && obj.hasNoDependencies())
			fail("Expected non-null taint - got: "  + obj);
	}
	
	public static void assertTaintHasLabel(Taint obj, Object lbl)
	{
		assertNotNull(obj);
		if(obj.lbl == lbl)
			return;
		if(obj.hasNoDependencies())
			fail("Expected taint contained "+ lbl+", has nothing");
		Node<Object> dep = obj.dependencies.getFirst();
		while(dep != null)
		{
			if(dep.entry != null && dep.entry == lbl)
				return;
			dep = dep.next;
		}
		fail("Expected taint contained "+ lbl+", has " + lbl);
	}
	public static void assertTaintHasOnlyLabel(Taint obj, Object lbl)
	{
		assertNotNull(obj);
		if(obj.lbl == lbl)
			return;
		if(obj.hasNoDependencies())
			fail("Expected taint contained "+ lbl+", has nothing");
		Node<Object> dep = obj.dependencies.getFirst();
		while(dep != null)
		{
			if(dep.entry != null && dep.entry == lbl && dep.next == null)
				return;
			else if(dep.entry != null)
				fail("Expected taint contained ONLY "+ lbl+", found " + dep.entry);

			dep = dep.next;
		}
		fail("Expected taint contained "+ lbl+", has " + obj);
	}
	public static void assertTaintHasOnlyLabels(Taint obj, Object lbl, Object lbl2)
	{
		assertNotNull(obj);
		if(obj.lbl == lbl)
			return;
		if(obj.hasNoDependencies())
			fail("Expected taint contained "+ lbl+", has nothing");
		Node<Object> dep = obj.dependencies.getFirst();
		boolean l1 = false;
		boolean l2 = false;
		while(dep != null)
		{
			if(dep.entry != null && dep.entry == lbl)
			{
				l1 = true;
			}
			else if(dep.entry != null && dep.entry == lbl2)
			{
				l2 = true;
			} else if (dep.entry != null)
				fail("Expected taint contained ONLY " + lbl + "," + lbl2 + ", found " + dep.entry);

			dep = dep.next;
		}
		if(l1 && l2)
			return;
		fail("Expected taint contained "+ lbl+", has " + obj);
	}
}
