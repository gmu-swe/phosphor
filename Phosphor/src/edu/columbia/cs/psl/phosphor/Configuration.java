package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class Configuration {
//	public static final String TAINT_TAG_DESC = "I";
//	public static final String TAINT_TAG_ARRAYDESC = "[I";
//	public static String TAINT_TAG_INTERNAL_NAME = null;	
//	public static String TAINT_TAG_ARRAY_INTERNAL_NAME = "[I";
//	public static final int NULL_TAINT_LOAD_OPCODE = Opcodes.ICONST_0;
//	public static final Object TAINT_TAG_STACK_TYPE = Opcodes.INTEGER;
//	public static final Object TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;
//
//	public static final int TAINT_TAG_TYPE = Type.INT;
//	public static final Class TAINT_TAG_CLASS = Integer.TYPE;
//	public static final Class TAINT_TAG_ARRAY_CLASS = int[].class;

	
	public static final String TAINT_TAG_DESC = "Ledu/columbia/cs/psl/phosphor/runtime/Taint;";
	public static final String TAINT_TAG_ARRAYDESC = "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;";
	public static String TAINT_TAG_INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/Taint";	
	public static String TAINT_TAG_ARRAY_INTERNAL_NAME = "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;";
	public static final int NULL_TAINT_LOAD_OPCODE = Opcodes.ACONST_NULL;
	public static final Object TAINT_TAG_STACK_TYPE = "edu/columbia/cs/psl/phosphor/runtime/Taint";
	public static final Object TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;

	public static final int TAINT_TAG_TYPE = Type.OBJECT;
	public static final Class TAINT_TAG_CLASS = Taint.class;
	public static final Class TAINT_TAG_ARRAY_CLASS = Taint[].class;
	
	
	public static boolean IMPLICIT_TRACKING = false; //default 

	public static final String MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/SimpleMultiTaintHandler";
	
	public static Object[] newTaintArray(int len)
	{
		return new Taint[len];
	}
}
