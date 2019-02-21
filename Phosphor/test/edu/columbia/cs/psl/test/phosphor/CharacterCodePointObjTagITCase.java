package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

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
}
