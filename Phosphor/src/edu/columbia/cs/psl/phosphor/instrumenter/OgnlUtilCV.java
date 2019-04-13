package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/* Disables Ognl expression caching by changing reads of the enableExpressionCache field to return false. */
public class OgnlUtilCV extends ClassVisitor {

    // The name of field whose reads are being changed
    private static final String targetFieldName = "enableExpressionCache";

    public OgnlUtilCV(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new OgnlUtilMV(mv);
    }

    private static class OgnlUtilMV extends MethodVisitor {

        OgnlUtilMV(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if(isOgnlUtilClass(owner) && name.equals(targetFieldName)) {
                if(opcode == Opcodes.GETFIELD) {
                    super.visitInsn(Opcodes.POP);
                    super.visitInsn(Opcodes.ICONST_0);
                } else if(opcode == Opcodes.GETSTATIC) {
                    super.visitInsn(Opcodes.ICONST_0);
                }
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }

    }

    /* Returns whether the class with the specified name is OgnlUtil. */
    public static boolean isOgnlUtilClass(String className) {
        return className != null && className.equals("com/opensymphony/xwork2/ognl/OgnlUtil");
    }
}
