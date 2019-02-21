package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;

public class StringConcatIntTagITCase extends BasePhosphorTest {
	@Test
	public void testLDCStringConcat() throws Exception {
		String str1 = "abcdefg";
		((TaintedWithIntTag)((Object)str1)).setPHOSPHOR_TAG(10);
		String str2 = "a"+str1;
		assertTrue(Tainter.getTaint(str2.charAt(0)) == 0);
		assertTrue(Tainter.getTaint(str2.charAt(1)) == 10);
	}

	@Test
	public void testNewStringConcat() throws Exception {
		String str1 = new String("abcdefg");
		((TaintedWithIntTag)((Object)str1)).setPHOSPHOR_TAG(10);
		String str2 = "a"+str1;
		assertTrue(Tainter.getTaint(str2.charAt(0)) == 0);
		assertTrue(Tainter.getTaint(str2.charAt(1)) == 10);
	}

	@Test
	public void testFromInt() throws Exception {
		int taintedInt = Tainter.taintedInt(100, 10);
		String str = taintedInt + "\n";
		assertTrue(Tainter.getTaint(str.charAt(0)) == 10);
	}	
}
