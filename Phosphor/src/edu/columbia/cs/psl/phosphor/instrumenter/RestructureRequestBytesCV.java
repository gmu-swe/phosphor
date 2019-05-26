package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;

import edu.columbia.cs.psl.struct.PhosphorHttpRequest;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.FieldNode;

import java.nio.ByteBuffer;

/* Builds a request object from the bytes that reach the server side of a socket in order to taint them. */
public class RestructureRequestBytesCV extends ClassVisitor implements Opcodes {

    // The name of the class that reads request bytes from the socket in Tomcat 8
    public static final String TOMCAT_8_BUFFER_CLASS = "org/apache/coyote/http11/InternalNioInputBuffer";
    // The name of the class that reads request bytes from the socket in Tomcat 9
    public static final String TOMCAT_9_BUFFER_CLASS = "org/apache/coyote/http11/Http11InputBuffer";
    // The name of the method for the buffer class in Tomcat 8 and 9 where the request bytes are read
    private static final String TOMCAT_READ_METHOD = "fill";
    // The name of the method for the buffer class in Tomcat 8 and 9 where the socket is added
    private static final String TOMCAT_INIT_METHOD = "init";
    // The name of the ByteBuffer field added to store the restructured bytes
    public static final String BYTE_BUFF_FIELD_NAME = "$$PHOSPHOR_BUF";

    // Node for field added to store the restructured bytes
    private final FieldNode bufFieldNode;
    // The name of the class being visited
    private String className;

    public RestructureRequestBytesCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
        bufFieldNode = new FieldNode(ACC_PUBLIC, BYTE_BUFF_FIELD_NAME, "Ljava/nio/ByteBuffer;", null, null);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(TOMCAT_INIT_METHOD.equals(name)) {
            // Visiting the targeted method
            mv = new RestructureRequestByteMV(mv, className, bufFieldNode);
        } else if(TOMCAT_READ_METHOD.equals(name)) {
            mv = new SocketReadMV(mv, className, bufFieldNode);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        bufFieldNode.accept(cv);
        super.visitEnd();
    }


    private static class RestructureRequestByteMV extends MethodVisitor {

        // The class that owns the method being visited
        private final String owner;
        // Node for field added to store the restructured bytes
        private final FieldNode bufFieldNode;

        RestructureRequestByteMV(MethodVisitor mv, String owner, FieldNode bufFieldNode) {
            super(Configuration.ASM_VERSION, mv);
            this.owner = owner;
            this.bufFieldNode = bufFieldNode;
        }

        @Override
        public void visitInsn(int opcode) {
            if(TaintUtils.isReturnOpcode(opcode)) {
                super.visitVarInsn(ALOAD, 0); // Load this onto the stack
                super.visitInsn(DUP);
                // Call structureRequestBytes
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(PhosphorHttpRequest.class), "structureRequestBytes", "(Ljava/lang/Object;)"+bufFieldNode.desc, false);
                // Put the result of structure request bytes into the added buffer field
                super.visitFieldInsn(PUTFIELD, owner, bufFieldNode.name, bufFieldNode.desc);
            }
            super.visitInsn(opcode);
        }
    }

    private static class SocketReadMV extends MethodVisitor {

        // The class that owns the method being visited
        private final String owner;
        // Node for field added to store the restructured bytes
        private final FieldNode bufFieldNode;

        SocketReadMV(MethodVisitor mv, String owner, FieldNode bufFieldNode) {
            super(Configuration.ASM_VERSION, mv);
            this.owner = owner;
            this.bufFieldNode = bufFieldNode;
        }

        /* Adds code to read bytes from the added byte buffer field into a byte buffer argument. */
        private void readFromAddedBuffer(int opcode, Type[] args) {
            // Pop the arguments pushed for the original read method call off the stack except the byte buffer argument
            boolean found = false;
            for(int i = args.length-1; i >= 0; i--) {
                if(!found && args[i].getClassName().equals(ByteBuffer.class.getName())) {
                    found = true;
                } else {
                    if(found) {
                        super.visitInsn(SWAP);
                    }
                    if(args[i].getSort() == Type.LONG || args[i].getSort() == Type.DOUBLE) {
                        super.visitInsn(POP2);
                    } else {
                        super.visitInsn(POP);
                    }
                }
            }
            if(opcode == INVOKEVIRTUAL) {
                // Pop the receiver object for the original read call off of the stack
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }
            // The destination byte buffer should be on the top of the stack
            super.visitVarInsn(ALOAD, 0); // Load this onto the stack
            // Call read
            String readDesc = String.format("(%sLjava/lang/Object;)I", bufFieldNode.desc);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(PhosphorHttpRequest.class), "read", readDesc, false);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if(name.equals("read")) {
                readFromAddedBuffer(opcode, Type.getArgumentTypes(desc));
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

    /* Returns whether the class with the specified name is one targeted by this class visitor */
    public static boolean isApplicable(String className) {
        return TOMCAT_8_BUFFER_CLASS.equals(className) || TOMCAT_9_BUFFER_CLASS.equals(className);
    }
}