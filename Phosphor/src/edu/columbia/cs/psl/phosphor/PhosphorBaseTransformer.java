package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/* Provides appropriate phosphor tagged versions of transform. */
public abstract class PhosphorBaseTransformer implements ClassFileTransformer {

    public static boolean INITED = false;

    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, LazyByteArrayObjTags classtaint,
                                                          byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!INITED) {
            Configuration.IMPLICIT_TRACKING = false;
            Configuration.MULTI_TAINTING = true;
            Configuration.init();
            INITED = true;
        }
        LazyByteArrayObjTags ret = null;
        if (className != null && className.startsWith("sun")) //there are dynamically generated accessors for reflection, we don't want to instrument those.
            ret = new LazyByteArrayObjTags(classfileBuffer);
        else
            ret = new LazyByteArrayObjTags(transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer));

        return ret;
    }

    public LazyByteArrayObjTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, LazyByteArrayObjTags classtaint,
                                                          byte[] classfileBuffer, ControlTaintTagStack ctrl) throws IllegalClassFormatException {
        if (!INITED) {
            Configuration.IMPLICIT_TRACKING = true;
            Configuration.MULTI_TAINTING = true;
            Configuration.init();
            INITED = true;
        }
        LazyByteArrayObjTags ret = null;

        if (className != null && className.startsWith("sun")) //there are dynamically generated accessors for reflection, we don't want to instrument those.
            ret = new LazyByteArrayObjTags(classfileBuffer);
        else
            ret = new LazyByteArrayObjTags(transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer));


        return ret;
    }

    public LazyByteArrayIntTags transform$$PHOSPHORTAGGED(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, LazyByteArrayIntTags classtaint,
                                                          byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!INITED) {
            Configuration.IMPLICIT_TRACKING = false;
            Configuration.MULTI_TAINTING = false;
            Configuration.init();
            INITED = true;
        }
        LazyByteArrayIntTags ret;
        if (className != null && className.startsWith("sun")) //there are dynamically generated accessors for reflection, we don't want to instrument those.
            ret = new LazyByteArrayIntTags(classfileBuffer);
        else
            ret = new LazyByteArrayIntTags(transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer));
        return ret;
    }

}
