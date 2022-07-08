package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.StringUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/* Provides appropriate phosphor tagged versions of transform. */
public abstract class PhosphorBaseTransformer implements ClassFileTransformer {

    public static boolean INITED = false;
    protected static int isBusyTransforming = 0;

    public abstract byte[] _transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) ;

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                                                ProtectionDomain protectionDomain,
                                                                byte[] clazz) throws IllegalClassFormatException {
        try {
            if (!INITED) {
                Configuration.IMPLICIT_TRACKING = false;
                Configuration.init();
                INITED = true;
            }
            return signalAndTransform(loader, className, classBeingRedefined, protectionDomain, clazz);
        } finally{
        }
    }

    private byte[] signalAndTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                      byte[] classFileBuffer) throws IllegalClassFormatException {
        //if(className != null && StringUtils.startsWith(className, "sun") && !StringUtils.startsWith(className, "sun/nio")) {
            // Avoid instrumenting dynamically generated accessors for reflection
            //return classFileBuffer;
        //}
        try {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming++;
            }
            return _transform(loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
        } finally {
            synchronized(PhosphorBaseTransformer.class) {
                isBusyTransforming--;
            }
        }
    }
}
