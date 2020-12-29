package edu.columbia.cs.psl.phosphor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
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
}