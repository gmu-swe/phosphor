package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class AsmPatchingCV extends ClassVisitor {
    private static final String ASM_PREFIX = "edu/columbia/cs/psl/phosphor/org/objectweb/asm/";
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    public AsmPatchingCV(ClassVisitor classVisitor) {
        super(Configuration.ASM_VERSION, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new AsmPatchingMV(mv);
    }

    public static boolean isApplicable(String className) {
        return className.startsWith(ASM_PREFIX);
    }

    private static class AsmPatchingMV extends MethodVisitor {
        public AsmPatchingMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            // Ensure that the return value is unwrapped if necessary
            if (owner.startsWith("java/") && OBJECT_TYPE.equals(Type.getReturnType(descriptor))) {
                TaintMethodRecord.TAINTED_REFERENCE_ARRAY_UNWRAP.delegateVisit(mv);
            }
        }
    }
}