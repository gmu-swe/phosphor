package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.mask.JdkUnsafeAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class JdkUnsafeAdapterPatchingCV extends ClassVisitor {
    private final String UNSAFE_INTERNAL_NAME = "jdk/internal/misc/Unsafe";

    public JdkUnsafeAdapterPatchingCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    public static boolean isApplicable(String className, boolean patchUnsafeNames) {
        return Type.getInternalName(JdkUnsafeAdapter.class).equals(className) && !patchUnsafeNames;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (UNSAFE_INTERNAL_NAME.equals(owner)) {
                    name = name.replace("Object", "Reference");
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        };
    }
}
