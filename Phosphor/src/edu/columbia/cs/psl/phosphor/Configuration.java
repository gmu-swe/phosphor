package edu.columbia.cs.psl.phosphor;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.phosphor.runtime.DerivedTaintListener;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class Configuration {
	public static String ADDL_IGNORE = null;
	public static boolean MULTI_TAINTING = true;
	public static boolean IMPLICIT_TRACKING = true; // must be set to TRUE for MULTI_TAINTING to work!
	public static boolean DATAFLOW_TRACKING = true; // default
	public static boolean ARRAY_LENGTH_TRACKING = false;
	public static boolean WITH_ENUM_BY_VAL = false;
	public static boolean WITH_UNBOX_ACMPEQ = false;

	public static boolean WITH_SELECTIVE_INST = false;
	public static String selective_inst_config;
	
	public static boolean WITHOUT_PROPOGATION = false;
	public static boolean WITHOUT_FIELD_HIDING = false;
	
	public static boolean GENERATE_UNINST_STUBS = false;
	
	/*
	 * Derived configuration values
	 */
	public static String TAINT_TAG_DESC = (MULTI_TAINTING
			? "Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
			: "I");
	public static String TAINT_TAG_ARRAYDESC = (MULTI_TAINTING
			? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
			: "[I");
	public static String TAINT_TAG_INTERNAL_NAME = (MULTI_TAINTING
			? "edu/columbia/cs/psl/phosphor/runtime/Taint"
			: null);
	public static String TAINT_TAG_ARRAY_INTERNAL_NAME = (MULTI_TAINTING
			? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
			: "[I");
	public static int NULL_TAINT_LOAD_OPCODE = (MULTI_TAINTING
			? Opcodes.ACONST_NULL
			: Opcodes.ICONST_0);
	public static Object TAINT_TAG_STACK_TYPE = (MULTI_TAINTING
			? "edu/columbia/cs/psl/phosphor/runtime/Taint"
			: Opcodes.INTEGER);
	public static Object TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;
	public static String MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/Taint";
	public static String TAINTED_INT_INTERNAL_NAME = (MULTI_TAINTING
			? "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithObjTag"
			: "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithIntTag");
	public static String TAINTED_INT_DESC = "L" + TAINTED_INT_INTERNAL_NAME + ";";
	public static int TAINT_ARRAY_LOAD_OPCODE = (MULTI_TAINTING
			? Opcodes.AALOAD
			: Opcodes.IALOAD);
	public static int TAINT_ARRAY_STORE_OPCODE = (MULTI_TAINTING
			? Opcodes.AASTORE
			: Opcodes.IASTORE);
	public static int TAINT_LOAD_OPCODE = (MULTI_TAINTING
			? Opcodes.ALOAD
			: Opcodes.ILOAD);
	public static int TAINT_STORE_OPCODE = (MULTI_TAINTING
			? Opcodes.ASTORE
			: Opcodes.ISTORE);
	public static boolean OPT_CONSTANT_ARITHMETIC = !IMPLICIT_TRACKING;
	public static Class TAINT_TAG_OBJ_CLASS = (Taint.class);
	public static Class TAINT_TAG_OBJ_ARRAY_CLASS = (Taint[].class);

	public static Class<? extends TaintAdapter> extensionMethodVisitor;
	public static Class extensionClassVisitor;

	public static TaintTagFactory taintTagFactory = new DataAndControlFlowTagFactory();
	public static DerivedTaintListener derivedTaintListener;
	public static String CACHE_DIR = null;


	public static void init() {
		TAINT_TAG_DESC = (MULTI_TAINTING
				? "Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
				: "I");
		TAINT_TAG_ARRAYDESC = (MULTI_TAINTING
				? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
				: "[I");
		TAINT_TAG_INTERNAL_NAME = (MULTI_TAINTING
				? "edu/columbia/cs/psl/phosphor/runtime/Taint"
				: null);
		TAINT_TAG_ARRAY_INTERNAL_NAME = (MULTI_TAINTING
				? "[Ledu/columbia/cs/psl/phosphor/runtime/Taint;"
				: "[I");
		NULL_TAINT_LOAD_OPCODE = (MULTI_TAINTING
				? Opcodes.ACONST_NULL
				: Opcodes.ICONST_0);
		TAINT_TAG_STACK_TYPE = (MULTI_TAINTING
				? "edu/columbia/cs/psl/phosphor/runtime/Taint"
				: Opcodes.INTEGER);
		TAINT_TAG_ARRAY_STACK_TYPE = TAINT_TAG_ARRAY_INTERNAL_NAME;
		MULTI_TAINT_HANDLER_CLASS = "edu/columbia/cs/psl/phosphor/runtime/Taint";
		TAINTED_INT_INTERNAL_NAME = (MULTI_TAINTING
				? "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithObjTag"
				: "edu/columbia/cs/psl/phosphor/struct/TaintedIntWithIntTag");
		TAINTED_INT_DESC = "L" + TAINTED_INT_INTERNAL_NAME + ";";
		TAINT_ARRAY_LOAD_OPCODE = (MULTI_TAINTING
				? Opcodes.AALOAD
				: Opcodes.IALOAD);
		TAINT_ARRAY_STORE_OPCODE = (MULTI_TAINTING
				? Opcodes.AASTORE
				: Opcodes.IASTORE);
		TAINT_LOAD_OPCODE = (MULTI_TAINTING
				? Opcodes.ALOAD
				: Opcodes.ILOAD);
		TAINT_STORE_OPCODE = (MULTI_TAINTING
				? Opcodes.ASTORE
				: Opcodes.ISTORE);
		OPT_CONSTANT_ARITHMETIC = !IMPLICIT_TRACKING;
		TAINT_TAG_OBJ_ARRAY_CLASS = (MULTI_TAINTING
				? Taint[].class
				:int[].class);
		TAINT_TAG_OBJ_CLASS = (MULTI_TAINTING
				? Taint.class
				: Integer.TYPE);
		
		if(WITH_SELECTIVE_INST)
			GENERATE_UNINST_STUBS = true;

		if (TaintTrackingClassVisitor.class != null
				&& TaintTrackingClassVisitor.class.getClassLoader() != null) {

			URL r = TaintTrackingClassVisitor.class
				.getClassLoader().getResource("phosphor-mv");

			if (r != null)
				try {
					Properties props = new Properties();
					props.load(r.openStream());
					if (props.containsKey("extraMV"))
						extensionMethodVisitor =
							(Class<? extends TaintAdapter>)
							Class.forName(props.getProperty("extraMV"));
					if (props.containsKey("extraCV"))
						extensionClassVisitor =
							(Class<? extends ClassVisitor>)
							Class.forName(props.getProperty("extraCV"));
					if (props.containsKey("taintTagFactory"))
						taintTagFactory = (TaintTagFactory)
							Class.forName(props.getProperty("taintTagFactory")).newInstance();
					if (props.containsKey("derivedTaintListener"))
						derivedTaintListener = (DerivedTaintListener)
							Class.forName(props.getProperty("derivedTaintListener")).newInstance();
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
}
