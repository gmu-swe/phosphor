package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.util.IgnoredTestUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharacterCodePointObjTagITCase extends BasePhosphorTest {
	@Test
	public void testCodePointAt(){
		char[] c = new char[1];
		c[0] = MultiTainter.taintedChar((char)4,"foo");
		int i = Character.codePointAt(c,0);
		Assert.assertNotNull(MultiTainter.getTaint(i));

		final char[] crar = Character.toChars(Character.codePointAt(c, 0));
		Assert.assertNotNull(MultiTainter.getTaint(crar[0]));
	}

	@Test
	public void testCodePointBefore() {
		char[] s = {'a', 'b', 'c', 'd', 'e'};
		for (int i = 0; i < 5; i++) {
			s[i] = MultiTainter.taintedChar(s[i], i);
		}

		for (int i = 1; i <= 5; i++) {
			int ret = Character.codePointBefore(s, i);
			assertEquals(MultiTainter.getTaint(ret).getLabels()[0], i-1);
		}
	}
}
