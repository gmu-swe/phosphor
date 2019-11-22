package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.SourceSinkTaintingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

/* Transforms classes modifying the code for sink, source, and taintThrough methods. */
public class SourceSinkTransformer extends PhosphorBaseTransformer {

    // Stores classes for which retransform was called before Configuration.init() was called or PreMain's instrumentation
    // was set.
    private static LinkedList<Class<?>> retransformQueue = new LinkedList<>();
    // Whether classes are in the process of being retransformed or checked for retransformation
    private static boolean isBusyRetransforming = false;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(classBeingRedefined == null) {
            // The transform was triggered by a class load not a redefine or retransform then no transformations
            // should be performed
            return null;
        } else if(Throwable.class.isAssignableFrom(classBeingRedefined)) {
            return null;
        }
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = cw;
            if(PreMain.DEBUG || TaintUtils.VERIFY_CLASS_GENERATION) {
                cv = new CheckClassAdapter(cw, false);
            }
            cr.accept(new SourceSinkTaintingClassVisitor(cv), ClassReader.EXPAND_FRAMES);
            if(PreMain.DEBUG) {
                try {
                    File f = new File("debug-source-sink/" + className + ".class");
                    f.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(cw.toByteArray());
                    fos.close();
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            return cw.toByteArray();
        } catch(Throwable t) {
            //If we don't try/catch and print the error, then it will be silently swallowed by the JVM
            //and we'll never know the instrumentation failed :(
            t.printStackTrace();
            throw t;
        }
    }

    /* Retransforms the specified class modifying the code for sink, source, and taintThrough methods. Called by <clinit>. Stores
     * classes until the VM is initialized at which point all stored classes are retransformed. */
    public static void retransform(Class<?> clazz) {
        synchronized(BasicSourceSinkManager.class) {
            synchronized(SourceSinkTransformer.class) {
                try {
                    int isBusyTransforming;
                    synchronized(PhosphorBaseTransformer.class) {
                        isBusyTransforming = PhosphorBaseTransformer.isBusyTransforming;
                    }
                    // Check if PreMain's instrumentation has been set by a call to premain and that Configuration.init() has
                    // been called to initialize the configuration
                    if(isBusyTransforming == 0 && !isBusyRetransforming && INITED && PreMain.getInstrumentation() != null) {
                        isBusyRetransforming = true;
                        retransformQueue.addFast(clazz);
                        // Retransform clazz and any classes that were initialized before retransformation could occur.
                        while(!retransformQueue.isEmpty()) {
                            Class<?> poppedClazz = retransformQueue.pop();
                            BasicSourceSinkManager.recordClass(poppedClazz);
                            if(poppedClazz.getName() != null && BasicSourceSinkManager.getInstance().isSourceOrSinkOrTaintThrough(poppedClazz)) {
                                // poppedClazz represents a class or interface that is or is a subtype of a class or interface with
                                // at least one method labeled as being a sink or source or taintThrough method
                                if(!poppedClazz.equals(PrintStream.class)) {
                                    PreMain.getInstrumentation().retransformClasses(poppedClazz);
                                }
                            }
                        }
                        isBusyRetransforming = false;
                    } else {
                        retransformQueue.addFast(clazz);
                    }
                } catch(UnmodifiableClassException e) {
                    //
                } catch(Throwable e) {
                    // for anything else, we probably want to make sure that it gets printed
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }
}
