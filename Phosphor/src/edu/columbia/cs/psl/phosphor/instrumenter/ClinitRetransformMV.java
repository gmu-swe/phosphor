package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.PreMain;

public class ClinitRetransformMV extends MethodVisitor {

    private final String className;

    public ClinitRetransformMV(MethodVisitor mv, String className) {
        super(Opcodes.ASM5, mv);
        this.className = className;
    }

    @Override
    public void visitInsn(int opcode) {
        if(isReturn(opcode)) {
            mv.visitLdcInsn(className);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PreMain.class), "retransform", "(Ljava/lang/String;)V", false);
        }
        super.visitInsn(opcode);
    }

    /* Returns whether or not the specified opcode is for a return */
    public static boolean isReturn(int opcode) {
        switch(opcode) {
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.RETURN:
            case Opcodes.DRETURN:
            case Opcodes.FRETURN:
            case Opcodes.LRETURN:
                return true;
            default:
                return false;
        }
    }


}
