package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstOrUninstChoosingMV extends MethodVisitor {

	private UninstrumentedCompatMV umv;
	public InstOrUninstChoosingMV(TaintPassingMV tmv, UninstrumentedCompatMV umv) {
		super(Opcodes.ASM5, tmv);
		this.umv = umv;
	}

	public void disableTainting(){
		this.mv = umv;
	}

}
