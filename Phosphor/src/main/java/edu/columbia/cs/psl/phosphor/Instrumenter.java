package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.runtime.StringUtils;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.UnsafeProxy;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.apache.commons.cli.CommandLine;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ModuleHashesAttribute;
import org.objectweb.asm.commons.ModuleResolutionAttribute;
import org.objectweb.asm.commons.ModuleTargetAttribute;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.concurrent.*;
import java.util.zip.*;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManagerPackage;
import static edu.columbia.cs.psl.phosphor.Configuration.taintTagFactoryPackage;

public class Instrumenter {
    // jmod magic number and version number, taken from java.base/jdk/internal/jmod/JmodFile.java
    private static final int JMOD_MAJOR_VERSION = 0x01;
    private static final int JMOD_MINOR_VERSION = 0x00;
    public static final byte[] JMOD_MAGIC_NUMBER = {
            0x4A, 0x4D, /* JM */
            JMOD_MAJOR_VERSION, JMOD_MINOR_VERSION, /* version 1.0 */
    };

    public static ClassLoader loader;
    public static Map<String, ClassNode> classes = Collections.synchronizedMap(new HashMap<>());
    public static InputStream sourcesFile;
    public static InputStream sinksFile;
    public static InputStream taintThroughFile;
    static String curPath;
    static int n = 0;
    private static ClassFileTransformer addlTransformer;

    static {
        classes.putAll(ClassSupertypeReadingTransformer.classNodes);
        ClassSupertypeReadingTransformer.classNodes = null;
    }

    private Instrumenter() {
        // Prevents this class from being instantiated
    }

    public static void preAnalysis() {

    }

    public static void finishedAnalysis() {
        System.out.println("Analysis Completed: Beginning Instrumentation Phase");
    }

    public static boolean isCollection(String internalName) {
        try {
            Class<?> c;
            if(TaintTrackingClassVisitor.IS_RUNTIME_INST && !internalName.startsWith("java/")) {
                return false;
            }
            c = Class.forName(internalName.replace("/", "."), false, loader);
            if(java.util.Collection.class.isAssignableFrom(c)) {
                return true;
            }
        } catch(Throwable ex) {
            //
        }
        return false;
    }

    public static boolean isClassWithHashMapTag(String clazz) {
        return clazz.startsWith("java/lang/Boolean")
                || clazz.startsWith("java/lang/Character")
                || clazz.startsWith("java/lang/Byte")
                || clazz.startsWith("java/lang/Short");
    }

    public static boolean isIgnoredClass(String owner) {
        return Configuration.taintTagFactory.isIgnoredClass(owner)
                || taintTagFactoryPackage != null && StringUtils.startsWith(owner, taintTagFactoryPackage)
                || controlFlowManagerPackage != null && StringUtils.startsWith(owner, controlFlowManagerPackage)
                || (Configuration.controlFlowManager != null && Configuration.controlFlowManager.isIgnoredClass(owner))
                || (Configuration.ADDL_IGNORE != null && StringUtils.startsWith(owner, Configuration.ADDL_IGNORE))
                //|| !owner.startsWith("edu/columbia/cs/psl")
                /*
                For these classes: HotSpot expects fields to be at hardcoded offsets of these classes. If we instrument
                them, it will break those assumptions and segfault.

                Different clasess have different assumptions, and there are special cases for these elsewhere in Phosphor.
                 */
                || StringUtils.startsWith(owner, "java/lang/Object")
                || StringUtils.startsWith(owner, "java/lang/Boolean")
                || StringUtils.startsWith(owner, "java/lang/Character")
                || StringUtils.startsWith(owner, "java/lang/Byte")
                || StringUtils.startsWith(owner, "java/lang/Short")
                || StringUtils.startsWith(owner, "java/lang/Number")
                || StringUtils.startsWith(owner, "java/lang/ref/Reference")
                || StringUtils.startsWith(owner, "java/lang/ref/SoftReference")
                || StringUtils.startsWith(owner, "java/lang/invoke/LambdaForm") //Lambdas are hosted by this class, and when generated, will have hard-coded offsets to constant pool
                /*
                Phosphor internal methods
                 */
                || StringUtils.startsWith(owner, "edu/columbia/cs/psl/phosphor")
                || StringUtils.startsWith(owner, "edu/gmu/swe/phosphor/ignored")
                /*
                Reflection is handled by:
                    DelagatingMethod/ConstructorAccessorImpl calls either NativeMethod/ConstructorAccessorImpl OR
                    calls GeneratedMethod/ConstructorAccessorImpl. We do the wrapping at the delagating level.
                    Generated code won't be instrumented. Hence, it's convenient to also not wrap the native version,
                    so that passed params/returns line up exactly when we hit the reflected call
                 */
                //|| StringUtils.startsWith(owner, "sun/reflect/NativeConstructorAccessorImpl")
                //|| StringUtils.startsWith(owner, "sun/reflect/NativeMethodAccessorImpl")

                || StringUtils.startsWith(owner, "edu/columbia/cs/psl/phosphor/struct/TaintedWith")
                //|| StringUtils.startsWith(owner, "java/util/regex/HashDecompositions") //Huge constant array/hashmap
                //|| StringUtils.startsWith(owner, "jdk/internal/module/SystemModules")
                || StringUtils.startsWith(owner, "jdk/internal/misc/UnsafeConstants"); //Java 9+ class full of hardcoded offsets
    }

