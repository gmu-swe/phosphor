package de.ecspride;

import edu.columbia.cs.psl.test.phosphor.DroidBenchObjTagITCase;

public class VarA extends General {

	@Override
	public String getInfoMultiTaint() {
		return DroidBenchObjTagITCase.taintedString("abcd");
	}
}
