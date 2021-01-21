package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.HackyClassWriter;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurJSRInlinerAdapter;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class LegacyClassFixer {

    private static ClassReader approximateFrames(ClassReader cr) {
        // Compute approximate frames
        ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        // Remove JSR instructions and inline subroutines
        cr.accept(new JsrInliningClassVisitor(cw), 0);
        return (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(cw.toByteArray()) :
                new ClassReader(cw.toByteArray()));
    }

    private static ClassReader correctFrames(ClassReader cr) {
        ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        // Add CHECKCASTs to accommodate for incorrect frames
        cr.accept(new FrameCorrectingClassVisitor(cw), ClassReader.EXPAND_FRAMES);
        return (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(cw.toByteArray()) :
                new ClassReader(cw.toByteArray()));
    }

    public static ClassReader fix(ClassReader cr) {
        return correctFrames(approximateFrames(cr));
    }

    public static boolean shouldFixFrames(ClassNode cn, String className, ClassReader cr) {
        if (cn.version >= 100 || cn.version <= 50 || className.endsWith("$Access4JacksonSerializer")
                || className.endsWith("$Access4JacksonDeSerializer")) {
            return true;
        } else if (Configuration.ALWAYS_CHECK_FOR_FRAMES) {
            cn = new ClassNode();
            cr.accept(cn, 0);
            for (MethodNode mn : cn.methods) {
                if (hasFrames(mn)) {
                    return false;
                } else if (hasJumps(mn)) {
                    // The method has at least one jump but no frames
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasJumps(MethodNode mn) {
        if (!mn.tryCatchBlocks.isEmpty()) {
            return true;
        }
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof JumpInsnNode
                    || insn instanceof TableSwitchInsnNode
                    || insn instanceof LookupSwitchInsnNode) {
                return true;
            }
            insn = insn.getNext();
        }
        return false;
    }

    private static boolean hasFrames(MethodNode mn) {
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof FrameNode) {
                return true;
            }
            insn = insn.getNext();
        }
        return false;
    }

    private static final class JsrInliningClassVisitor extends ClassVisitor {

        private JsrInliningClassVisitor(ClassVisitor classVisitor) {
            super(Configuration.ASM_VERSION, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            return new OurJSRInlinerAdapter(super.visitMethod(access, name, descriptor, signature, exceptions),
                    access, name, descriptor, signature, exceptions);
        }
    }

    private static final class FrameCorrectingClassVisitor extends ClassVisitor {
        /**
         * The name of the class being visited.
         */
        private String className;

        private FrameCorrectingClassVisitor(ClassVisitor classVisitor) {
            super(Configuration.ASM_VERSION, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            FrameCorrectingMethodVisitor correctingMv = new FrameCorrectingMethodVisitor(mv);
            AnalyzerAdapter analyzerAdapter = new AnalyzerAdapter(className, access, name, descriptor, correctingMv);
            correctingMv.setAnalyzerAdapter(analyzerAdapter);
            LocalVariablesSorter localVariablesSorter = new LocalVariablesSorter(access, descriptor,
                    analyzerAdapter);
            correctingMv.setLocalVariablesSorter(localVariablesSorter);
            return localVariablesSorter;
        }
    }

    private static final class FrameCorrectingMethodVisitor extends MethodVisitor {

        private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
        private AnalyzerAdapter analyzerAdapter;
        private LocalVariablesSorter localVariablesSorter;

        private FrameCorrectingMethodVisitor(MethodVisitor methodVisitor) {
            super(Configuration.ASM_VERSION, methodVisitor);
        }

        private void setAnalyzerAdapter(AnalyzerAdapter analyzerAdapter) {
            this.analyzerAdapter = analyzerAdapter;
        }

        private void setLocalVariablesSorter(LocalVariablesSorter localVariablesSorter) {
            this.localVariablesSorter = localVariablesSorter;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            List<Object> stack = analyzerAdapter.stack;
            if (stack == null) {
                // Unreachable instruction
                return;
            }
            Type[] argTypes = Type.getArgumentTypes(descriptor);
            Type[] expectedTypes;
            if (opcode == INVOKESTATIC || opcode == INVOKESPECIAL) {
                // Ignore the receiver for invokespecial
                // [arg1, arg2, ...]
                expectedTypes = argTypes;
            } else {
                // objectref, [arg1, arg2, ...]
                expectedTypes = new Type[1 + argTypes.length];
                System.arraycopy(argTypes, 0, expectedTypes, 1, argTypes.length);
                expectedTypes[0] = Type.getObjectType(owner);
            }
            reverse(expectedTypes);
            int slotOffset = 0;
            int numElements = 0;
            boolean[] shouldCast = new boolean[expectedTypes.length];
            for (int i = 0; i < expectedTypes.length; i++) {
                Type expected = expectedTypes[i];
                Object slot = stack.get(stack.size() - 1 - slotOffset++);
                if (slot == Opcodes.TOP && slotOffset < stack.size()) {
                    slot = stack.get(stack.size() - 1 - slotOffset++);
                }
                if (slot instanceof String) {
                    Type actual = Type.getObjectType(((String) slot));
                    if (!actual.equals(expected) && !expected.equals(OBJECT_TYPE)) {
                        numElements = i + 1;
                        shouldCast[i] = true;
                    }
                }
            }
            if (numElements > 0) {
                castStack(numElements, expectedTypes, shouldCast);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        private void castStack(Type expected) {
            super.visitTypeInsn(CHECKCAST, expected.getInternalName());
        }

        private void castStack(Type expected1, boolean shouldCast1, Type expected2) {
            if (shouldCast1) {
                super.visitTypeInsn(CHECKCAST, expected1.getInternalName());
            }
            if (expected1.getSize() == 1) {
                super.visitInsn(Opcodes.SWAP);
                super.visitTypeInsn(CHECKCAST, expected2.getInternalName());
                super.visitInsn(Opcodes.SWAP);
            } else {
                // objectref J TOP
                super.visitInsn(Opcodes.DUP2_X1);
                // J TOP objectref J TOP
                super.visitInsn(Opcodes.POP2);
                // J TOP objectref
                super.visitTypeInsn(CHECKCAST, expected2.getInternalName());
                super.visitInsn(Opcodes.DUP_X2);
                // objectref J TOP objectref
                super.visitInsn(Opcodes.POP);
            }
        }

        private void castStack(int numElements, Type[] expectedTypes, boolean[] shouldCast) {
            if (numElements == 1) {
                castStack(expectedTypes[0]);
            } else if (numElements == 2) {
                castStack(expectedTypes[0], shouldCast[0], expectedTypes[1]);
            } else {
                int[] indices = new int[numElements];
                // Store elements to local variables
                for (int i = 0; i < numElements; i++) {
                    Type type = expectedTypes[i];
                    if (expectedTypes[i].getSort() == Type.ARRAY || expectedTypes[i].getSort() == Type.OBJECT) {
                        type = OBJECT_TYPE;
                    }
                    indices[i] = localVariablesSorter.newLocal(type);
                    super.visitVarInsn(type.getOpcode(Opcodes.ISTORE), indices[i]);
                }
                // Load elements from local variables onto stack then cast if needed
                for (int i = numElements - 1; i >= 0; i--) {
                    Type expected = expectedTypes[i];
                    super.visitVarInsn(expected.getOpcode(Opcodes.ILOAD), indices[i]);
                    if (shouldCast[i]) {
                        super.visitTypeInsn(CHECKCAST, expected.getInternalName());
                    }
                }
            }
        }

        private static <T> void reverse(T[] array) {
            for (int i = 0; i < array.length / 2; i++) {
                T temp = array[i];
                array[i] = array[array.length - 1 - i];
                array[array.length - 1 - i] = temp;
            }
        }
    }
}