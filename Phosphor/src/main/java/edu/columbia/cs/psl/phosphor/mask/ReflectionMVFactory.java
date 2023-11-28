package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.RuntimeSunMiscUnsafePropagator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class ReflectionMVFactory {
    public static ReflectionMV create(MethodVisitor mv, String className, String methodName) {
        // TODO
        MaskRegistry.MaskInfo mask = MaskRegistry.getMask(className, methodName, "()V");
        if (ObjectStreamReflectionMV.isApplicable(className, methodName)) {
            return new ObjectStreamReflectionMV(mv, className, methodName);
        } else if (DisabledReflectionMV.isApplicable(className, methodName)) {
            return new DisabledReflectionMV(mv, className, methodName);
        } else {
            return new ReflectionHidingMV(mv, className, methodName);
        }
    }

    public static String getRuntimeUnsafePropagatorClassName() {
        return Type.getInternalName(
                Configuration.IS_JAVA_8
                        ? RuntimeSunMiscUnsafePropagator.class
                        : RuntimeJDKInternalUnsafePropagator.class);
    }
}
