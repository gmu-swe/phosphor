package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/* Prevents Jetty's buffer utility class from creating direct buffers. */
public class JettyBufferUtilCV extends ClassVisitor implements Opcodes {

    public JettyBufferUtilCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new JettyBufferUtilMV(mv);
    }

    /* Returns whether this class visitor should be applied to the class with the specified name. */
    public static boolean isApplicable(String className) {
        return className != null && className.equals("org/eclipse/jetty/util/BufferUtil");
    }

    private static class JettyBufferUtilMV extends MethodVisitor {

        JettyBufferUtilMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if(opcode == INVOKESTATIC && owner.equals("java/nio/ByteBuffer") && name.equals("allocateDirect") && desc.equals("(I)Ljava/nio/ByteBuffer;")) {
                super.visitMethodInsn(opcode, owner, "allocate", desc, itf);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
