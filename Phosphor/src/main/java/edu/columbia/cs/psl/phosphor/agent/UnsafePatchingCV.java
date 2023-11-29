package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.mask.UnsafeProxy;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class UnsafePatchingCV extends ClassVisitor {
    private static final String UNSAFE_PROXY_INTERNAL_NAME = Type.getInternalName(UnsafeProxy.class);
    private static final String UNSAFE_PROXY_DESC = Type.getDescriptor(UnsafeProxy.class);
    private final boolean patchNames;
    private final String UNSAFE_INTERNAL_NAME = "jdk/internal/misc/Unsafe";

    public UnsafePatchingCV(ClassVisitor cv, boolean patchNames) {
        super(Configuration.ASM_VERSION, cv);
        this.patchNames = patchNames;
    }

    private String patchInternalName(String name) {
        return UNSAFE_PROXY_INTERNAL_NAME.equals(name) ? UNSAFE_INTERNAL_NAME : name;
    }

    private String patchDesc(String desc) {
        String UNSAFE_DESCRIPTOR = "L" + UNSAFE_INTERNAL_NAME + ";";
        return desc == null ? null : desc.replace(UNSAFE_PROXY_DESC, UNSAFE_DESCRIPTOR);
    }

    private String patchMethodName(String owner, String name) {
        return patchNames && owner.equals(UNSAFE_PROXY_INTERNAL_NAME) ? name.replace("Reference", "Object") : name;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, patchDesc(descriptor), patchDesc(signature), exceptions);
        return new MethodVisitor(api, mv) {
            @Override
            public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
                for (int i = 0; i < numLocal; i++) {
                    if (local[i] instanceof String) {
                        local[i] = patchInternalName((String) local[i]);
                    }
                }
                for (int i = 0; i < numStack; i++) {
                    if (stack[i] instanceof String) {
                        stack[i] = patchInternalName((String) stack[i]);
                    }
                }
                super.visitFrame(type, numLocal, local, numStack, stack);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                super.visitFieldInsn(opcode, patchInternalName(owner), name, patchDesc(descriptor));
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitMethodInsn(
                        opcode,
                        patchInternalName(owner),
                        patchMethodName(owner, name),
                        patchDesc(descriptor),
                        isInterface);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                super.visitTypeInsn(opcode, patchInternalName(type));
            }

            @Override
            public void visitLocalVariable(
                    String name, String descriptor, String signature, Label start, Label end, int index) {
                super.visitLocalVariable(name, patchDesc(descriptor), signature, start, end, index);
            }
        };
    }
}