    public static byte[] instrumentClass(String path, InputStream is, boolean renameInterfaces) {
        try {
            // n is shared among threads, but is used only to provide progress feedback
            // Therefore, it's ok to increment it in a non-thread-safe way
            n++;
            if(!Configuration.QUIET_MODE && n % 1000 == 0) {
                System.out.println("Processed: " + n);
            }
            curPath = path;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            for(int nRead; (nRead = is.read(data, 0, data.length)) != -1;) {
                buffer.write(data, 0, nRead);
            }
            is.close();
            buffer.flush();
            PreMain.PCLoggingTransformer transformer = new PreMain.PCLoggingTransformer();
            byte[] ret = transformer.transform(Instrumenter.loader, path, null, null, buffer.toByteArray());
            if(addlTransformer != null) {
                byte[] ret2 = addlTransformer.transform(Instrumenter.loader, path, null, null, ret);
                if(ret2 != null) {
                    ret = ret2;
                }
            }
            curPath = null;
            return ret;
        } catch(Exception ex) {
            curPath = null;
            ex.printStackTrace();
            //return null;
            throw new IllegalStateException();
        }
    }

    public static void main(String[] args) {
        long START = System.currentTimeMillis();
        CommandLine line = PhosphorOption.configure(false, args);
        if(line == null) {
            return;
        }
        Configuration.init();
        if(Configuration.DATAFLOW_TRACKING) {
            System.out.println("Data flow tracking: enabled");
        } else {
            System.out.println("Data flow tracking: disabled");
        }
        if(Configuration.IMPLICIT_TRACKING) {
            System.out.println("Control flow tracking: enabled");
        } else {
            System.out.println("Control flow tracking: disabled");
        }
        if(Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
            System.out.println("Branch not taken: disabled");
        } else {
            System.out.println("Branch not taken: enabled");
        }
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        _main(line.getArgs());
        System.out.println("Done after " + (System.currentTimeMillis() - START) + " ms");
    }

