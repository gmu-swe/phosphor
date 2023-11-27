package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.UnsafeProxy;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Performs patching of embedded Phosphors (for Java 9+).
 */
public class EmbeddedPhosphorPatcher {
    private final boolean patchUnsafeNames;

    public EmbeddedPhosphorPatcher(byte[] unsafeClassFileBuffer) {
        this(shouldPatchUnsafeNames(unsafeClassFileBuffer));
    }

    public EmbeddedPhosphorPatcher(boolean patchUnsafeNames) {
        this.patchUnsafeNames = patchUnsafeNames;
    }

    public byte[] patch(String name, byte[] content) throws IOException {
        if (name.equals("edu/columbia/cs/psl/phosphor/Configuration.class")) {
            return setConfigurationVersion(new ByteArrayInputStream(content));
        } else if (name.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeJDKInternalUnsafePropagator.class")) {
            return transformUnsafePropagator(
                    new ByteArrayInputStream(content), "jdk/internal/misc/Unsafe", patchUnsafeNames);
        } else {
            return content;
        }
    }

    private static boolean shouldPatchUnsafeNames(byte[] in) {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, ClassReader.SKIP_CODE);
        for (MethodNode mn : cn.methods) {
            if (mn.name.contains("putReference")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Modify {@link Configuration} to set {@link Configuration#IS_JAVA_8} to false.
     * This flag must be set correctly in order to boot the JVM.
     */
    private static byte[] setConfigurationVersion(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Configuration.ASM_VERSION, cw) {
            @Override
            public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("<clinit>")) {
                    return new ConfigurationEmbeddingMV(mv);
                } else {
                    return mv;
                }
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static byte[] transformUnsafePropagator(InputStream in, String unsafeInternalName, boolean patchUnsafeNames)
            throws IOException {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new UnsafePatchingCV(cw, unsafeInternalName, patchUnsafeNames);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    private static class UnsafePatchingCV extends ClassVisitor {
        private static final String UNSAFE_PROXY_INTERNAL_NAME = Type.getInternalName(UnsafeProxy.class);
        private static final String UNSAFE_PROXY_DESC = Type.getDescriptor(UnsafeProxy.class);
        private final String unsafeDesc;
        private final boolean patchNames;
        private final String unsafeInternalName;

        public UnsafePatchingCV(ClassWriter cw, String unsafeInternalName, boolean patchNames) {
            super(Configuration.ASM_VERSION, cw);
            this.unsafeInternalName = unsafeInternalName;
            this.unsafeDesc = "L" + unsafeInternalName + ";";
            this.patchNames = patchNames;
        }

        private String patchInternalName(String name) {
            return UNSAFE_PROXY_INTERNAL_NAME.equals(name) ? unsafeInternalName : name;
        }

        private String patchDesc(String desc) {
            return desc == null ? null : desc.replace(UNSAFE_PROXY_DESC, unsafeDesc);
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
                public void visitMethodInsn(
                        int opcode, String owner, String name, String descriptor, boolean isInterface) {
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
}
