package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PowerMockUtilCV extends ClassVisitor {
    public PowerMockUtilCV(ClassVisitor classVisitor) {
        super(Configuration.ASM_VERSION, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if(name.equals("<init>")) {
            mv = new MethodVisitor(Configuration.ASM_VERSION, mv) {
                @Override
                public void visitInsn(int opcode) {
                    if(opcode == Opcodes.RETURN) {
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitInsn(Opcodes.ICONST_1);
                        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                        super.visitInsn(Opcodes.DUP);
                        super.visitInsn(Opcodes.ICONST_0);
                        super.visitLdcInsn("edu.columbia.cs.psl.phosphor.*");
                        super.visitInsn(Opcodes.AASTORE);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/powermock/core/classloader/DeferSupportingClassLoader", "addIgnorePackage", "([Ljava/lang/String;)V", false);

                    }
                    super.visitInsn(opcode);
                }
            };
        }
        return mv;
    }

    /* Returns whether this class visitor should be applied to the class with the specified name. */
    public static boolean isApplicable(String className) {
        return className != null && className.equals("org/powermock/core/classloader/DeferSupportingClassLoader");
    }
}
