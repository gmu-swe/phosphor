package edu.columbia.cs.psl.phosphor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.objectweb.asm.Opcodes.*;

public class InstrumenterTest {

    @Before
    public void clearErrorFlag() {
        PreMain.INSTRUMENTATION_EXCEPTION_OCCURRED = false;
    }

    @After
    public void checkForError() {
        if (PreMain.INSTRUMENTATION_EXCEPTION_OCCURRED) {
            Assert.fail("Instrumentation error occurred");
        }
    }

    @Test
    public void testAggressivelyReduceMethodSizeGetStaticThenArrayLength() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "Test", null, "java/lang/Object",
                 null);
        MethodVisitor mv;
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "example", "()V", null, null);
        mv.visitCode();
        for (int i = 0; i < 20000; i++) {
            mv.visitInsn(ICONST_0);
            mv.visitInsn(POP);
        }
        mv.visitFieldInsn(GETSTATIC, "Test", "arr", "[I");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();
        InputStream in = new ByteArrayInputStream(cw.toByteArray());
        Instrumenter.instrumentClass("Test", in, true);
    }

    @Test
    public void wideDuplicates() {
        Configuration.IMPLICIT_TRACKING = false;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, "Example", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "e", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "Example", "l1", "J");
        mv.visitVarInsn(LSTORE, 0);
        mv.visitInsn(ICONST_1);
        Label label0 = new Label();
        mv.visitJumpInsn(IFEQ, label0);
        mv.visitFieldInsn(GETSTATIC, "Example", "l2", "J");
        mv.visitVarInsn(LSTORE, 0);
        mv.visitLabel(label0);
        mv.visitLdcInsn(5L);
        mv.visitVarInsn(LLOAD, 0);
        // {v2 v2} {v1 v1}
        mv.visitInsn(DUP2_X2);
        // {v1 v1} {v2 v2} {v1 v1}
        mv.visitInsn(POP2);
        // {v1 v1} {v2 v2}
        mv.visitInsn(DUP2_X2);
        // {v2 v2} {v1 v1} {v2 v2}
        mv.visitInsn(DUP2_X2);
        // {v2 v2} {v2 v2} {v1 v1} {v2 v2}
        mv.visitInsn(POP2);
        // {v2 v2} {v2 v2} {v1 v1}
        mv.visitInsn(DUP2_X2);
        // {v2 v2} {v1 v1} {v2 v2} {v1 v1}
        mv.visitInsn(POP2);
        // {v2 v2} {v1 v1} {v2 v2}
        mv.visitInsn(POP2);
        // {v2 v2} {v1 v1}
        mv.visitInsn(LCMP);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
        InputStream in = new ByteArrayInputStream(cw.toByteArray());
        Instrumenter.instrumentClass("Test", in, true);
    }
}