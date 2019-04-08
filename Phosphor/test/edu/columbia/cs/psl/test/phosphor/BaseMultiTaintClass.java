package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class BaseMultiTaintClass extends BasePhosphorTest{

	/**
	 * For implicit tracking, if we just called 'fail', then the
	 * method summary analysis with exception handling would say
	 * that the taint tag for our decision to fail or not should
	 * be accumulated for the control flow of the test :)
	 *
	 * However, the analysis is shallow, so it looks only 1 method deep
	 * that is not called. Hence, if failIndirectly is not called, then
	 * we don't record that an exception might be thrown by fail.
	 *
	 * @param message
	 */
	public static void failIndirectly(String message){
		fail(message);
	}

	public static void assertNullOrEmpty(Taint taint)
	{
		if(taint != null)
		{
			if(taint.lbl == null && taint.hasNoDependencies())
				return;
			failIndirectly("Expected null taint. Got: " + taint);
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
		failIndirectly("Expected null taint. Got: " + taint);
	}
	
	public static void assertNonNullTaint(Object obj)
	{
		Taint t = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		assertNotNull(obj);
		if(t == null || (t.lbl == null && t.hasNoDependencies()))
			failIndirectly("Expected non-null taint - got: "  + t);
	}

	public static void assertNonNullTaint(Taint obj)
	{
		assertNotNull(obj);
		if(obj.lbl == null && obj.hasNoDependencies())
			failIndirectly("Expected non-null taint - got: "  + obj);
	}
	
	public static void assertTaintHasLabel(Taint obj, Object lbl)
	{
		assertNotNull(obj);
		if(obj.lbl == lbl)
			return;
		if(obj.hasNoDependencies())
			failIndirectly("Expected taint contained "+ lbl+", has nothing");
		for(Object o : obj.dependencies){
			if(o == lbl)
				return;
		}
		failIndirectly("Expected taint contained "+ lbl+", has " + obj);
	}
	public static void assertTaintHasOnlyLabel(Taint obj, Object lbl)
	{
		if(obj == null)
			failIndirectly("Expected non-null taint");
		if(obj.lbl == lbl)
			return;
		if(obj.hasNoDependencies())
			failIndirectly("Expected taint contained "+ lbl+", has nothing");
		boolean found = false;
		for(Object o : obj.dependencies)
		{
			if(o == lbl)
				found = true;
			else
				failIndirectly("Expected taint contained ONLY "+ lbl+", found " + o);
		}
		if(!found)
		failIndirectly("Expected taint contained "+ lbl+", has " + obj);
	}
	public static void assertTaintHasOnlyLabels(Taint obj, Object... lbl)
	{
		assertNotNull(obj);
		if(obj.hasNoDependencies() && obj.lbl == null)
			failIndirectly("Expected taint contained "+ Arrays.toString(lbl)+", has nothing");
		boolean l1 = false;
		boolean l2 = false;
		HashSet<Object> expected = new HashSet<Object>();
		for(Object o : lbl)
			expected.add(o);
		for(Object o : obj.getDependencies())
		{
			if(expected.contains(o))
				expected.remove(o);
			else
				failIndirectly("Expected taint contained ONLY " + Arrays.toString(lbl) + ", found " + o);
		}
		expected.remove(obj.lbl);
		if(expected.isEmpty())
			return;
		failIndirectly("Expected taint contained "+ expected +", has " + obj);
	}
}
