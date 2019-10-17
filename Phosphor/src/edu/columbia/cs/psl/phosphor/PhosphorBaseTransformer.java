package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/* Provides appropriate phosphor tagged versions of transform. */
public abstract class PhosphorBaseTransformer implements ClassFileTransformer {

    public static boolean INITED = false;
    protected static int isBusyTransforming = 0;

    @SuppressWarnings("unused")
    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, LazyByteArrayObjTags classtaint,
                                                          byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = false;
            Configuration.MULTI_TAINTING = true;
            Configuration.init();
            INITED = true;
        }
        return new LazyByteArrayObjTags(signalAndTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer));
    }

    @SuppressWarnings("unused")
    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, LazyByteArrayObjTags classtaint,
                                                          byte[] classfileBuffer, ControlTaintTagStack ctrl) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = true;
            Configuration.MULTI_TAINTING = true;
            Configuration.init();
            INITED = true;
        }
        return new LazyByteArrayObjTags(signalAndTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer));
    }

    private byte[] signalAndTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(className != null && className.startsWith("sun") && !className.startsWith("sun/nio")) {
            // Avoid instrumenting dynamically generated accessors for reflection
            return classfileBuffer;
        }
        try {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming++;
            }
            return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        } finally {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming--;
            }
        }
    }
}
