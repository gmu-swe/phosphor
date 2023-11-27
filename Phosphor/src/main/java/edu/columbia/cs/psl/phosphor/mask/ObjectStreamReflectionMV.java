package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.IS_INSTANCE;

class ObjectStreamReflectionMV extends ReflectionMV {
    private final String methodName;

    ObjectStreamReflectionMV(MethodVisitor mv, String className, String methodName) {
        super(Configuration.ASM_VERSION, mv);
        if (!isApplicable(className, methodName)) {
            throw new IllegalArgumentException();
        }
        this.methodName = methodName;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (owner.equals("java/lang/Class") && name.startsWith("isInstance")) {
            // Even if we are not masking other methods, this must be masked
            IS_INSTANCE.delegateVisit(mv);
        } else if (owner.equals("sun/misc/Unsafe") && shouldMask(name)) {
            owner = ReflectionMVFactory.getRuntimeUnsafePropagatorClassName();
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        }
    }

    private boolean shouldMask(String name) {
        switch (methodName) {
            case "setObjFieldValues":
                return name.startsWith("putObject") || name.startsWith("compareAndSwapObject");
            case "getObjFieldValues":
                return name.startsWith("getObject");
            case "getPrimFieldValues":
            case "setPrimFieldValues":
                // Check for name.startsWith("put") || name.startsWith("get") was included but unhandled with a
                // TODO and the note: name = name + "$$NOUNBOX"
                // It is unclear if this needs to fixed
            default:
                return false;
        }
    }

    public static boolean isApplicable(String className, String methodName) {
        return (className.equals("java/io/ObjectStreamClass") || className.equals("java/io/ObjectStreamField"))
                && Configuration.TAINT_THROUGH_SERIALIZATION
                && !methodName.equals("getDeclaredSerialFields$$PHOSPHORTAGGED");
    }
}
