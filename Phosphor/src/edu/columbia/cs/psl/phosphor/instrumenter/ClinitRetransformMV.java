package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkRetransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.PreMain;

public class ClinitRetransformMV extends MethodVisitor {

    private final String className;

    private final boolean fixLdcClass;

    public ClinitRetransformMV(MethodVisitor mv, String className, boolean fixLdcClass) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.fixLdcClass = fixLdcClass;
    }

    @Override
    public void visitInsn(int opcode) {
        if(isReturn(opcode)) {
            if(fixLdcClass)
            {
                super.visitLdcInsn(className.replace("/", "."));
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            }
            else {
                mv.visitLdcInsn(Type.getObjectType(className));
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(SourceSinkRetransformer.class), "retransform", "(Ljava/lang/Class;)V", false);
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
