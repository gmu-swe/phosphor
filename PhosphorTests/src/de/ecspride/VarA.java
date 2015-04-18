package de.ecspride;

import phosphor.test.DroidBenchTest;
import phosphor.test.DroidBenchTestMultiTaint;

public class VarA extends General{
	@Override
	public String getInfo() {
		return DroidBenchTest.taintedString("abcd");
	}
	@Override
	public String getInfoMultiTaint() {
		return DroidBenchTestMultiTaint.taintedString("abcd");
	}
}
