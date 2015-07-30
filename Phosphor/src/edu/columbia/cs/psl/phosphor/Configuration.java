package edu.columbia.cs.psl.phosphor;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.runtime.DerivedTaintListener;
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

	public static Class<? extends TaintAdapter> extensionMethodVisitor;
	public static TaintTagFactory taintTagFactory;
	public static DerivedTaintListener derivedTaintListener;
	
	public static void init() {
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

		taintTagFactory = new DataAndControlFlowTagFactory();

		URL r = TaintTrackingClassVisitor.class.getClassLoader().getResource("phosphor-mv");
		if (r != null)
			try {
				Properties props = new Properties();
				props.load(r.openStream());
				if (props.containsKey("extraMV"))
					extensionMethodVisitor = (Class<? extends TaintAdapter>) Class.forName(props.getProperty("extraMV"));
				if (props.containsKey("taintTagFactory"))
					taintTagFactory = (TaintTagFactory) Class.forName(props.getProperty("taintTagFactory")).newInstance();
				if (props.containsKey("derivedTaintListener"))
					derivedTaintListener = (DerivedTaintListener) Class.forName(props.getProperty("derivedTaintListener")).newInstance();
			} catch (IOException ex) {
				//fail silently
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	}
}
