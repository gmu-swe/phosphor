package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/* Visits a Java class modifying the <clint> method to add code to retransform the class on class initialization. */
public class ClinitRetransformClassVisitor extends ClassVisitor {

    // Whether or not the <clint> has been visited
    private boolean visitedClassInitializer;
    // The name of the class being visited
    private String className;
    // Whether or not the version is at least the required version 1.5 for the ldc of a constant class
    private boolean fixLdcClass;

    public ClinitRetransformClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
        this.visitedClassInitializer = false;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(name.equals("<clinit>")) {
            visitedClassInitializer = true;
            if(!className.contains("$$Lambda$")) { // Do not add retransform code to  lambdas
                mv = new ClinitRetransformMV(mv, className, fixLdcClass);
            }
        }
        return mv;
    }

    /* Checks if the <clinit> method was visited. If it was not visited, makes calls to visit it. */
    @Override
    public void visitEnd() {
        if(!visitedClassInitializer) {
            MethodVisitor mv = visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}
