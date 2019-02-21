package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
public class OverTaintingImplicitITCase extends BasePhosphorTest {
	static int v1;
	static int v2;
	
	@Test
	public void testControlFlowCraziness() throws Exception {
		System.out.println("OK");
		v1 = 5;
		v2 = v1+6;
		//End Test
		System.out.println(v1);
		int v3 = v2 + 11;
//		assertEquals(22, v3);
		System.out.println(MultiTainter.getTaint(v3));
		System.out.println(MultiTainter.getTaint(v2));

		System.out.println(v2);
	}
}
