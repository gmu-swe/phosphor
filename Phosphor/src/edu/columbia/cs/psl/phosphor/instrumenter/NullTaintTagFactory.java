package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

public class NullTaintTagFactory implements EmptyTaintTagFactory {

	@Override
	public void generateEmptyTaint(MethodVisitor mv) {
		mv.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
	}

	@Override
	public void generateEmptyTaintArray(Object[] array, int dims) {
		
	}

}
