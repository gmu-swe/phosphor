package edu.columbia.cs.psl.phosphor;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/* Provides appropriate phosphor tagged versions of transform. */
public abstract class PhosphorBaseTransformer {

    public static boolean INITED = false;
    protected static int isBusyTransforming = 0;

    public abstract byte[] transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain, byte[] classfileBuffer);

    public byte[] signalAndTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                     byte[] classFileBuffer) throws IllegalClassFormatException {
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
