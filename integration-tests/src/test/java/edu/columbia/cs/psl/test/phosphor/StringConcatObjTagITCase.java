package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import static org.junit.Assert.*;

public class StringConcatObjTagITCase extends BasePhosphorTest {
	@Test
	public void testLDCStringConcat() throws Exception {
		String str1 = "abcdefg";
		((TaintedWithObjTag)((Object)str1)).setPHOSPHOR_TAG(Taint.withLabel("sensitive"));
		String str2 = "a"+str1;
		assertTrue(MultiTainter.getTaint(str2.charAt(0)).isEmpty());
		assertFalse(MultiTainter.getTaint(str2.charAt(1)).isEmpty());
	}

	@Test
	public void testNewStringConcat() throws Exception {
		String str1 = "abcdefg";
		((TaintedWithObjTag)((Object)str1)).setPHOSPHOR_TAG(Taint.withLabel("sensitive"));
		String str2 = "a"+str1;
		assertTrue(MultiTainter.getTaint(str2.charAt(0)).isEmpty());
		assertFalse(MultiTainter.getTaint(str2.charAt(1)).isEmpty());
	}
	
	@Test
	public void testConcatAndMultiTainter() throws Exception {
		String str1 = "abcdefg";
		MultiTainter.taintedObject(str1, Taint.withLabel("Sensitive"));
		String str2 = str1 + "a";
		assertFalse(MultiTainter.getTaint(str2.charAt(0)).isEmpty());
		assertTrue(MultiTainter.getTaint(str2.charAt(7)).isEmpty());
	}
	
	@Test
	public void testConcateStringInt() {
		String s = "abc";
		int val = 98;
		
		MultiTainter.taintedObject(s, Taint.withLabel("string"));
		val = MultiTainter.taintedInt(val, "int");
		
		String concate = s + val;
		
		for (int i = 0; i < concate.length(); i++) {
			char c = concate.charAt(i);
			Taint ct = MultiTainter.getTaint(c);
			if (i < 3) {
				assertEquals(ct.getLabels()[0], "string");
			} else {
				assertEquals(ct.getLabels()[0], "int");
			}
		}
	}
}
