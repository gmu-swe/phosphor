package de.ecspride;

import phosphor.test.DroidBenchTest;

public class VarA extends General{
	@Override
	public String getInfo() {
		return DroidBenchTest.taintedString("abcd");
	}
}
