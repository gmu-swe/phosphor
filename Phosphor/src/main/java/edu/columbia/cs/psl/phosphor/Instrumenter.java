package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.StringUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManagerPackage;
import static edu.columbia.cs.psl.phosphor.Configuration.taintTagFactoryPackage;

public final class Instrumenter {
    public static ClassLoader loader;
    public static Map<String, ClassNode> classes = Collections.synchronizedMap(new HashMap<>());
    public static InputStream sourcesFile;
    public static InputStream sinksFile;
    public static InputStream taintThroughFile;

    static {
        classes.putAll(ClassSupertypeReadingTransformer.classNodes);
        ClassSupertypeReadingTransformer.classNodes = null;
    }

    private Instrumenter() {
        throw new AssertionError("Tried to instantiate static utility class: " + getClass());
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

    /* Returns the class node associated with the specified class name or null if none exists and a new one could not
     * successfully be created for the class name. */
    public static ClassNode getClassNode(String className) {
        ClassNode cn = classes.get(className);
        if (cn == null) {
            // Class was loaded before ClassSupertypeReadingTransformer was added
            return tryToAddClassNode(className);
        } else {
            return cn;
        }
    }

    /* Attempts to create a ClassNode populated with supertype information for this class. */
    private static ClassNode tryToAddClassNode(String className) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(className + ".class")) {
            if (is == null) {
                return null;
            }
            ClassReader cr = new ClassReader(is);
            cr.accept(
                    new ClassVisitor(Configuration.ASM_VERSION) {
                        private ClassNode cn;

                        @Override
                        public void visit(
                                int version,
                                int access,
                                String name,
                                String signature,
                                String superName,
                                String[] interfaces) {
                            super.visit(version, access, name, signature, superName, interfaces);
                            cn = new ClassNode();
                            cn.name = name;
                            cn.superName = superName;
                            cn.interfaces = new java.util.ArrayList<>(java.util.Arrays.asList(interfaces));
                            cn.methods = new java.util.LinkedList<>();
                            classes.put(name, cn);
                        }

                        @Override
                        public MethodVisitor visitMethod(
                                int access, String name, String descriptor, String signature, String[] exceptions) {
                            cn.methods.add(new MethodNode(access, name, descriptor, signature, exceptions));
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    },
                    ClassReader.SKIP_CODE);
            return classes.get(className);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isUnsafeClass(String className) {
        return (Configuration.IS_JAVA_8 && "sun/misc/Unsafe".equals(className))
                || (!Configuration.IS_JAVA_8 && "jdk/internal/misc/Unsafe".equals(className));
    }
}
