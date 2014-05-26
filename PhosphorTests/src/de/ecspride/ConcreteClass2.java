package de.ecspride;

import phosphor.test.DroidBenchTest;

public class ConcreteClass2 extends BaseClass2 {

	@Override
	public String foo() {
		return DroidBenchTest.taintedString();
	}
	
	@Override
	public void bar(String s) {
		assert(DroidBenchTest.getTaint(s) != 0);
	}
	
}
