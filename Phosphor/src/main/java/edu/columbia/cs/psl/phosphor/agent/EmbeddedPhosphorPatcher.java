package edu.columbia.cs.psl.phosphor.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Performs patching of embedded Phosphor JAR (for Java 9+).
 */
public class EmbeddedPhosphorPatcher {
    private final boolean patchUnsafeNames;

    public EmbeddedPhosphorPatcher(byte[] unsafeClassFileBuffer) {
        this.patchUnsafeNames = shouldPatchUnsafeNames(unsafeClassFileBuffer);
    }

    public byte[] patch(String name, byte[] content) {
        name = name.replace(".class", "");
        if (ConfigurationEmbeddingCV.isApplicable(name)) {
            return PhosphorPatcher.apply(content, ConfigurationEmbeddingCV::new);
        } else if (name.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeJDKInternalUnsafePropagator")) {
            return PhosphorPatcher.apply(
                    content, cv -> new UnsafePatchingCV(cv, "jdk/internal/misc/Unsafe", patchUnsafeNames));
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
}
