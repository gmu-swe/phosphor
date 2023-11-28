package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.*;

/**
 * Embeds the current values for the field of {@link Configuration} into the class file for {@link Configuration}.
 */
public class ConfigurationEmbeddingCV extends ClassVisitor {
    private static final String TARGET_METHOD_NAME = "<clinit>";

    protected ConfigurationEmbeddingCV(ClassVisitor classVisitor) {
        super(Configuration.ASM_VERSION, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return !name.equals(TARGET_METHOD_NAME) ? mv : new ConfigurationEmbeddingMV(mv);
    }

    public static boolean isApplicable(String className) {
        return Type.getInternalName(Configuration.class).equals(className);
    }

    private static class ConfigurationEmbeddingMV extends MethodVisitor {
        protected ConfigurationEmbeddingMV(MethodVisitor methodVisitor) {
            super(Configuration.ASM_VERSION, methodVisitor);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.PUTSTATIC) {
                try {
                    Field f = Configuration.class.getField(name);
                    f.setAccessible(true);
                    if (Modifier.isPublic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers())) {
                        replaceValue(Type.getType(descriptor), f.get(null));
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to access field owned by " + Configuration.class, e);
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        private void replaceValue(Type type, Object newValue) {
            switch (type.getSort()) {
                case Type.VOID:
                case Type.ARRAY:
                case Type.METHOD:
                    return;
            }
            // Pop the original value
            super.visitInsn(type.getSize() == 1 ? POP : POP2);
            // Push the new value
            if (type.getSort() != Type.OBJECT || newValue instanceof String) {
                super.visitLdcInsn(newValue);
            } else if (newValue == null) {
                super.visitInsn(ACONST_NULL);
            } else if (newValue instanceof Class) {
                mv.visitLdcInsn(Type.getType((Class<?>) newValue));
            } else if (newValue instanceof Set) {
                Set<?> set = (Set<?>) newValue;
                newInstance(newValue.getClass());
                for (Object element : set) {
                    super.visitInsn(DUP);
                    super.visitLdcInsn(element);
                    TaintMethodRecord.SET_ADD.delegateVisit(mv);
                    super.visitInsn(POP);
                }
            } else {
                newInstance(newValue.getClass());
            }
        }

        private void newInstance(Class<?> clazz) {
            try {
                clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Public, zero-argument constructor not found for: " + clazz);
            }
            String className = Type.getInternalName(clazz);
            super.visitTypeInsn(Opcodes.NEW, className);
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        }
    }
}
