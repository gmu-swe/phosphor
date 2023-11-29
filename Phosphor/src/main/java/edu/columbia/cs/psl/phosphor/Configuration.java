package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.standard.StandardControlFlowManager;
import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.runtime.DerivedTaintListener;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class Configuration {
    public static final int ASM_VERSION = Opcodes.ASM9;
    public static final String TAINT_TAG_DESC = "Ledu/columbia/cs/psl/phosphor/runtime/Taint;";
    public static final String TAINT_TAG_INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/Taint";
    public static final String TAINT_TAG_ARRAY_INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/struct/TaggedArray";
    public static final Object TAINT_TAG_STACK_TYPE = "edu/columbia/cs/psl/phosphor/runtime/Taint";
    public static final int TAINT_LOAD_OPCODE = Opcodes.ALOAD;
    public static final Class<?> TAINT_TAG_OBJ_CLASS = (Taint.class);
    public static final boolean DEBUG_STACK_FRAME_WRAPPERS = false;
    public static boolean SKIP_LOCAL_VARIABLE_TABLE = false;
    public static String IGNORE = null;
    public static boolean REFERENCE_TAINTING = true;
    // default
    public static boolean DATAFLOW_TRACKING = true;
    public static boolean ARRAY_INDEX_TRACKING = false;
    // TODO need to set this at runtime somewhere
    public static boolean IMPLICIT_TRACKING = false;
    public static boolean IMPLICIT_LIGHT_TRACKING;
    public static boolean IMPLICIT_HEADERS_NO_TRACKING = false;
    public static boolean IMPLICIT_EXCEPTION_FLOW = false;
    public static boolean WITHOUT_BRANCH_NOT_TAKEN = false;
    public static boolean ANNOTATE_LOOPS = false;
    public static boolean WITH_ENUM_BY_VAL = false;
    public static boolean WITH_UNBOX_ACMPEQ = false;
    public static boolean WITHOUT_PROPAGATION = false;
    public static boolean WITHOUT_FIELD_HIDING = false;
    public static boolean READ_AND_SAVE_BCI = false;
    public static boolean ALWAYS_CHECK_FOR_FRAMES = false;
    public static boolean REENABLE_CACHES = false;
    public static Class<? extends ClassVisitor> PRIOR_CLASS_VISITOR = null;
    public static Class<? extends ClassVisitor> POST_CLASS_VISITOR = null;
    public static ControlFlowManager controlFlowManager = new StandardControlFlowManager();
    public static String controlFlowManagerPackage = null;
    public static boolean QUIET_MODE = false;
    // Option is set for Java 9+ JVM by the embedded configuration
    public static boolean IS_JAVA_8 = true;
    public static Set<String> ignoredMethods = new HashSet<>();
    public static TaintTagFactory taintTagFactory = new DataAndControlFlowTagFactory();
    public static String taintTagFactoryPackage = null;
    public static TaintSourceWrapper<?> autoTainter = new TaintSourceWrapper<>();
    public static DerivedTaintListener derivedTaintListener = new DerivedTaintListener();
    public static boolean TAINT_THROUGH_SERIALIZATION = true;

    private Configuration() {
        // Prevents this class from being instantiated
    }

    public static void init() {
        if (IMPLICIT_TRACKING) {
            ARRAY_INDEX_TRACKING = true;
        }
    }
}
