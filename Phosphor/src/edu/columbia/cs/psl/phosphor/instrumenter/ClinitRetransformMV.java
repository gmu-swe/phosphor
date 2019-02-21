package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.SourceSinkTransformer;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/* Visits the <clint> method to add code to retransform the class on class initialization. */
public class ClinitRetransformMV extends MethodVisitor {

    // The name of the class that owns the method being visited
    private final String className;
    // Whether or not the version is at least the required version 1.5 for the ldc of a constant class
    private final boolean fixLdcClass;

    public ClinitRetransformMV(MethodVisitor mv, String className, boolean fixLdcClass) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.fixLdcClass = fixLdcClass;
    }

    @Override
    public void visitInsn(int opcode) {
        if(TaintUtils.isReturnOpcode(opcode)) {
            if(fixLdcClass) {
                // Since the class is not at least the required version 1.5 for the ldc of a constant class, push the class
                // onto the stack by making a call to Class.forName
                super.visitLdcInsn(className.replace("/", "."));
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            }
            else {
                // Directly push the class onto the stack
                mv.visitLdcInsn(Type.getObjectType(className));
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(SourceSinkTransformer.class), "retransform", "(Ljava/lang/Class;)V", false);
        }
        super.visitInsn(opcode);
    }

}
