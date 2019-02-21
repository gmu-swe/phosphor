package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class StringConcatObjTagITCase extends BasePhosphorTest {
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
	
	@Test
	public void testConcateStringInt() {
		String s = "abc";
		int val = 98;
		
		MultiTainter.taintedObject(s, new Taint("string"));
		val = MultiTainter.taintedInt(val, "int");
		
		String concate = s + val;
		
		for (int i = 0; i < concate.length(); i++) {
			char c = concate.charAt(i);
			Taint ct = MultiTainter.getTaint(c);
			if (i < 3) {
				assertEquals(ct.lbl, "string");
			} else {
				assertEquals(ct.lbl, "int");
			}
		}
	}
}
