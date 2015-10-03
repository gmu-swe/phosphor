package de.ecspride;

import edu.columbia.cs.psl.test.phosphor.DroidBenchIntTagITCase;
import edu.columbia.cs.psl.test.phosphor.DroidBenchObjTagITCase;

public class VarA extends General{
	@Override
	public String getInfo() {
		return DroidBenchIntTagITCase.taintedString("abcd");
	}
	@Override
	public String getInfoMultiTaint() {
		return DroidBenchObjTagITCase.taintedString("abcd");
	}
}
