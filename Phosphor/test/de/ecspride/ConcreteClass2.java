package de.ecspride;

import static org.junit.Assert.*;


import edu.columbia.cs.psl.test.phosphor.DroidBenchIntTagITCase;
import edu.columbia.cs.psl.test.phosphor.DroidBenchObjTagITCase;


public class ConcreteClass2 extends BaseClass2 {

	@Override
	public String foo() {
		return DroidBenchIntTagITCase.taintedString();
	}
	
	@Override
	public void bar(String s) {
		assertTrue(DroidBenchIntTagITCase.getTaint(s) != 0);
	}

	@Override
	public String fooMultiTaint() {
		return DroidBenchObjTagITCase.taintedString();
	}

	@Override
	public void barMultiTaint(String s) {
		assertTrue(DroidBenchObjTagITCase.getTaint(s) != 0);
	}
	
}
