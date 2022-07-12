package edu.columbia.cs.psl.phosphor.instrumenter;


import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InstrumentedJREProxyGenerator {
    static String[] CLASSES = {"edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper",
            "edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper"};

    public static void main(String[] args) throws IOException {
        String outputDir = args[0];
        String version = System.getProperty("java.version");
        for (String clazz : CLASSES) {
            String classLocation = outputDir + '/' + clazz.replace('.', '/') + ".class";
            ClassReader cr = new ClassReader(new FileInputStream(classLocation));
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cr.accept(
                    new CheckClassAdapter(
                            new ClassVisitor(Configuration.ASM_VERSION, cw) {
                                @Override
                                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                    GeneratorAdapter ga = new GeneratorAdapter(mv, access, name, descriptor);

                                    ga.visitCode();
                                    if (name.equals("<clinit>")) {
                                    } else if (name.equals("<init>")) {
                                        ga.visitVarInsn(Opcodes.ALOAD, 0);
                                        ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                                    } else if (name.equals("_crash")) {
                                        ga.visitInsn(Opcodes.ACONST_NULL);
                                    } else if (clazz.endsWith("FieldHelper")) {
                                        generateFieldHelper(ga, name, descriptor);
                                    } else {
                                        generateMethodHelper(ga, name, descriptor);
                                    }
                                    ga.returnValue();
                                    ga.visitEnd();
                                    ga.visitMaxs(0, 0);
                                    return ga;
                                }
                            }, false), ClassReader.SKIP_CODE);
            byte[] ret = cw.toByteArray();
            Files.write(Paths.get(classLocation), ret);
        }
    }

    public static void generateFieldHelper(GeneratorAdapter ga, String name, String descriptor) {
        Type returnType = Type.getReturnType(descriptor);
        String fieldOwner = Type.getArgumentTypes(descriptor)[0].getInternalName();
        if (name.startsWith("JAVA_8")) {
            name = name.substring(6);
        }
        String fieldName = name.substring(3);
        Type actualFieldType = null;

        if (name.startsWith("get")) {
            ga.visitVarInsn(Opcodes.ALOAD, 0);
            ga.visitFieldInsn(Opcodes.GETFIELD, fieldOwner, fieldName, (actualFieldType != null ? actualFieldType.getDescriptor() : returnType.getDescriptor()));
        } else {
            Type argType = Type.getArgumentTypes(descriptor)[1];
            ga.visitVarInsn(Opcodes.ALOAD, 0);
            ga.visitVarInsn(argType.getOpcode(Opcodes.ILOAD), 1);
            if (actualFieldType != null) {
                ga.visitTypeInsn(Opcodes.CHECKCAST, actualFieldType.getInternalName());
            }
            ga.visitFieldInsn(Opcodes.PUTFIELD, fieldOwner, fieldName, (actualFieldType != null ? actualFieldType.getDescriptor() : argType.getDescriptor()));
        }
    }

    public static void generateMethodHelper(GeneratorAdapter ga, String name, String descriptor) {
        boolean isInstanceMethod = name.endsWith("_i");
        int opcode = Opcodes.INVOKESTATIC;
        boolean isIface = false;
        if (isInstanceMethod) {
            name = name.substring(0, name.length() - 2);
            opcode = Opcodes.INVOKEVIRTUAL;
        }
        String owner = name.substring(0, name.lastIndexOf('_')).replace('_', '/');

        String methodName = name.substring(name.lastIndexOf('_') + 1);
        String descriptorToInvoke = descriptor;
        if (methodName.equals("init")) {
            opcode = Opcodes.INVOKESPECIAL;
            ga.visitTypeInsn(Opcodes.NEW, owner);
            ga.visitInsn(Opcodes.DUP);
            methodName = "<init>";
            descriptorToInvoke = descriptorToInvoke.substring(0, descriptor.indexOf(')')) + ")V";
        }
        if (isInstanceMethod) {
            descriptorToInvoke = "(" + descriptor.substring(descriptor.indexOf(';') + 1);
            try {
                Class c = Class.forName(owner.replace('/', '.'));
                if (c.isInterface()) {
                    isIface = true;
                    opcode = Opcodes.INVOKEVIRTUAL;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        Type[] args = Type.getArgumentTypes(descriptor);
        int argIdx = 0;
        for (int i = 0; i < args.length; i++) {
            ga.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), argIdx);
            argIdx += args[i].getSize();
        }
        ga.visitMethodInsn(opcode, owner, methodName, descriptorToInvoke, isIface);
    }
}
