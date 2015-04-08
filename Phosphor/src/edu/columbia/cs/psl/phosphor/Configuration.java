package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class Configuration {
	public static boolean MULTI_TAINTING = true;
	public static boolean IMPLICIT_TRACKING = true; //must be set to TRUE for MULTI_TAINTING to work!
	public static boolean DATAFLOW_TRACKING = true; //default

	/*
	 * Derived configuration values
	 */
	public static String TAINT_TAG_DESC = (MULTI_TAINTING ? "Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "I");
	public static String TAINT_TAG_ARRAYDESC = (MULTI_TAINTING ? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "[I");
	public static String TAINT_TAG_INTERNAL_NAME = (MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/runtime/Taint" : null);
	public static String TAINT_TAG_ARRAY_INTERNAL_NAME = (MULTI_TAINTING ? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "[I");
	public static int NULL_TAINT_LOAD_OPCODE = (MULTI_TAINTING ? Opcodes.ACONST_NULL : Opcodes.ICONST_0);
	public static Object TAINT_TAG_STACK_TYPE = (MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/runtime/Taint" : Opcodes.INTEGER);
	public static Object TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;
	public static String MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/Taint";
	public static String TAINTED_INT_INTERNAL_NAME = (!MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithIntTag" : "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithObjTag");
	public static String TAINTED_INT_DESC = "L" + TAINTED_INT_INTERNAL_NAME + ";";
	public static int TAINT_ARRAY_LOAD_OPCODE = (!MULTI_TAINTING ? Opcodes.IALOAD : Opcodes.AALOAD);
	public static int TAINT_ARRAY_STORE_OPCODE = (!MULTI_TAINTING ? Opcodes.IASTORE : Opcodes.AASTORE);
	public static int TAINT_LOAD_OPCODE = (!MULTI_TAINTING ? Opcodes.ILOAD : Opcodes.ALOAD);
	public static int TAINT_STORE_OPCODE = (!MULTI_TAINTING ? Opcodes.ISTORE : Opcodes.ASTORE);
	public static boolean OPT_CONSTANT_ARITHMETIC = true && !IMPLICIT_TRACKING;
	public static Class TAINT_TAG_OBJ_CLASS = (Taint.class);
	public static Class TAINT_TAG_OBJ_ARRAY_CLASS = (Taint[].class);
	public static void init()
	{
		 TAINT_TAG_DESC = (MULTI_TAINTING ? "Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "I");
		 TAINT_TAG_ARRAYDESC = (MULTI_TAINTING ? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "[I");
		 TAINT_TAG_INTERNAL_NAME = (MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/runtime/Taint" : null);
		 TAINT_TAG_ARRAY_INTERNAL_NAME = (MULTI_TAINTING ? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;" : "[I");
		 NULL_TAINT_LOAD_OPCODE = (MULTI_TAINTING ? Opcodes.ACONST_NULL : Opcodes.ICONST_0);
		 TAINT_TAG_STACK_TYPE = (MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/runtime/Taint" : Opcodes.INTEGER);
		 TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;
		 MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/Taint";
		 TAINTED_INT_INTERNAL_NAME = (!MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithIntTag" : "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithObjTag");
		 TAINTED_INT_DESC = "L" + TAINTED_INT_INTERNAL_NAME + ";";
		 TAINT_ARRAY_LOAD_OPCODE = (!MULTI_TAINTING ? Opcodes.IALOAD : Opcodes.AALOAD);
		 TAINT_ARRAY_STORE_OPCODE = (!MULTI_TAINTING ? Opcodes.IASTORE : Opcodes.AASTORE);
		 TAINT_LOAD_OPCODE = (!MULTI_TAINTING ? Opcodes.ILOAD : Opcodes.ALOAD);
		 TAINT_STORE_OPCODE = (!MULTI_TAINTING ? Opcodes.ISTORE : Opcodes.ASTORE);
		 OPT_CONSTANT_ARITHMETIC = true && !IMPLICIT_TRACKING;
		 TAINT_TAG_OBJ_ARRAY_CLASS = (MULTI_TAINTING ? Taint[].class : int[].class);
		 TAINT_TAG_OBJ_CLASS = (MULTI_TAINTING ? Taint.class : Integer.TYPE);
	}
}
