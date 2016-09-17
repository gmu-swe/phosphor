package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class StringConcatObjTagITCase {
	@Test
	public void testLDCStringConcat() throws Exception {
		String str1 = "abcdefg";
		((TaintedWithObjTag)((Object)str1)).setPHOSPHOR_TAG(new Taint("sensitive"));
		String str2 = "a"+str1;
		assertTrue(MultiTainter.getTaint(str2.charAt(0)) == null);
		assertTrue(MultiTainter.getTaint(str2.charAt(1)) != null);
	}

	@Test
	public void testNewStringConcat() throws Exception {
		String str1 = new String("abcdefg");
		((TaintedWithObjTag)((Object)str1)).setPHOSPHOR_TAG(new Taint("sensitive"));
		String str2 = "a"+str1;
		assertTrue(MultiTainter.getTaint(str2.charAt(0)) == null);
		assertTrue(MultiTainter.getTaint(str2.charAt(1)) != null);
	}
	
	@Test
	public void testConcatAndMultiTainter() throws Exception {
		String str1 = new String("abcdefg");
		MultiTainter.taintedObject(str1, new Taint("Sensitive"));
		String str2 = str1 + "a";
		assertTrue(MultiTainter.getTaint(str2.charAt(0)) != null);
		assertTrue(MultiTainter.getTaint(str2.charAt(7)) == null);
	}
}
