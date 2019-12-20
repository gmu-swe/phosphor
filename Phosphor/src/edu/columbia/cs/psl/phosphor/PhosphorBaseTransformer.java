package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.StringUtils;
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
    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                                          ProtectionDomain protectionDomain, LazyByteArrayObjTags classTaint) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = false;
            Configuration.init();
            INITED = true;
        }
        return LazyByteArrayObjTags.factory(signalAndTransform(loader, className, classBeingRedefined, protectionDomain, classTaint.val));
    }

    @SuppressWarnings("unused")
    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                                          ProtectionDomain protectionDomain, LazyByteArrayObjTags classTaint,
                                                          ControlTaintTagStack ctrl) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = true;
            Configuration.init();
            INITED = true;
        }
        return LazyByteArrayObjTags.factory(signalAndTransform(loader, className, classBeingRedefined, protectionDomain, classTaint.val));
    }

    private byte[] signalAndTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                      byte[] classFileBuffer) throws IllegalClassFormatException {
        if(className != null && StringUtils.startsWith(className, "sun") && !StringUtils.startsWith(className, "sun/nio")) {
            // Avoid instrumenting dynamically generated accessors for reflection
            return classFileBuffer;
        }
        try {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming++;
            }
            return transform(loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
        } finally {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming--;
            }
        }
    }
}
