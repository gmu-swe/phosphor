package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TaintThroughTaintingMV extends MethodVisitor implements Opcodes {

    private final String owner;
    private final String desc;
    private final boolean isStatic;

    public TaintThroughTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv);
        this.owner = owner;
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        taintArguments();
    }

    /* Adds code to add this instance's taint tags to the arguments passed to this method. */
    private void taintArguments() {
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for(int i = 0; i < args.length; i++) {
            if((!args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC) && args[i].getSort() == Type.OBJECT) ||
                    (args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT)) {
                // Argument is an object or array of objects
                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                super.visitVarInsn(ALOAD, idx); // Load the argument onto the stack
                // super.visitVarInsn(ALOAD, 0); // Load this onto the stack for the call to GETFIELD
                // super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
                super.visitVarInsn(ALOAD, 1); //Load our reference taint onto the stack
                //
                //                hodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "(" + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "addTaint", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
            }
            idx += args[i].getSize();
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(OpcodesUtil.isReturnOpcode(opcode)) {
            taintArguments();
        }
        if(opcode == ARETURN) {

            // Wrapped primitive return type. The stack before this code runs contains wrapped primitive return type
            super.visitInsn(DUP); //for the PUTFIELD
            super.visitInsn(DUP); //for the combineTags
            super.visitFieldInsn(GETFIELD, Type.getInternalName(TaintedPrimitiveWithObjTag.class), "taint", Configuration.TAINT_TAG_DESC); //for combined tags
            // super.visitVarInsn(ALOAD, 0);
            // super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
            super.visitVarInsn(ALOAD, 1); //Load our reference taint onto the stack

            super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
            super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithObjTag.class), "taint", Configuration.TAINT_TAG_DESC);
        }
        super.visitInsn(opcode);
    }
}
