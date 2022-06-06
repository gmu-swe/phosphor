package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.test.phopshor.Example;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class MethodArgReindexerTest {
    @Test
    public void parametersAnnotationsCorrect() throws IOException {
        Configuration.IMPLICIT_TRACKING = false;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        MethodNode mn = instrument(getMethod(getClassNode(Example.class), "parameters"));
        Assert.assertNotNull(mn.invisibleParameterAnnotations);
        Assert.assertNotNull(mn.visibleParameterAnnotations);
        Type[] parameters = Type.getArgumentTypes(mn.desc);
        for (int i = 0; i < parameters.length; i++) {
            List<AnnotationNode> invisibleAnnotations = mn.invisibleParameterAnnotations[i];
            List<AnnotationNode> visibleAnnotations = mn.visibleParameterAnnotations[i];
            Type parameter = parameters[i];
            switch (parameter.getSort()) {
                case Type.LONG:
                case Type.SHORT:
                    Assert.assertEquals(1, visibleAnnotations.size());
                    Assert.assertTrue(invisibleAnnotations == null || invisibleAnnotations.isEmpty());
                    break;
                case Type.OBJECT:
                    if (parameter.equals(org.objectweb.asm.Type.getType(Object.class))) {
                        Assert.assertEquals(1, invisibleAnnotations.size());
                        Assert.assertTrue(visibleAnnotations == null || visibleAnnotations.isEmpty());
                        break;
                    }
                default:
                    Assert.assertTrue(invisibleAnnotations == null || invisibleAnnotations.isEmpty());
                    Assert.assertTrue(visibleAnnotations == null || visibleAnnotations.isEmpty());
            }
        }
    }

    @Test
    public void longToTopFrame() {
        Configuration.IMPLICIT_TRACKING = false;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        MethodNode mn = instrument(longToTopNode());
        List<FrameNode> frames = Arrays.stream(mn.instructions.toArray())
                                       .filter(i -> i instanceof FrameNode)
                                       .map(i -> (FrameNode) i)
                                       .collect(Collectors.toList());
        Assert.assertEquals(1, frames.size());
        FrameNode frame = frames.get(0);
        Assert.assertEquals(Opcodes.F_NEW, frame.type);
        Assert.assertTrue(frame.stack.isEmpty());
        Assert.assertEquals(5, frame.local.size());
        Assert.assertEquals(TOP, frame.local.get(0));
        Assert.assertEquals(TOP, frame.local.get(1));
        Assert.assertTrue(
                frame.local.get(2) == TOP || frame.local.get(2).equals(Configuration.TAINT_TAG_INTERNAL_NAME));
        Assert.assertEquals(Type.getInternalName(TaintedIntWithObjTag.class), frame.local.get(3));
        Assert.assertEquals(INTEGER, frame.local.get(4));
    }

    @Test
    public void longToTopFrameControl() {
        Configuration.IMPLICIT_TRACKING = true;
        Configuration.IMPLICIT_HEADERS_NO_TRACKING = false;
        MethodNode mn = instrument(longToTopNode());
        List<FrameNode> frames = Arrays.stream(mn.instructions.toArray())
                                       .filter(i -> i instanceof FrameNode)
                                       .map(i -> (FrameNode) i)
                                       .collect(Collectors.toList());
        Assert.assertEquals(1, frames.size());
        FrameNode frame = frames.get(0);
        Assert.assertEquals(Opcodes.F_NEW, frame.type);
        Assert.assertTrue(frame.stack.isEmpty());
        Assert.assertEquals(6, frame.local.size());
        Assert.assertEquals(TOP, frame.local.get(0));
        Assert.assertEquals(TOP, frame.local.get(1));
        Assert.assertTrue(
                frame.local.get(2) == TOP || frame.local.get(2).equals(Configuration.TAINT_TAG_INTERNAL_NAME));
        Assert.assertEquals(TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME, frame.local.get(3));
        Assert.assertEquals(Type.getInternalName(TaintedIntWithObjTag.class), frame.local.get(4));
        Assert.assertEquals(INTEGER, frame.local.get(5));
    }

    private static MethodNode instrument(MethodNode mn) {
        boolean isLambda = mn.name.contains("$$Lambda$");
        boolean isStatic = (Opcodes.ACC_STATIC & mn.access) != 0;
        String taintedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(!isStatic, mn.desc);
        MethodNode wrapper = new MethodNode(Configuration.ASM_VERSION, mn.access, mn.name + TaintUtils.METHOD_SUFFIX,
                                            taintedDesc, mn.signature,
                                            mn.exceptions.toArray(new String[0]));
        MethodNode instrumented =
                new MethodNode(Configuration.ASM_VERSION, mn.access, mn.name + TaintUtils.METHOD_SUFFIX,
                               taintedDesc, mn.signature,
                               mn.exceptions.toArray(new String[0]));
        MethodArgReindexer mv =
                new MethodArgReindexer(instrumented, mn.access, mn.name, taintedDesc, mn.desc, wrapper, isLambda);

        mn.accept(mv);
        return instrumented;
    }

    static MethodNode getMethod(ClassNode cn, String name) {
        return cn.methods.stream()
                         .filter(m -> m.name.equals(name))
                         .findFirst()
                         .orElseThrow(AssertionError::new);
    }

    static ClassNode getClassNode(Class<?> clazz) throws IOException {
        ClassNode cn = new ClassNode();
        new ClassReader(clazz.getName()).accept(cn, ClassReader.EXPAND_FRAMES);
        return expandFramesAndComputeMaxStack(cn);
    }

    static ClassNode expandFramesAndComputeMaxStack(ClassNode cn) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        ClassReader cr = new ClassReader(cw.toByteArray());
        ClassNode result = new ClassNode();
        cr.accept(result, ClassReader.EXPAND_FRAMES);
        return result;
    }

    static MethodNode longToTopNode() {
        MethodNode mn = new MethodNode(ACC_PUBLIC + ACC_STATIC, "example", "(J)I", null, null);
        mn.visitCode();
        Label l0 = new Label();
        mn.visitInsn(ICONST_3);
        mn.visitVarInsn(ISTORE, 2);
        mn.visitJumpInsn(GOTO, l0);
        mn.visitLabel(l0);
        mn.visitFrame(F_NEW, 3, new Object[]{TOP, TOP, INTEGER}, 0, new Object[0]);
        mn.visitVarInsn(ILOAD, 2);
        mn.visitInsn(IRETURN);
        mn.visitMaxs(2, 3);
        mn.visitEnd();
        return mn;
    }
}