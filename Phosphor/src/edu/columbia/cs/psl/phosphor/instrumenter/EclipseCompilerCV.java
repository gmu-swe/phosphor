package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/* Changes the error handling policies of an instance of the Compiler class used by the Eclipse Compiler for Java, ECJ,
 * to ignore all problems in the hopes that these issue will be resolved when Phosphor instruments the class compiled
 * class. */
public class EclipseCompilerCV extends ClassVisitor {

    public EclipseCompilerCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(name.equals("<init>")) {
            // Visiting an instance initialization method for ECJ's compiler class
            mv = new ECJInitMV(mv, desc);
        }
        return mv;
    }

    /* Returns whether the class with the specified name is ECJ's compiler class. */
    public static boolean isEclipseCompilerClass(String className) {
        return className != null && className.equals("org/eclipse/jdt/internal/compiler/Compiler");
    }

    private static class ECJInitMV extends MethodVisitor {
        private final Type[] args;

        ECJInitMV(MethodVisitor mv, String desc) {
            super(Configuration.ASM_VERSION, mv);
            this.args = Type.getArgumentTypes(desc);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            int idx = 1; // Start at 1 to skip over the "this" argument
            for(Type arg : args) {
                if(arg.getInternalName().equals("org/eclipse/jdt/internal/compiler/IErrorHandlingPolicy")) {
                    // Make the replacement policy
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/eclipse/jdt/internal/compiler/DefaultErrorHandlingPolicies", "ignoreAllProblems", "()Lorg/eclipse/jdt/internal/compiler/IErrorHandlingPolicy;", false);
                    // Store the replacement policy into the old policy's local variable
                    super.visitVarInsn(Opcodes.ASTORE, idx);
                }
                idx += arg.getSize();
            }
        }
    }
}
