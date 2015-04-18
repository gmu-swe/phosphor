package de.ecspride;

import phosphor.test.DroidBenchTest;
import phosphor.test.DroidBenchTestMultiTaint;

public class ConcreteClass2 extends BaseClass2 {

	@Override
	public String foo() {
		return DroidBenchTest.taintedString();
	}
	
	@Override
	public void bar(String s) {
		assert(DroidBenchTest.getTaint(s) != 0);
	}

	@Override
	public String fooMultiTaint() {
		return DroidBenchTestMultiTaint.taintedString();
	}

	@Override
	public void barMultiTaint(String s) {
		assert(DroidBenchTestMultiTaint.getTaint(s) != 0);
	}
	
}
