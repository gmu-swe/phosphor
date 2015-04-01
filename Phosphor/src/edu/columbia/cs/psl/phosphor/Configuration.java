package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

public class Configuration {
	public static final String TAINT_TAG_DESC = "I";
	public static final Object TAINT_TAG_STACK_TYPE = Opcodes.INTEGER;
	public static final int TAINT_TAG_TYPE = Type.INT;
	
	public static boolean IMPLICIT_TRACKING = false; //default 
	public static final String MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/SimpleMultiTaintHandler";
}
