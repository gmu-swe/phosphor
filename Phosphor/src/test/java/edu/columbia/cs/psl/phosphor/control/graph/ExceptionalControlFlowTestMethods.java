package edu.columbia.cs.psl.phosphor.control.graph;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class ExceptionalControlFlowTestMethods {

    public static final String OWNER = "edu/columbia/cs/psl/phosphor/control/graph/ExceptionalControlFlowTestMethods";

    public static MethodNode callsExceptionThrowingMethod() {
        //public void callsExceptionThrowingMethod() {
        //    blockID = 0;
        //    try {
        //        exceptionThrowingMethod();
        //        blockID = 1;
        //    } catch(FileNotFoundException e) {
        //        blockID = 2;
        //    } catch(IOException e) {
        //        blockID = 3;
        //    }
        //    blockID = 4;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC, "callsExceptionThrowingMethod", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/io/FileNotFoundException");
        Label l3 = new Label();
        mv.visitTryCatchBlock(l0, l1, l3, "java/io/IOException");
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, OWNER, "exceptionThrowingMethod", "()V", false);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l1);
        Label l4 = new Label();
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 1, new Object[]{OWNER}, 1, new Object[]{"java/io/FileNotFoundException"});
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l3);
        mv.visitFrame(F_NEW, 1, new Object[]{OWNER}, 1, new Object[]{"java/io/IOException"});
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(ICONST_3);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l4);
        mv.visitFrame(F_NEW, 1, new Object[]{OWNER}, 0, null);
        mv.visitInsn(ICONST_4);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode implicitNPE() {
        //public int implicitNPE(int[] a) {
        //    blockID = 0;
        //    int i = 0;
        //    try {
        //        a[0] = 5;
        //        blockID = 1;
        //        i = 7;
        //    } catch(NullPointerException e) {
        //        blockID = 2;
        //        e.printStackTrace();
        //    }
        //    blockID = 3;
        //    return i;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC, "implicitNPE", "([I)I", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/NullPointerException");
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_5);
        mv.visitInsn(IASTORE);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitIntInsn(BIPUSH, 7);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitLabel(l1);
        Label l3 = new Label();
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 3, new Object[]{OWNER, "[I", INTEGER}, 1, new Object[]{"java/lang/NullPointerException"});
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NullPointerException", "printStackTrace", "()V", false);
        mv.visitLabel(l3);
        mv.visitFrame(F_NEW, 3, new Object[]{OWNER, "[I", INTEGER}, 1, new Object[]{"java/lang/NullPointerException"});
        mv.visitInsn(ICONST_3);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode nestedHandlers() {
        //public void nestedHandlers(Object obj) {
        //    blockID = 0;
        //    try {
        //        try {
        //            int i = 0;
        //            String s = obj.toString();
        //            blockID = 1;
        //        } catch(NullPointerException e) {
        //            blockID = 2;
        //            e.printStackTrace();
        //        }
        //        blockID = 3;
        //    } catch(RuntimeException e) {
        //        blockID = 4;
        //    }
        //    blockID = 5;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC, "nestedHandlers", "(Ljava/lang/Object;)V",
                null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/NullPointerException");
        Label l3 = new Label();
        Label l4 = new Label();
        mv.visitTryCatchBlock(l0, l3, l4, "java/lang/RuntimeException");
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l1);
        Label l5 = new Label();
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "java/lang/Object", INTEGER, "java/lang/String"}, 1,
                new Object[]{"java/lang/NullPointerException"});
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NullPointerException", "printStackTrace", "()V", false);
        mv.visitLabel(l5);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "java/lang/Object", INTEGER, "java/lang/String"}, 0,
                new Object[0]);
        mv.visitInsn(ICONST_3);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l3);
        Label l6 = new Label();
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l4);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "java/lang/Object", INTEGER, "java/lang/String"}, 1,
                new Object[]{"java/lang/RuntimeException"});
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(ICONST_4);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l6);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "java/lang/Object", INTEGER, "java/lang/String"}, 0,
                new Object[0]);
        mv.visitInsn(ICONST_5);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 4);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode explicitlyThrowCaughtException() {
        //public int explicitlyThrowCaughtException(int[] a) {
        //    blockID = 0;
        //    int i = 0;
        //    try {
        //        if(a[0] < 2) {
        //            blockID = 1;
        //            throw new IOException();
        //        }
        //        blockID = 2;
        //    } catch(IOException e) {
        //        blockID = 3;
        //        e.printStackTrace();
        //    }
        //    blockID = 4;
        //    return i;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC, "explicitlyThrowCaughtException", "([I)I", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/io/IOException");
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IALOAD);
        mv.visitInsn(ICONST_2);
        Label l3 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l3);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitTypeInsn(NEW, "java/io/IOException");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/io/IOException", "<init>", "()V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitFrame(F_NEW, 3, new Object[]{OWNER, "[I", INTEGER}, 0, null);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l1);
        Label l4 = new Label();
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 3, new Object[]{OWNER, "[I", INTEGER}, 1, new Object[]{"java/io/IOException"});
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(ICONST_3);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/IOException", "printStackTrace", "()V", false);
        mv.visitLabel(l4);
        mv.visitFrame(F_NEW, 3, new Object[]{OWNER, "[I", INTEGER}, 0, null);
        mv.visitInsn(ICONST_4);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode explicitlyThrowUncaughtException() {
        //public int explicitlyThrowUncaughtException(int[] a, IOException e) throws java.io.IOException {
        //    blockID = 0;
        //    int i = 0;
        //    try {
        //        if(a[0] < 2) {
        //            blockID = 1;
        //            throw e;
        //        }
        //        blockID = 2;
        //    } catch(IllegalArgumentException e2) {
        //        blockID = 3;
        //    }
        //    blockID = 4;
        //    return i;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC, "explicitlyThrowUncaughtException",
                "([ILjava/io/IOException;)I", null, new String[]{"java/io/IOException"});
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/IllegalArgumentException");
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IALOAD);
        mv.visitInsn(ICONST_2);
        Label l3 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l3);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "[I", "java/io/IOException", INTEGER}, 0, null);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l1);
        Label l4 = new Label();
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "[I", "java/io/IOException", INTEGER},
                1, new Object[]{"java/lang/IllegalArgumentException"});
        mv.visitVarInsn(ASTORE, 4);
        mv.visitInsn(ICONST_3);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitLabel(l4);
        mv.visitFrame(F_NEW, 4, new Object[]{OWNER, "[I", "java/io/IOException", INTEGER}, 0, null);
        mv.visitInsn(ICONST_4);
        mv.visitFieldInsn(PUTSTATIC, OWNER, "blockID", "I");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 5);
        mv.visitEnd();
        return mv;
    }
}
