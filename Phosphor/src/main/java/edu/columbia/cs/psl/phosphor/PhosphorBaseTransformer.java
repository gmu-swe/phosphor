package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.StringUtils;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/* Provides appropriate phosphor tagged versions of transform. */
public abstract class PhosphorBaseTransformer implements ClassFileTransformer {

    public static boolean INITED = false;
    protected static int isBusyTransforming = 0;

    @SuppressWarnings("unused")
    public TaintedReferenceWithObjTag transform$$PHOSPHORTAGGED(Taint refTaint, ClassLoader loader, Taint loaderTaint, String className, Taint classNameTaint, Class<?> classBeingRedefined,
                                                                Taint classBeingRedefinedTaint, ProtectionDomain protectionDomain,
                                                                Taint protectionDomainTaint, LazyByteArrayObjTags clazz,
                                                                Taint clazzTaint,
                                                                TaintedReferenceWithObjTag ret) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = false;
            Configuration.init();
            INITED = true;
        }
        ret.taint = Taint.emptyTaint();
        ret.val = LazyByteArrayObjTags.factory(null, signalAndTransform(loader, className, classBeingRedefined, protectionDomain, clazz.val));
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedReferenceWithObjTag transform$$PHOSPHORTAGGED(Taint refTaint, ClassLoader loader, Taint loaderTaint, String className, Taint classNameTaint, Class<?> classBeingRedefined,
                                                                Taint classBeingRedefinedTaint, ProtectionDomain protectionDomain,
                                                                Taint protectionDomainTaint, LazyByteArrayObjTags clazz,
                                                                Taint clazzTaint,
                                                                ControlFlowStack ctrl,
                                                                TaintedReferenceWithObjTag ret) throws IllegalClassFormatException {
        if(!INITED) {
            Configuration.IMPLICIT_TRACKING = true;
            Configuration.init();
            INITED = true;
        }
        ret.taint = Taint.emptyTaint();
        ret.val = LazyByteArrayObjTags.factory(null, signalAndTransform(loader, className, classBeingRedefined, protectionDomain, clazz.val));
        return ret;
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
