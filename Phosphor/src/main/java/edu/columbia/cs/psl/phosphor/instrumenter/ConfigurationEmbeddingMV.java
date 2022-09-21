package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;

public class ConfigurationEmbeddingMV extends MethodVisitor  {
    public ConfigurationEmbeddingMV(MethodVisitor mv) {
        super(ASM9, mv);
    }

    // Embed initialized Configuration class into class file.
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (opcode == Opcodes.PUTSTATIC) {
            Type type = Type.getType(descriptor);
            Object fieldValue;
            if (name.equals("IS_JAVA_8")) {
                fieldValue = false;
            } else {
                try {
                    Field f = Configuration.class.getField(name);
                    if ((f.getModifiers() & ACC_PUBLIC) == 0) {
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                        return;
                    }
                    f.setAccessible(true);
                    fieldValue = f.get(null);
                    if (fieldValue instanceof Class) {
                        fieldValue = Type.getType((Class<?>) fieldValue);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                    return;
                }
            }
            switch (type.getSort()) {
                case Type.LONG:
                case Type.DOUBLE:
                    super.visitInsn(POP2);
                    super.visitLdcInsn(fieldValue);
                    break;
                case Type.BOOLEAN:
                case Type.INT:
                case Type.FLOAT:
                case Type.SHORT:
                case Type.BYTE:
                case Type.CHAR:
                    super.visitInsn(POP);
                    super.visitLdcInsn(fieldValue);
                    break;
                case Type.OBJECT:
                    if (fieldValue == null) {
                        super.visitInsn(POP);
                        super.visitInsn(ACONST_NULL);
                    } else if (descriptor.equals("Ljava/lang/Class;") ||
                            descriptor.equals("Ljava/lang/String;") ||
                            fieldValue instanceof String) {
                        super.visitInsn(POP);
                        super.visitLdcInsn(fieldValue);
                    } else if (descriptor.equals("Ledu/columbia/cs/psl/phosphor/struct/harmony/util/Set;")) {
                        // All sets are Set<String>.
                        super.visitInsn(POP);
                        String className = fieldValue.getClass().getName().replace(".", "/");
                        super.visitTypeInsn(Opcodes.NEW, className);
                        super.visitInsn(DUP);
                        super.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);

                        for (String s : ((Set<String>) fieldValue)) {
                            super.visitInsn(DUP);
                            super.visitLdcInsn(s);
                            super.visitMethodInsn(INVOKEINTERFACE,
                                    "Ledu/columbia/cs/psl/phosphor/struct/harmony/util/Set;",
                                    "add", "(Ljava/lang/Object;)Z", true);
                            super.visitInsn(POP);
                        }
                    } else {
                        Class<?> objectClass = fieldValue.getClass();
                        try {
                            // Make sure constructor is callable.
                            Constructor<?> constructor = objectClass.getDeclaredConstructor();
                            if ((constructor.getModifiers() & ACC_PUBLIC) != 0) {
                                String className = objectClass.getName().replace(".", "/");
                                super.visitInsn(POP);
                                super.visitTypeInsn(Opcodes.NEW, className);
                                super.visitInsn(DUP);
                                super.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
                            }
                        } catch (NoSuchMethodException e) {
                        }
                    }
                    break;
            }
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }
}
