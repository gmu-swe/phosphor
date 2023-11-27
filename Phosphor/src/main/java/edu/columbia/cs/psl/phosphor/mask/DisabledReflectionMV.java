package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.IS_INSTANCE;

class DisabledReflectionMV extends ReflectionMV implements Opcodes {
    DisabledReflectionMV(MethodVisitor mv, String className, String methodName) {
        super(Configuration.ASM_VERSION, mv);
        if (!isApplicable(className, methodName)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (owner.equals("java/lang/Class") && name.startsWith("isInstance")) {
            // Even if we are not masking other methods, this must be masked
            IS_INSTANCE.delegateVisit(mv);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        }
    }

    public static boolean isApplicable(String className, String methodName) {
        switch (className) {
            case "org/codehaus/groovy/vmplugin/v5/Java5":
                return methodName.equals("makeInterfaceTypes");
            case "jdk/internal/reflect/ReflectionFactory":
            case "java/lang/reflect/ReflectAccess":
                // Java >= 9
                // TODO keep?
            case "java/io/ObjectOutputStream":
            case "java/io/ObjectInputStream":
                return true;
            default:
                return false;
        }
    }
}
