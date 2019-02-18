package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.SourceSinkTaintingMV;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;


public class SourceSinkRetransformer implements ClassFileTransformer {

    public static class SourceSinkRetransformerCV extends ClassVisitor {
        private String className;
        public SourceSinkRetransformerCV(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ((access & Opcodes.ACC_NATIVE) == 0) {
                mv = new SourceSinkTaintingMV(mv, access, className, name, desc, desc);
            }
            return mv;
        }
    }
    static boolean INITED = false;

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

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new SourceSinkRetransformerCV(cw), 0);
        return cw.toByteArray();
    }

    public static void retransform(Class<?> clazz) {
        try{
        	if(INITED && PreMain.getInstrumentation() != null)
                PreMain.getInstrumentation().retransformClasses(clazz);
        } catch (UnmodifiableClassException e) {
            e.printStackTrace();
        }
    }
}

