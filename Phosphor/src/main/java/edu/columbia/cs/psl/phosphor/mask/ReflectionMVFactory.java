package edu.columbia.cs.psl.phosphor.mask;

import org.objectweb.asm.MethodVisitor;

public final class ReflectionMVFactory {
    public static ReflectionMV create(MethodVisitor mv, String className, String methodName) {
        if (ObjectStreamReflectionMV.isApplicable(className, methodName)) {
            return new ObjectStreamReflectionMV(mv, className, methodName);
        } else if (DisabledReflectionMV.isApplicable(className, methodName)) {
            return new DisabledReflectionMV(mv, className, methodName);
        } else {
            return new ReflectionHidingMV(mv, className, methodName);
        }
    }
}
