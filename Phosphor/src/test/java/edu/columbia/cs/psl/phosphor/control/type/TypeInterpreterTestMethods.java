package edu.columbia.cs.psl.phosphor.control.type;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class TypeInterpreterTestMethods {

    public static MethodNode mergeStringAndNull() {
        //public static void mergeStringAndNull(boolean b) {
        //    String s = "Hello";
        //    if(b) {
        //        s = null;
        //    }
        //    String s2 = s;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC + ACC_STATIC, "mergeStringAndNull",
                "(Z)V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("Hello");
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ILOAD, 0);
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitLabel(l0);
        mv.visitFrame(F_NEW, 2, new Object[]{INTEGER, "java/lang/String"}, 0, new Object[0]);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 3);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode mergeNullAndString() {
        //public static void mergeNullAndString(boolean b) {
        //    String s = null;
        //    if(b) {
        //        s = "Hello";
        //    }
        //    String s2 = s;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC + ACC_STATIC, "mergeNullAndString", "(Z)V",
                null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ILOAD, 0);
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);
        mv.visitLdcInsn("Hello");
        mv.visitVarInsn(ASTORE, 1);
        mv.visitLabel(l0);
        mv.visitFrame(F_NEW, 2, new Object[]{INTEGER, "java/lang/String"}, 0, new Object[0]);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 3);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode mergeNullAndNull() {
        //public static void mergeNullAndNull(boolean b) {
        //    String s2 = null;
        //    String s = s2;
        //    if(b) {
        //        s = null;
        //    }
        //    s2 = s;
        //}
        MethodNode mv = new MethodNode(ACC_PUBLIC + ACC_STATIC, "mergeNullAndNull",
                "(Z)V", null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ILOAD, 0);
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitLabel(l0);
        mv.visitFrame(F_NEW, 3, new Object[]{INTEGER, "java/lang/String", "java/lang/String"}, 0, new Object[0]);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 3);
        mv.visitEnd();
        return mv;
    }

    public static MethodNode impl_getCellBounds() {
        MethodNode mv = new MethodNode(ACC_PUBLIC + ACC_FINAL + ACC_DEPRECATED, "impl_getCellBounds",
                "(II)Ljavafx/geometry/Bounds;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getHgap", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getVgap", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 5);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getInsets",
                "()Ljavafx/geometry/Insets;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Insets", "getTop", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 7);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getInsets",
                "()Ljavafx/geometry/Insets;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Insets", "getRight", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 9);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getInsets",
                "()Ljavafx/geometry/Insets;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Insets", "getBottom", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 11);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getInsets",
                "()Ljavafx/geometry/Insets;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Insets", "getLeft", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSpace", "(D)D", false);
        mv.visitVarInsn(DSTORE, 13);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getHeight", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSize", "(D)D", false);
        mv.visitVarInsn(DLOAD, 7);
        mv.visitVarInsn(DLOAD, 11);
        mv.visitInsn(DADD);
        mv.visitInsn(DSUB);
        mv.visitVarInsn(DSTORE, 15);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getWidth", "()D", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "snapSize", "(D)D", false);
        mv.visitVarInsn(DLOAD, 13);
        mv.visitVarInsn(DLOAD, 9);
        mv.visitInsn(DADD);
        mv.visitInsn(DSUB);
        mv.visitVarInsn(DSTORE, 17);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getGrid", "()[[D", false);
        mv.visitVarInsn(ASTORE, 21);
        mv.visitVarInsn(ALOAD, 21);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_DOUBLE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DASTORE);
        mv.visitVarInsn(ASTORE, 20);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_DOUBLE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DASTORE);
        mv.visitVarInsn(ASTORE, 19);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 1);
        Label l1 = new Label();
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l0);
        mv.visitFrame(F_NEW, 14, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, TOP, TOP, "[[D"}, 0, new Object[0]);
        mv.visitVarInsn(ALOAD, 21);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 19);
        mv.visitVarInsn(ALOAD, 21);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 20);
        mv.visitLabel(l1);
        mv.visitFrame(F_NEW, 14, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D"}, 0, new Object[0]);
        mv.visitInsn(DCONST_0);
        mv.visitVarInsn(DSTORE, 22);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 24);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitFrame(F_NEW, 16, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                        DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, INTEGER},
                0, new Object[0]);
        mv.visitVarInsn(ILOAD, 24);
        mv.visitVarInsn(ALOAD, 20);
        mv.visitInsn(ARRAYLENGTH);
        Label l3 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l3);
        mv.visitVarInsn(DLOAD, 22);
        mv.visitVarInsn(ALOAD, 20);
        mv.visitVarInsn(ILOAD, 24);
        mv.visitInsn(DALOAD);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 22);
        mv.visitIincInsn(24, 1);
        mv.visitJumpInsn(GOTO, l2);
        mv.visitLabel(l3);
        mv.visitFrame(F_NEW, 15, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                        DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE},
                0, new Object[0]);
        mv.visitVarInsn(DLOAD, 22);
        mv.visitVarInsn(ALOAD, 20);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitInsn(I2D);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(DMUL);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 22);
        mv.visitVarInsn(DLOAD, 7);
        mv.visitVarInsn(DLOAD, 15);
        mv.visitVarInsn(DLOAD, 22);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getAlignment",
                "()Ljavafx/geometry/Pos;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Pos", "getVpos",
                "()Ljavafx/geometry/VPos;", false);
        mv.visitMethodInsn(INVOKESTATIC, "javafx/scene/layout/Region", "computeYOffset",
                "(DDLjavafx/geometry/VPos;)D", false);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 24);
        mv.visitVarInsn(ALOAD, 20);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(DALOAD);
        mv.visitVarInsn(DSTORE, 26);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 28);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitFrame(F_NEW, 18, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE, DOUBLE,
                INTEGER}, 0, new Object[0]);
        mv.visitVarInsn(ILOAD, 28);
        mv.visitVarInsn(ILOAD, 2);
        Label l5 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l5);
        mv.visitVarInsn(DLOAD, 24);
        mv.visitVarInsn(ALOAD, 20);
        mv.visitVarInsn(ILOAD, 28);
        mv.visitInsn(DALOAD);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(DADD);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 24);
        mv.visitIincInsn(28, 1);
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l5);
        mv.visitFrame(F_NEW, 17, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                        DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE, DOUBLE},
                0, new Object[0]);
        mv.visitInsn(DCONST_0);
        mv.visitVarInsn(DSTORE, 28);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 30);
        Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitFrame(F_NEW, 19, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE,
                DOUBLE, DOUBLE, INTEGER}, 0, new Object[0]);
        mv.visitVarInsn(ILOAD, 30);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitInsn(ARRAYLENGTH);
        Label l7 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l7);
        mv.visitVarInsn(DLOAD, 28);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitVarInsn(ILOAD, 30);
        mv.visitInsn(DALOAD);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 28);
        mv.visitIincInsn(30, 1);
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l7);
        mv.visitFrame(F_NEW, 18, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE,
                DOUBLE, DOUBLE}, 0, new Object[0]);
        mv.visitVarInsn(DLOAD, 28);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitInsn(I2D);
        mv.visitVarInsn(DLOAD, 3);
        mv.visitInsn(DMUL);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 28);
        mv.visitVarInsn(DLOAD, 13);
        mv.visitVarInsn(DLOAD, 17);
        mv.visitVarInsn(DLOAD, 28);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/scene/layout/GridPane", "getAlignment",
                "()Ljavafx/geometry/Pos;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javafx/geometry/Pos", "getHpos",
                "()Ljavafx/geometry/HPos;", false);
        mv.visitMethodInsn(INVOKESTATIC, "javafx/scene/layout/Region", "computeXOffset",
                "(DDLjavafx/geometry/HPos;)D", false);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 30);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(DALOAD);
        mv.visitVarInsn(DSTORE, 32);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 34);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitFrame(F_NEW, 21, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, INTEGER}, 0, new Object[0]);
        mv.visitVarInsn(ILOAD, 34);
        mv.visitVarInsn(ILOAD, 1);
        Label l9 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l9);
        mv.visitVarInsn(DLOAD, 30);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitVarInsn(ILOAD, 34);
        mv.visitInsn(DALOAD);
        mv.visitVarInsn(DLOAD, 3);
        mv.visitInsn(DADD);
        mv.visitInsn(DADD);
        mv.visitVarInsn(DSTORE, 30);
        mv.visitIincInsn(34, 1);
        mv.visitJumpInsn(GOTO, l8);
        mv.visitLabel(l9);
        mv.visitFrame(F_NEW, 20, new Object[]{"javafx/scene/layout/GridPane", INTEGER, INTEGER, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, "[D", "[D", "[[D", DOUBLE, DOUBLE,
                DOUBLE, DOUBLE, DOUBLE, DOUBLE}, 0, new Object[0]);
        mv.visitTypeInsn(NEW, "javafx/geometry/BoundingBox");
        mv.visitInsn(DUP);
        mv.visitVarInsn(DLOAD, 30);
        mv.visitVarInsn(DLOAD, 24);
        mv.visitVarInsn(DLOAD, 32);
        mv.visitVarInsn(DLOAD, 26);
        mv.visitMethodInsn(INVOKESPECIAL, "javafx/geometry/BoundingBox", "<init>", "(DDDD)V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(10, 35);
        mv.visitEnd();
        return mv;
    }
}
