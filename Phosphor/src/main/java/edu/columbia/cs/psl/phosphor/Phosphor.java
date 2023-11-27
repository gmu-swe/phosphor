package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.StringUtils;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManagerPackage;
import static edu.columbia.cs.psl.phosphor.Configuration.taintTagFactoryPackage;

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

    public static boolean isIgnoredClass(String owner) {
        return Configuration.taintTagFactory.isIgnoredClass(owner)
                || taintTagFactoryPackage != null && StringUtils.startsWith(owner, taintTagFactoryPackage)
                || controlFlowManagerPackage != null && StringUtils.startsWith(owner, controlFlowManagerPackage)
                || (Configuration.controlFlowManager != null && Configuration.controlFlowManager.isIgnoredClass(owner))
                || (Configuration.ADDL_IGNORE != null && StringUtils.startsWith(owner, Configuration.ADDL_IGNORE))
                // || !owner.startsWith("edu/columbia/cs/psl")
                // For these classes: HotSpot expects fields to be at hardcoded offsets of these classes.
                // If we instrument them, it will break those assumptions and segfault.
                // Different classes have different assumptions, and there are special cases for these elsewhere in
                // Phosphor.
                || StringUtils.startsWith(owner, "java/lang/Object")
                || StringUtils.startsWith(owner, "java/lang/Boolean")
                || StringUtils.startsWith(owner, "java/lang/Character")
                || StringUtils.startsWith(owner, "java/lang/Byte")
                || StringUtils.startsWith(owner, "java/lang/Short")
                || StringUtils.startsWith(owner, "java/lang/Number")
                || StringUtils.startsWith(owner, "java/lang/ref/Reference")
                || StringUtils.startsWith(owner, "java/lang/ref/FinalReference")
                || StringUtils.startsWith(owner, "java/lang/ref/SoftReference")
                // Lambdas are hosted by this class, and when generated, will have hard-coded offsets to constant pool
                || StringUtils.equals(owner, "java/lang/invoke/LambdaForm")
                // Lambdas are hosted by this class, and when generated, will have hard-coded offsets to constant pool
                || StringUtils.startsWith(owner, "java/lang/invoke/LambdaForm$")
                // Phosphor internal classes
                || StringUtils.startsWith(owner, "edu/columbia/cs/psl/phosphor")
                || StringUtils.startsWith(owner, "edu/gmu/swe/phosphor/ignored")
                // Reflection is handled by:
                // DelegatingMethod/ConstructorAccessorImpl calls either NativeMethod/ConstructorAccessorImpl OR
                // calls GeneratedMethod/ConstructorAccessorImpl.
                // We do the wrapping at the delegating level.
                // Generated code won't be instrumented.
                // Hence, it's convenient to also not wrap the native version,
                // so that passed params/returns line up exactly when we hit the reflected call
                || StringUtils.startsWith(owner, "edu/columbia/cs/psl/phosphor/struct/TaintedWith")
                // Java 9+ class full of hardcoded offsets
                || StringUtils.startsWith(owner, "jdk/internal/misc/UnsafeConstants");
    }

    public static boolean isIgnoredMethod(String owner, String name, String desc) {
        return false; // TODO see below from old jdk14 version
        // if(name.equals("wait") && desc.equals("(J)V")) {
        //    return true;
        // }
        // if(name.equals("wait") && desc.equals("(JI)V")) {
        //    return true;
        // }
        // if (owner.equals("jdk/internal/reflect/Reflection") && name.equals("getCallerClass")) {
        //    return true;
        // }
        // if (owner.equals("java/lang/invoke/MethodHandle")
        //        && ((name.equals("invoke") || name.equals("invokeBasic") || name.startsWith("linkTo")))) {
        //    return true;
        // }
        // return owner.equals("java/lang/invoke/VarHandle"); //TODO wrap these all
    }

    public static boolean isPolymorphicSignatureMethod(String owner, String name) {
        if (owner.equals("java/lang/invoke/VarHandle")) {
            switch (name) {
                case "get":
                case "set":
                case "getVolatile":
                case "setVolatile":
                case "getOpaque":
                case "setOpaque":
                case "getAcquire":
                case "setRelease":
                case "compareAndSet":
                case "compareAndExchange":
                case "compareAndExchangeAcquire":
                case "compareAndExchangeRelease":
                case "weakCompareAndSetPlain":
                case "weakCompareAndSet":
                case "weakCompareAndSetAcquire":
                case "weakCompareAndSetRelease":
                case "getAndSet":
                case "getAndSetAcquire":
                case "getAndSetRelease":
                case "getAndAdd":
                case "getAndAddAcquire":
                case "getAndAddRelease":
                case "getAndBitwiseOr":
                case "getAndBitwiseOrAcquire":
                case "getAndBitwiseOrRelease":
                case "getAndBitwiseAnd":
                case "getAndBitwiseAndAcquire":
                case "getAndBitwiseAndRelease":
                case "getAndBitwiseXor":
                case "getAndBitwiseXorAcquire":
                case "getAndBitwiseXorRelease":
                    return true;
            }
        }
        return false;
    }

    public static boolean isUninstrumentedField(String owner, String name) {
        return owner.equals("sun/java2d/cmm/lcms/LCMSImageLayout") && name.equals("dataArray");
    }

    public static boolean isUnsafeClass(String className) {
        return (Configuration.IS_JAVA_8 && "sun/misc/Unsafe".equals(className))
                || (!Configuration.IS_JAVA_8 && "jdk/internal/misc/Unsafe".equals(className));
    }
}
