package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.*;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurJSRInlinerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurSerialVersionUIDAdder;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.ProtectionDomain;

public class PreMain {

    public static boolean DEBUG = System.getProperty("phosphor.debug") != null;
    public static boolean RUNTIME_INST = false;
    public static boolean INSTRUMENTATION_EXCEPTION_OCCURRED = false;
    public static ClassLoader bigLoader = PreMain.class.getClassLoader();

    /**
     * As I write this I realize what a multithreaded classloader mess this can create... let's see how bad it is.
     */
    public static ClassLoader curLoader;
    private static Instrumentation instrumentation;

    private PreMain() {
        // Prevents this class from being instantiated
    }

    public static void premain$$PHOSPHORTAGGED(String args, Instrumentation inst, ControlFlowStack ctrl) {
        Configuration.IMPLICIT_TRACKING = true;
        Configuration.init();
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassSupertypeReadingTransformer());
        RUNTIME_INST = true;
        if(args != null) {
            PhosphorOption.configure(true, parseArgs(args));
        }
        if(System.getProperty("phosphorCacheDirectory") != null) {
            Configuration.CACHE_DIR = System.getProperty("phosphorCacheDirectory");
            File f = new File(Configuration.CACHE_DIR);
            if(!f.exists()) {
                if(!f.mkdir()) {
                    // The cache directory did not exist and the attempt to create it failed
                    System.err.printf("Failed to create cache directory: %s. Generated files are not being cached.\n", Configuration.CACHE_DIR);
                    Configuration.CACHE_DIR = null;
                }
            }
        }
        if(Instrumenter.loader == null) {
            Instrumenter.loader = bigLoader;
        }
        // Ensure that BasicSourceSinkManager & anything needed to call isSourceOrSinkOrTaintThrough gets initialized
        BasicSourceSinkManager.getInstance().isSourceOrSinkOrTaintThrough(Object.class);
        inst.addTransformer(new PCLoggingTransformer());
        inst.addTransformer(new SourceSinkTransformer(), true);
        instrumentation = inst;
    }

    private static String[] parseArgs(String argString) {
        String[] args = argString.split(",");
        SinglyLinkedList<String> argList = new SinglyLinkedList<>();
        for(String arg : args) {
            int split = arg.indexOf('=');
            if(split == -1) {
                argList.addLast("-" + arg);
            } else {
                String option = arg.substring(0, split);
                String value = arg.substring(split + 1);
                argList.addLast("-" + option);
                argList.addLast(value);
            }
        }
        return argList.toArray(new String[0]);
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static final class PCLoggingTransformer extends PhosphorBaseTransformer {
        static boolean innerException = false;
        static MessageDigest md5inst;

        public PCLoggingTransformer() {
            TaintUtils.VERIFY_CLASS_GENERATION = System.getProperty("phosphor.verify") != null;
        }

        @Override
        public byte[] transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            return _transform(loader, className2, classBeingRedefined, protectionDomain, classfileBuffer);
        }

        static byte[] instrumentWithRetry(ClassReader cr, byte[] classFileBuffer, boolean isiFace, String className, boolean skipFrames, boolean upgradeVersion, List<FieldNode> fields, Set<String> methodsToReduceSizeOf, boolean traceClass) throws InstantiationException {
            TraceClassVisitor debugTracer = null;
            try {
                try {
                    ClassWriter cw = new HackyClassWriter(null, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor _cv = cw;
                    _cv = new ClassVisitor(Opcodes.ASM7, cw) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if(name.endsWith("$PHOSPHORTAGGED$$PHOSPHORTAGGED")){
                                throw new IllegalArgumentException();
                            }
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    };
                    if(traceClass) {
                        System.out.println("Saving " + className + " to debug-preinst/");
                        File f = new File("debug-preinst/" + className.replace("/", ".") + ".class");
                        if(!f.getParentFile().isDirectory() && !f.getParentFile().mkdirs()) {
                            System.err.println("Failed to make debug directory: " + f);
                        } else {
                            try {
                                FileOutputStream fos = new FileOutputStream(f);
                                fos.write(classFileBuffer);
                                fos.close();
                            } catch(Exception ex2) {
                                ex2.printStackTrace();
                            }
                        }
                        debugTracer = new TraceClassVisitor(null, null);
                        _cv = debugTracer;
                    }
                    if(Configuration.extensionClassVisitor != null) {
                        Constructor<? extends ClassVisitor> extra = Configuration.extensionClassVisitor.getConstructor(ClassVisitor.class, Boolean.TYPE);
                        _cv = extra.newInstance(_cv, skipFrames);
                    }
                    if(DEBUG || TaintUtils.VERIFY_CLASS_GENERATION) {
                        _cv = new CheckClassAdapter(_cv, false);
                    }
                    if(SerializationFixingCV.isApplicable(className)) {
                        _cv = new SerializationFixingCV(_cv, className);
                    }
                    _cv = new ClinitRetransformClassVisitor(_cv);
                    if(isiFace) {
                        _cv = new TaintTrackingClassVisitor(_cv, skipFrames, fields, methodsToReduceSizeOf);
                    } else {
                        _cv = new OurSerialVersionUIDAdder(new TaintTrackingClassVisitor(_cv, skipFrames, fields, methodsToReduceSizeOf));
                    }
                    if(EclipseCompilerCV.isEclipseCompilerClass(className)) {
                        _cv = new EclipseCompilerCV(_cv);
                    }
                    if(OgnlUtilCV.isOgnlUtilClass(className) && !Configuration.REENABLE_CACHES) {
                        _cv = new OgnlUtilCV(_cv);
                    }
                    if(JettyBufferUtilCV.isApplicable(className)) {
                        _cv = new JettyBufferUtilCV(_cv);
                    }
                    if(PowerMockUtilCV.isApplicable(className)) {
                        _cv = new PowerMockUtilCV(_cv);
                    }
                    if(Configuration.PRIOR_CLASS_VISITOR != null) {
                        try {
                            Constructor<? extends ClassVisitor> extra = Configuration.PRIOR_CLASS_VISITOR.getConstructor(ClassVisitor.class, Boolean.TYPE);
                            _cv = extra.newInstance(_cv, skipFrames);
                        } catch(Exception e) {
                            //
                        }
                    }
                    _cv = new HidePhosphorFromASMCV(_cv, upgradeVersion);
                    cr.accept(_cv, ClassReader.EXPAND_FRAMES);
                    byte[] instrumentedBytes = cw.toByteArray();
                    if (!traceClass && (DEBUG || TaintUtils.VERIFY_CLASS_GENERATION)) {

                        ClassReader cr2 = new ClassReader(instrumentedBytes);
                        try {
                            cr2.accept(new CheckClassAdapter(new ClassWriter(0), true), ClassReader.EXPAND_FRAMES);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            File f = new File("debug-verify/" + className.replace("/", ".") + ".class");
                            if (!f.getParentFile().isDirectory() && !f.getParentFile().mkdirs()) {
                                System.err.println("Failed to make debug directory: " + f);
                            } else {
                                try {
                                    FileOutputStream fos = new FileOutputStream(f);
                                    fos.write(instrumentedBytes);
                                    fos.close();
                                } catch (Exception ex2) {
                                    ex2.printStackTrace();
                                }
                                System.out.println("Saved broken class to " + f);
                            }
                        }
                    }

                    return instrumentedBytes;
                } catch(MethodTooLargeException ex) {
                    if(methodsToReduceSizeOf == null) {
                        methodsToReduceSizeOf = new HashSet<>();
                    }
                    methodsToReduceSizeOf.add(ex.getMethodName() + ex.getDescriptor());
                    return instrumentWithRetry(cr, classFileBuffer, isiFace, className, skipFrames, upgradeVersion, fields,  methodsToReduceSizeOf, false);
                }
            } catch (Throwable ex) {
                INSTRUMENTATION_EXCEPTION_OCCURRED = true;
                if (!traceClass) {
                    System.err.println("Exception occurred while instrumenting " + className + ":");
                    ex.printStackTrace();
                    instrumentWithRetry(cr, classFileBuffer, isiFace, className, skipFrames, upgradeVersion, fields,  methodsToReduceSizeOf, true);
                    return classFileBuffer;
                }
                ex.printStackTrace();
                System.err.println("method so far:");
                try {
                    PrintWriter pw = new PrintWriter(new FileWriter("lastClass.txt"));
                    debugTracer.p.print(pw);
                    pw.flush();
                } catch (IOException ex2) {
                    ex2.printStackTrace();
                }
                return classFileBuffer;
            }
        }

        public static byte[] _transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            ClassReader cr = (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(classfileBuffer) : new ClassReader(classfileBuffer));
            String className = cr.getClassName();
            curLoader = loader;
            if(Instrumenter.isIgnoredClass(className)) {
                switch(className) {
                    case "java/lang/Boolean":
                    case "java/lang/Byte":
                    case "java/lang/Character":
                    case "java/lang/Short":
                        return processBoolean(classfileBuffer);
                }
                return classfileBuffer;
            }

            Configuration.taintTagFactory.instrumentationStarting(className);
            try {
                ClassNode cn = new ClassNode();
                cr.accept(cn, (Configuration.ALWAYS_CHECK_FOR_FRAMES ? 0 : ClassReader.SKIP_CODE));
                boolean skipFrames = false;
                boolean upgradeVersion = false;
                if(className.equals("org/jruby/parser/Ruby20YyTables")) {
                    cn.version = 51;
                    upgradeVersion = true;
                }
                if(cn.version >= 100 || cn.version <= 50 || className.endsWith("$Access4JacksonSerializer") || className.endsWith("$Access4JacksonDeSerializer")) {
                    skipFrames = true;
                } else if(Configuration.ALWAYS_CHECK_FOR_FRAMES) {
                    for(MethodNode mn : cn.methods) {
                        boolean hasJumps = false;
                        boolean foundFrame = false;
                        AbstractInsnNode ins = mn.instructions.getFirst();
                        if(!mn.tryCatchBlocks.isEmpty()) {
                            hasJumps = true;
                        }
                        while(ins != null) {
                            if(ins instanceof JumpInsnNode || ins instanceof TableSwitchInsnNode || ins instanceof LookupSwitchInsnNode) {
                                hasJumps = true;
                            }
                            if(ins instanceof FrameNode) {
                                foundFrame = true;
                                break;
                            }
                            ins = ins.getNext();
                        }
                        if(foundFrame) {
                            break;
                        }
                        if(hasJumps) {
                            skipFrames = true;
                            break;
                        }
                    }
                }
                if(cn.visibleAnnotations != null) {
                    for(Object o : cn.visibleAnnotations) {
                        AnnotationNode an = (AnnotationNode) o;
                        if(an.desc.equals(Type.getDescriptor(TaintInstrumented.class))) {
                            return classfileBuffer;
                        }
                    }
                }
                if(cn.interfaces != null) {
                    for(Object s : cn.interfaces) {
                        if(s.equals(Type.getInternalName(TaintedWithObjTag.class))) {
                            return classfileBuffer;
                        }
                    }
                }
                for(Object mn : cn.methods) {
                    if(((MethodNode) mn).name.equals("getPHOSPHOR_TAG")) {
                        return classfileBuffer;
                    }
                }
                if(Configuration.CACHE_DIR != null) {
                    String cacheKey = className.replace("/", ".");
                    File f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".md5sum");
                    if(f.exists()) {
                        try {
                            FileInputStream fis = new FileInputStream(f);
                            byte[] cachedDigest = new byte[1024];
                            fis.read(cachedDigest);
                            fis.close();
                            if(md5inst == null) {
                                md5inst = MessageDigest.getInstance("MD5");
                            }
                            byte[] checksum;
                            synchronized(md5inst) {
                                checksum = md5inst.digest(classfileBuffer);
                            }
                            boolean matches = true;
                            if(checksum.length > cachedDigest.length) {
                                matches = false;
                            }
                            if(matches) {
                                for(int i = 0; i < checksum.length; i++) {
                                    if(checksum[i] != cachedDigest[i]) {
                                        matches = false;
                                        break;
                                    }
                                }
                            }
                            if(matches) {
                                return Files.readAllBytes(new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".class").toPath());
                            }
                        } catch(Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
                if(DEBUG) {
                    try {
                        File debugDir = new File("debug-preinst");
                        if(!debugDir.exists()) {
                            debugDir.mkdir();
                        }
                        File f = new File("debug-preinst/" + className.replace("/", ".") + ".class");
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(classfileBuffer);
                        fos.close();
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }

                boolean isiFace = (cn.access & Opcodes.ACC_INTERFACE) != 0;
                List<FieldNode> fields = new LinkedList<>();
                for(FieldNode node : cn.fields) {
                    fields.add(node);
                }
                if(skipFrames) {
                    // This class is old enough to not guarantee frames.
                    // Generate new frames for analysis reasons, then make sure
                    // to not emit ANY frames.
                    ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    cr.accept(new ClassVisitor(Configuration.ASM_VERSION, cw) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                            return new OurJSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
                        }
                    }, 0);
                    cr = (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(cw.toByteArray()) : new ClassReader(cw.toByteArray()));
                }
                // Find out if this class already has frames
                TraceClassVisitor cv;
                try {
                    byte[] instrumentedBytes = instrumentWithRetry(cr, classfileBuffer, isiFace, className, skipFrames, upgradeVersion, fields, null, false);

                    if(DEBUG) {
                        File f = new File("debug/" + className + ".class");
                        f.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(instrumentedBytes);
                        fos.close();
                    }

                    if(Configuration.CACHE_DIR != null) {
                        String cacheKey = className.replace("/", ".");
                        File f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".class");
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(instrumentedBytes);
                        fos.close();
                        if(md5inst == null) {
                            md5inst = MessageDigest.getInstance("MD5");
                        }
                        byte[] checksum;
                        synchronized(md5inst) {
                            checksum = md5inst.digest(classfileBuffer);
                        }
                        f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".md5sum");
                        fos = new FileOutputStream(f);

                        fos.write(checksum);
                        fos.close();
                        return instrumentedBytes;
                    }
                    return instrumentedBytes;
                } catch(Throwable ex) {
                    ex.printStackTrace();
                    throw new IllegalStateException(ex);
                }
            } finally {
                Configuration.taintTagFactory.instrumentationEnding(className);
            }
        }

        private static byte[] processBoolean(byte[] classFileBuffer) {
            ClassReader cr = new ClassReader(classFileBuffer);
            ClassNode cn = new ClassNode(Configuration.ASM_VERSION);
            cr.accept(cn, 0);
            boolean addField = true;
            for(Object o : cn.fields) {
                FieldNode fn = (FieldNode) o;
                if(fn.name.equals("valueOf")) {
                    addField = false;
                    break;
                }
            }
            for(Object o : cn.methods) {
                MethodNode mn = (MethodNode) o;
                if(mn.name.startsWith("toUpperCase") || mn.name.startsWith("codePointAtImpl") || mn.name.startsWith("codePointBeforeImpl")) {
                    mn.access = mn.access | Opcodes.ACC_PUBLIC;
                }
            }
            if(addField) {
                cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "valueOf", "Z", null, false));
                ClassWriter cw = new ClassWriter(0);
                cn.accept(cw);
                return cw.toByteArray();
            }
            return classFileBuffer;
        }

        private static final class HackyClassWriter extends ClassWriter {

            private HackyClassWriter(ClassReader classReader, int flags) {
                super(classReader, flags);
            }

            private Class<?> getClass(String name) throws ClassNotFoundException {
                if(RUNTIME_INST) {
                    throw new ClassNotFoundException();
                }
                try {
                    return Class.forName(name.replace("/", "."), false, bigLoader);
                } catch(SecurityException e) {
                    throw new ClassNotFoundException("Security exception when loading class");
                } catch(Throwable e) {
                    throw new ClassNotFoundException();
                }
            }

            protected String getCommonSuperClass(String type1, String type2) {
                Class<?> c, d;
                try {
                    c = getClass(type1);
                    d = getClass(type2);
                } catch(ClassNotFoundException | ClassCircularityError e) {
                    return "java/lang/Object";
                }
                if(c.isAssignableFrom(d)) {
                    return type1;
                }
                if(d.isAssignableFrom(c)) {
                    return type2;
                }
                if(c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while(!c.isAssignableFrom(d));
                    return c.getName().replace('.', '/');
                }
            }
        }
    }
}