    public static void _main(String[] args) {
        if(PreMain.DEBUG) {
            System.err.println("Warning: Debug output enabled (uses a lot of IO!)");
        }

        String outputFolder = args[1];
        File rootOutputDir = new File(outputFolder);
        if(!rootOutputDir.exists()) {
            rootOutputDir.mkdir();
        }
        String inputFolder = args[0];

        // Setup the class loader
        final ArrayList<URL> urls = new ArrayList<>();
        Path input = FileSystems.getDefault().getPath(args[0]);
        try {
            if(Files.isDirectory(input)) {
                Files.walkFileTree(input, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if(file.getFileName().toString().endsWith(".jar")) {
                            urls.add(file.toUri().toURL());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else if(inputFolder.endsWith(".jar")) {
                urls.add(new File(inputFolder).toURI().toURL());
            }
        } catch(IOException e1) {
            e1.printStackTrace();
        }

        try {
            urls.add(new File(inputFolder).toURI().toURL());
        } catch(MalformedURLException e1) {
            e1.printStackTrace();
        }
        if(args.length == 3) {
            System.out.println("Using extra classpath file: " + args[2]);
            try {
                File f = new File(args[2]);
                if(f.exists() && f.isFile()) {
                    for(Scanner s = new Scanner(f); s.hasNextLine();) {
                        urls.add(new File(s.nextLine()).getCanonicalFile().toURI().toURL());
                    }
                } else if(f.isDirectory()) {
                    urls.add(f.toURI().toURL());
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else if(args.length > 3) {
            for(int i = 2; i < args.length; i++) {
                File f = new File(args[i]);
                if(!f.exists()) {
                    System.err.println("Unable to read path " + args[i]);
                    System.exit(-1);
                }
                if(f.isDirectory() && !f.getAbsolutePath().endsWith("/")) {
                    f = new File(f.getAbsolutePath() + "/");
                }
                try {
                    urls.add(f.getCanonicalFile().toURI().toURL());
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        URL[] urlArray = new URL[urls.size()];
        urlArray = urls.toArray(urlArray);
        loader = new URLClassLoader(urlArray, Instrumenter.class.getClassLoader());
        PreMain.bigLoader = loader;

        final File f = new File(inputFolder);
        if(!f.exists()) {
            System.err.println("Unable to read path " + inputFolder);
            System.exit(-1);
        }

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        LinkedList<Future> toWait = new LinkedList<>();

        if(f.isDirectory()) {
            toWait.addAll(processDirectory(f, rootOutputDir, true, executor));
        } else if(inputFolder.endsWith(".jar") || inputFolder.endsWith(".zip") || inputFolder.endsWith(".war")
                || inputFolder.endsWith(".jmod")) {
            toWait.addAll(processZip(f, rootOutputDir, executor));
        } else if(inputFolder.endsWith(".class")) {
            toWait.addAll(processClass(f, rootOutputDir, executor));
        } else {
            System.err.println("Unknown type for path " + inputFolder);
            System.exit(-1);
        }

        while(!toWait.isEmpty()) {
            try {
                toWait.addAll((Collection<? extends Future>) toWait.removeFirst().get());
            } catch(InterruptedException e) {
                //
            } catch(ExecutionException e) {
                throw new Error(e);
            }
        }

        executor.shutdown();
        while(!executor.isTerminated()) {
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                //
            }
        }
    }

    private static List<Future<? extends Collection>> processClass(File f, final File outputDir, ExecutorService executor) {
        List<Future<? extends Collection>> ret = new LinkedList<>();
        try {
            final String name = f.getName();
            final InputStream is = new FileInputStream(f);
            ret.add(executor.submit(new Callable<List>() {
                @Override
                public List call() throws Exception {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    FileOutputStream fos = new FileOutputStream(outputDir.getPath() + File.separator + name);
                    byte[] c = instrumentClass(outputDir.getAbsolutePath(), is, true);
                    is.close();
                    if(c != null) {
                        bos.write(c);
                    }
                    bos.writeTo(fos);
                    fos.close();
                    return new LinkedList();
                }
            }));

        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    private static LinkedList<Future> processDirectory(File f, File parentOutputDir, boolean isFirstLevel, ExecutorService executor) {
        LinkedList<Future> ret = new LinkedList<>();
        if(f.getName().equals(".AppleDouble")) {
            return ret;
        }
        final File thisOutputDir;
        if(isFirstLevel) {
            thisOutputDir = parentOutputDir;
        } else {
            thisOutputDir = new File(parentOutputDir.getAbsolutePath() + File.separator + f.getName());
            thisOutputDir.mkdir();
        }
        for(final File fi : f.listFiles()) {
            if(fi.isDirectory()) {
                ret.addAll(processDirectory(fi, thisOutputDir, false, executor));
            } else if(fi.getName().endsWith(".class")) {
                ret.addAll(processClass(fi, thisOutputDir, executor));
            } else if(fi.getName().endsWith(".jar") || fi.getName().endsWith(".zip") || fi.getName().endsWith(".war")
                    || fi.getName().endsWith(".jmod")) {
                ret.addAll(processZip(fi, thisOutputDir, executor));
            } else {
                File dest = new File(thisOutputDir.getPath() + File.separator + fi.getName());
                FileChannel source = null;
                FileChannel destination = null;

                try {
                    source = new FileInputStream(fi).getChannel();
                    destination = new FileOutputStream(dest).getChannel();
                    destination.transferFrom(source, 0, source.size());
                    if(fi.canExecute()) {
                        dest.setExecutable(true);
                    }
                    if(fi.canRead()) {
                        dest.setReadable(true);
                    }
                    if(fi.canWrite()) {
                        dest.setWritable(true);
                    }
                } catch(Exception ex) {
                    System.err.println("error copying file " + fi);
                    ex.printStackTrace();
                } finally {
                    if(source != null) {
                        try {
                            source.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(destination != null) {
                        try {
                            destination.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Handles Jar files, Zip files and War files.
     */
    public static LinkedList<Future> processZip(final File f, File outputDir, ExecutorService executor) {
        return _processZip(f, outputDir, executor, false);
    }

    private static LinkedList<Future> _processZip(final File f, File outputDir, ExecutorService executor, boolean unCompressed) {
        try(ZipFile zip = new ZipFile(f); ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()))) {
            if(unCompressed) {
                zos.setLevel(ZipOutputStream.STORED);
            }
            LinkedList<Future<Result>> ret = new LinkedList<>();
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while(entries.hasMoreElements()) {
                final ZipEntry e = entries.nextElement();

                if(e.getName().endsWith(".class")) {
                    ret.add(executor.submit(new Callable<Result>() {
                        @Override
                        public Result call() throws Exception {
                            Result ret = new Result();
                            ret.e = e;
                            ret.buf = instrumentClass(f.getAbsolutePath(), zip.getInputStream(e), true);
                            return ret;
                        }
                    }));
                } else if(e.getName().endsWith(".jar")) {
                    ZipEntry outEntry = new ZipEntry(e.getName());
                    Random r = new Random();
                    String markFileName = Long.toOctalString(System.currentTimeMillis())
                            + Integer.toOctalString(r.nextInt(10000))
                            + e.getName().replace("/", "");
                    final String tempDir = System.getProperty("java.io.tmpdir");
                    File tmp = new File(tempDir, markFileName);
                    if(tmp.exists()) {
                        tmp.delete();
                    }
                    try(InputStream is = zip.getInputStream(e); FileOutputStream fos = new FileOutputStream(tmp)) {
                        byte[] buf = new byte[1024];
                        for(int len; (len = is.read(buf)) > 0;) {
                            fos.write(buf, 0, len);
                        }
                    }

                    File tmp2 = new File(tempDir, "tmp2");
                    if(!tmp2.exists()) {
                        tmp2.mkdir();
                    }
                    _processZip(tmp, tmp2, executor, true);
                    tmp.delete();

                    outEntry.setMethod(ZipEntry.STORED);
                    Path newPath = Paths.get(tempDir, "tmp2", markFileName);

                    outEntry.setSize(Files.size(newPath));
                    CRC32 crc = new CRC32();
                    crc.update(Files.readAllBytes(newPath));
                    outEntry.setCrc(crc.getValue());
                    zos.putNextEntry(outEntry);
                    File newFile = newPath.toFile();
                    try(InputStream is = new FileInputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        for(int count = is.read(buffer); count != -1; count = is.read(buffer)) {
                            zos.write(buffer, 0, count);
                        }
                    }
                    newFile.delete();
                    zos.closeEntry();
                } else {
                    ZipEntry outEntry = new ZipEntry(e.getName());
                    if(e.isDirectory()) {
                        try {
                            zos.putNextEntry(outEntry);
                            zos.closeEntry();
                        } catch(ZipException exxxx) {
                            System.out.println("Ignoring exception: " + exxxx.getMessage());
                        }
                    } else if(!e.getName().startsWith("META-INF")
                            || (!e.getName().endsWith(".SF")
                            && !e.getName().endsWith(".RSA"))) {
                        if(e.getName().equals("META-INF/MANIFEST.MF")) {
                            Scanner s = new Scanner(zip.getInputStream(e));
                            zos.putNextEntry(outEntry);

                            String curPair = "";
                            while(s.hasNextLine()) {
                                String line = s.nextLine();
                                if(line.equals("")) {
                                    curPair += "\n";
                                    if(!curPair.contains("SHA1-Digest:")) {
                                        zos.write(curPair.getBytes());
                                    }
                                    curPair = "";
                                } else {
                                    curPair += line + "\n";
                                }
                            }
                            s.close();
                            // Jar file is different from Zip file. :)
                            if(f.getName().endsWith(".zip")) {
                                zos.write("\n".getBytes());
                            }
                            zos.closeEntry();
                        } else {
                            try {
                                zos.putNextEntry(outEntry);
                                try(InputStream is = zip.getInputStream(e)) {
                                    byte[] buffer = new byte[1024];
                                    for(int count = is.read(buffer); count != -1; count = is.read(buffer)) {
                                        zos.write(buffer, 0, count);
                                    }
                                }
                                zos.closeEntry();
                            } catch(ZipException ex) {
                                if(!ex.getMessage().contains("duplicate entry")) {
                                    ex.printStackTrace();
                                    System.out.println("Ignoring above warning from improper source zip...");
                                }
                            }
                        }
                    }
                }
            }
            for(Future<Result> fr : ret) {
                Result r;
                while(true) {
                    try {
                        r = fr.get();
                        break;
                    } catch(InterruptedException e) {
                        //
                    }
                }
                try {
                    ZipEntry outEntry = new ZipEntry(r.e.getName());
                    zos.putNextEntry(outEntry);

                    byte[] clazz = r.buf;
                    if(clazz == null) {
                        System.out.println("Failed to instrument " + r.e.getName() + " in " + f.getName());
                        try(InputStream is = zip.getInputStream(r.e)) {
                            byte[] buffer = new byte[1024];
                            for(int count = is.read(buffer); count != -1; count = is.read(buffer)) {
                                zos.write(buffer, 0, count);
                            }
                        }
                    } else {
                        zos.write(clazz);
                    }
                    zos.closeEntry();
                } catch(ZipException ex) {
                    ex.printStackTrace();
                }
            }
        } catch(Exception e) {
            System.err.println("Unable to process zip/jar: " + f.getAbsolutePath());
            e.printStackTrace();
            File dest = new File(outputDir.getPath() + File.separator + f.getName());
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(f).getChannel();
                destination = new FileOutputStream(dest).getChannel();
                destination.transferFrom(source, 0, source.size());
            } catch(Exception ex) {
                System.err.println("Unable to copy zip/jar: " + f.getAbsolutePath());
                ex.printStackTrace();
            } finally {
                if(source != null) {
                    try {
                        source.close();
                    } catch(IOException e2) {
                        e2.printStackTrace();
                    }
                }
                if(destination != null) {
                    try {
                        destination.close();
                    } catch(IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        return new LinkedList<>();
    }

    public static boolean isIgnoredMethod(String owner, String name, String desc) {
        return false; //TODO see below from old jdk14 version
        //if(name.equals("wait") && desc.equals("(J)V")) {
        //    return true;
        //}
        //if(name.equals("wait") && desc.equals("(JI)V")) {
        //    return true;
        //}
        //if (owner.equals("jdk/internal/reflect/Reflection") && name.equals("getCallerClass")) {
        //    return true;
        //}
        //if (owner.equals("java/lang/invoke/MethodHandle")
        //        && ((name.equals("invoke") || name.equals("invokeBasic") || name.startsWith("linkTo")))) {
        //    return true;
        //}
        //return owner.equals("java/lang/invoke/VarHandle"); //TODO wrap these all
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
        if(cn == null) {
            // Class was loaded before ClassSupertypeReadingTransformer was added
            return tryToAddClassNode(className);
        } else {
            return cn;
        }
    }

    /* Attempts to create a ClassNode populated with supertype information for this class. */
    private static ClassNode tryToAddClassNode(String className) {
        try(InputStream is = ClassLoader.getSystemResourceAsStream(className + ".class")) {
            if(is == null) {
                return null;
            }
            ClassReader cr = new ClassReader(is);
            cr.accept(new ClassVisitor(Configuration.ASM_VERSION) {
                private ClassNode cn;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                    cn = new ClassNode();
                    cn.name = name;
                    cn.superName = superName;
                    cn.interfaces = new java.util.ArrayList<>(java.util.Arrays.asList(interfaces));
                    cn.methods = new java.util.LinkedList<>();
                    classes.put(name, cn);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    cn.methods.add(new MethodNode(access, name, descriptor, signature, exceptions));
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }, ClassReader.SKIP_CODE);
            return classes.get(className);
        } catch(Exception e) {
            return null;
        }
    }

    private static class Result {
        ZipEntry e;
        byte[] buf;
    }

    public static byte[] transformJavaBaseModuleInfo(InputStream is, java.util.Collection<String> packages) throws IOException {
        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(is);
        java.util.List<Attribute> attrs = new java.util.ArrayList<>();
        attrs.add(new ModuleTargetAttribute());
        attrs.add(new ModuleResolutionAttribute());
        attrs.add(new ModuleHashesAttribute());

        cr.accept(classNode, attrs.toArray(new Attribute[0]), 0);
        //Add export
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor", 0, null));
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor/control", 0, null));
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor/control/standard", 0, null));
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor/runtime", 0, null));
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor/struct", 0, null));
        classNode.module.exports.add(new ModuleExportNode("edu/columbia/cs/psl/phosphor/struct/multid", 0, null));

        //Add pac
        classNode.module.packages.addAll(packages);
        ClassWriter cw = new ClassWriter(0);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    public static boolean isPhosphorClassPatchedAtInstTime(String name){
        return name.equals("edu/columbia/cs/psl/phosphor/Configuration.class") || name.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeJDKInternalUnsafePropagator.class");
    }

    /**
     * We do rewriting of various phosphor classes to ensure easy compilation for java < 9
     */
    public static byte[] patchPhosphorClass(String name, InputStream is) throws IOException {
        if(name.equals("edu/columbia/cs/psl/phosphor/Configuration.class")){
            return transformPhosphorConfigurationToUseJava9(is);
        }
        if(name.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeJDKInternalUnsafePropagator.class")){
            return transformRuntimeJDKUnsafePropagator(is);
        }
        throw new UnsupportedEncodingException("We do not plan to instrument " + name);
    }

    public static byte[] transformRuntimeJDKUnsafePropagator(InputStream is) throws IOException {
        final String UNSAFE_PROXY_INTERNAL_NAME = Type.getInternalName(UnsafeProxy.class);
        final String UNSAFE_PROXY_DESC = Type.getDescriptor(UnsafeProxy.class);
        final String TARGET_UNSAFE_INTERNAL_NAME = "jdk/internal/misc/Unsafe";
        final String TARGET_UNSAFE_DESC = "Ljdk/internal/misc/Unsafe;";
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String patchInternalName(String in) {
                if (in.equals(UNSAFE_PROXY_INTERNAL_NAME)) {
                    return TARGET_UNSAFE_INTERNAL_NAME;
                }
                return in;
            }

            private String patchDesc(String in) {
                if (in == null) {
                    return null;
                }
                return in.replace(UNSAFE_PROXY_DESC, TARGET_UNSAFE_DESC);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, patchDesc(descriptor), patchDesc(signature), exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {

                    @Override
                    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
                        for (int i = 0; i < numLocal; i++) {
                            if (local[i] instanceof String) {
                                local[i] = patchInternalName((String) local[i]);
                            }
                        }
                        for (int i = 0; i < numStack; i++) {
                            if (stack[i] instanceof String) {
                                stack[i] = patchInternalName((String) stack[i]);
                            }
                        }
                        super.visitFrame(type, numLocal, local, numStack, stack);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        super.visitFieldInsn(opcode, patchInternalName(owner), name, patchDesc(descriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, patchInternalName(owner), name, patchDesc(descriptor), isInterface);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        super.visitTypeInsn(opcode, patchInternalName(type));
                    }

                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                        super.visitLocalVariable(name, patchDesc(descriptor), signature, start, end, index);
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();

    }
    /**
     * To boot the JVM... we need to have this flag set correctly.
     *
     * Default to setting it to be Java 8.
     *
     * In Java 9+, we pack Phosphor into the java.base module, and rewrite the configuration file
     * to set the flag to false.
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] transformPhosphorConfigurationToUseJava9(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if(name.equals("<clinit>")){
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if(opcode == Opcodes.PUTSTATIC && name.equals("IS_JAVA_8")){
                                super.visitInsn(Opcodes.POP);
                                super.visitInsn(Opcodes.ICONST_0);
                            }
                            super.visitFieldInsn(opcode, owner, name, descriptor);
                        }
                    };
                } else {
                    return mv;
                }

            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();

    }


    public static boolean isJava8JVMDir(File java_home) {
        return new File(java_home, "bin" + File.separator + "java").exists()
                && !new File(java_home, "jmods").exists()
                && !new File(java_home, "lib" + File.separator + "modules").exists();
    }

    public static boolean isUnsafeClass(String className){
        return (Configuration.IS_JAVA_8 && "sun/misc/Unsafe".equals(className))
                || (!Configuration.IS_JAVA_8 && "jdk/internal/misc/Unsafe".equals(className));
    }

}
