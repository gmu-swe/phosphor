package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

public class ClinitRetransformClassVisitor extends ClassVisitor {

    private boolean visitedClassInitializer;
    public static final String CLINIT_NAME = "<clinit>";
    private String className;
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
        if(name.equals(CLINIT_NAME)) {
            visitedClassInitializer = true;
            mv = new ClinitRetransformMV(mv, className, fixLdcClass);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if(!visitedClassInitializer) {
            MethodVisitor mv = visitMethod(Opcodes.ACC_STATIC, CLINIT_NAME, "()V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}
