package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

public class Configuration {	
	public static final int TAINT_TAG_TYPE = Type.INT; // or Type.INT;
	public static boolean IMPLICIT_TRACKING = false; //default 
	public static boolean DATAFLOW_TRACKING = true; //default
}
