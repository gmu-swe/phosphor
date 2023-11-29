package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.mask.JdkUnsafeAdapter;
import edu.columbia.cs.psl.phosphor.runtime.mask.SunUnsafeAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.regex.Pattern;

public class UnsafeAdapterPatchingCV extends ClassVisitor {
    private String className;

    public UnsafeAdapterPatchingCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (owner.equals(className) && opcode == Opcodes.INVOKESTATIC) {
                    Type[] args = Type.getArgumentTypes(descriptor);
                    owner = args[0].getInternalName();
                    descriptor = descriptor.replaceFirst(Pattern.quote(args[0].getDescriptor()), "");
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }

    public static boolean isApplicable(String className) {
        return Type.getInternalName(SunUnsafeAdapter.class).equals(className)
                || Type.getInternalName(JdkUnsafeAdapter.class).equals(className);
    }
}
