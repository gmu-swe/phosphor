package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;

import edu.columbia.cs.psl.struct.PhosphorHttpRequest;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/* Builds a request object from the bytes that reach the server side of a tomcat socket in order to taint them. */
public class TomcatPostSocketCV extends ClassVisitor implements Opcodes{

    public TomcatPostSocketCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(name.equals("fill")) {
            // Visiting the fill method for the internal buffer class
            mv = new BufferFillMV(mv);
        }
        return mv;
    }

    private static class BufferFillMV extends MethodVisitor {

        BufferFillMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if(TaintUtils.isReturnOpcode(opcode)) {
                // Structure the bytes in the buf array into an HttpUriRequest so that source method from that class are applied
                // and then convert back to a byte array
                // Load a length one array onto the stack to get an updated value for lastValid
                super.visitInsn(ICONST_1);
                super.visitIntInsn(NEWARRAY, T_INT);
                super.visitInsn(DUP); // Insert a copy of the array onto the stack
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack for GETFIELD
                super.visitFieldInsn(GETFIELD, "org/apache/coyote/http11/InternalNioInputBuffer", "buf", Type.getDescriptor(byte[].class));
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack for GETFIELD
                super.visitFieldInsn(GETFIELD, "org/apache/coyote/http11/InternalNioInputBuffer", "pos", "I");
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack for GETFIELD
                super.visitFieldInsn(GETFIELD, "org/apache/coyote/http11/InternalNioInputBuffer", "lastValid", "I");
                // Invoke structureIntoRequest
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(PhosphorHttpRequest.class), "structureIntoRequest", "([I[BII)[B", false);
                // Update buf with the result of structureIntoRequest
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack for PUTFIELD buf
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, "org/apache/coyote/http11/InternalNioInputBuffer", "buf", Type.getDescriptor(byte[].class));
                // Update lastValid
                super.visitInsn(ICONST_0); // Load the updated value for lastValid from the int array passed to structureIntoRequest
                super.visitInsn(IALOAD);
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack for PUTFIELD lastValid
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, "org/apache/coyote/http11/InternalNioInputBuffer", "lastValid", "I");
            }
            super.visitInsn(opcode);
        }
    }

    /* Returns whether the class with the specified name is the targeted internal buffer class. */
    public static boolean isInternalBufferClass(String className) {
        return className != null && className.equals("org/apache/coyote/http11/InternalNioInputBuffer");
    }
}