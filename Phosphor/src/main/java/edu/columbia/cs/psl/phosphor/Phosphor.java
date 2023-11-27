package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

public final class Phosphor {
    public static boolean DEBUG = System.getProperty("phosphor.debug") != null;
    public static boolean RUNTIME_INST = false;
    public static boolean INSTRUMENTATION_EXCEPTION_OCCURRED = false;
    public static ClassLoader bigLoader = Phosphor.class.getClassLoader();
    public static InstrumentationAdaptor instrumentation;

    private Phosphor() {
        throw new AssertionError("Tried to instantiate static agent class: " + getClass());
    }

    public static void initialize(String agentArgs, InstrumentationAdaptor instrumentation) {
        Phosphor.instrumentation = instrumentation;
        instrumentation.addTransformer(new ClassSupertypeReadingTransformer());
        RUNTIME_INST = true;
        if (agentArgs != null || Configuration.IS_JAVA_8) {
            PhosphorOption.configure(true, parseOptions(agentArgs));
        }
        if (System.getProperty("phosphorCacheDirectory") != null) {
            Configuration.CACHE = TransformationCache.getInstance(System.getProperty("phosphorCacheDirectory"));
        }
        if (Instrumenter.loader == null) {
            Instrumenter.loader = bigLoader;
        }
        // Ensure that BasicSourceSinkManager and anything needed to call isSourceOrSinkOrTaintThrough gets initialized
        BasicSourceSinkManager.loadTaintMethods();
        BasicSourceSinkManager.getInstance().isSourceOrSinkOrTaintThrough(Object.class);
        instrumentation.addTransformer(new PCLoggingTransformer());
        instrumentation.addTransformer(new SourceSinkTransformer(), true);
    }

    public static InstrumentationAdaptor getInstrumentation() {
        return instrumentation;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.INSTRUMENT_CLASS_BYTES)
    public static byte[] instrumentClassBytes(byte[] in) {
        return new PCLoggingTransformer().transform(null, null, null, null, in, false);
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.INSTRUMENT_CLASS_BYTES_ANONYMOUS)
    public static byte[] instrumentClassBytesAnonymous(byte[] in) {
        return new PCLoggingTransformer().transform(null, null, null, null, in, true);
    }

    private static String[] parseOptions(String agentArgs) {
        SinglyLinkedList<String> options = new SinglyLinkedList<>();
        if (agentArgs != null && !agentArgs.isEmpty()) {
            for (String arg : agentArgs.split(",")) {
                int split = arg.indexOf('=');
                if (split == -1) {
                    options.addLast("-" + arg);
                } else {
                    options.addLast("-" + arg.substring(0, split));
                    options.addLast(arg.substring(split + 1));
                }
            }
        }
        if (Configuration.IS_JAVA_8) {
            options.addLast("-java8");
        }
        return options.toArray(new String[0]);
    }
}
