package edu.columbia.cs.psl.phosphor;

import com.sun.org.apache.bcel.internal.generic.Type;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

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
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "Test", null, "java/lang/Object", null);
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

    @Test
    public void lambdaWithWrapperTypeToPreAllocate() {
        Configuration.IMPLICIT_TRACKING = false;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_SUPER, "Example$$Lambda$1", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
        cw.visitEnd();
        InputStream in = new ByteArrayInputStream(cw.toByteArray());
        Instrumenter.instrumentClass("Test", in, true);
    }

    @Test
    public void directLambdaArgumentsCorrect() {
        Configuration.IMPLICIT_TRACKING = false;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_SUPER, "Example$$Lambda$1", null, "java/lang/Object", null);
        MethodVisitor mv =
                cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", null,
                               null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
        cw.visitEnd();
        InputStream in = new ByteArrayInputStream(cw.toByteArray());
        byte[] result = Instrumenter.instrumentClass("Test", in, true);
        Assert.assertNotNull(result);
        ClassReader cr = new ClassReader(result);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        String expectedName = "apply" + TaintUtils.METHOD_SUFFIX;
        String expectedDesc = Type.getMethodSignature(Type.getType(TaintedReferenceWithObjTag.class),
                                                      new Type[]{Type.getType(Taint.class), Type.getType(Object.class),
                                                              Type.getType(Taint.class), Type.getType(Object.class),
                                                              Type.getType(Taint.class),
                                                              Type.getType(TaintedReferenceWithObjTag.class),
                                                              Type.getType(Object.class)});
        cn.methods.stream()
                  .filter(m -> m.name.equals(expectedName) && m.desc.equals(expectedDesc))
                  .findFirst()
                  .orElseThrow(AssertionError::new);
    }
}