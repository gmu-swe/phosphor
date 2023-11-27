package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import org.objectweb.asm.*;

public class AsmPatcher extends ClassVisitor {
    private static final String ASM_PREFIX = "edu/columbia/cs/psl/phosphor/org/objectweb/asm/";
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    public AsmPatcher(ClassWriter cw) {
        super(Configuration.ASM_VERSION, cw);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, mv) {

            @Override
            public void visitMethodInsn(
                    int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                // Ensure that the return value is unwrapped if necessary
                if (owner.startsWith("java/") && OBJECT_TYPE.equals(Type.getReturnType(descriptor))) {
                    TaintMethodRecord.TAINTED_REFERENCE_ARRAY_UNWRAP.delegateVisit(mv);
                }
            }
        };
    }

    public static byte[] patch(byte[] classFileBuffer) {
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new AsmPatcher(cw);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static boolean isApplicable(String className) {
        return className.startsWith(ASM_PREFIX);
    }
}