package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.SerializationWrapper;
import org.objectweb.asm.*;

public class SerializationFixingCV extends ClassVisitor implements Opcodes {

    // ObjectInputStream class name
    private static final String INPUT_STREAM_NAME = "java/io/ObjectInputStream";
    // ObjectOutputStream class name
    private static final String OUTPUT_STREAM_NAME = "java/io/ObjectOutputStream";

    public SerializationFixingCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    /* Returns whether this class visitor should be applied to the class with the specified name. */
    public static boolean isApplicable(String className) {
        return Configuration.MULTI_TAINTING && (INPUT_STREAM_NAME.equals(className) || OUTPUT_STREAM_NAME.equals(className));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        switch(name) {
            case "writeObject":
            case "writeObject$$PHOSPHORTAGGED":
            case "writeObject0$$PHOSPHORTAGGED":
                return new ObjectWriteMV(mv);
            case "readObject":
            case "readObject$$PHOSPHORTAGGED":
            case "readObject0$$PHOSPHORTAGGED":
                return new ObjectReadMV(mv);
            default:
                return mv;
        }
    }

    private static class ObjectWriteMV extends MethodVisitor {

        ObjectWriteMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitVarInsn(ALOAD, 1);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SerializationFixingCV.class), "wrapIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            super.visitVarInsn(ASTORE, 1);
        }
    }

    private static class ObjectReadMV extends MethodVisitor {

        ObjectReadMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if(TaintUtils.isReturnOpcode(opcode)) {
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SerializationFixingCV.class), "unwrapIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
            super.visitInsn(opcode);
        }
    }

    @SuppressWarnings("unused")
    public static Object wrapIfNecessary(Object obj) {
        Taint tag = MultiTainter.getTaint(obj);
        if(tag != null && !tag.isEmpty()) {
            if(obj instanceof Boolean) {
                return SerializationWrapper.wrap((Boolean) obj);
            } else if(obj instanceof Byte) {
                return SerializationWrapper.wrap((Byte) obj);
            } else if(obj instanceof Character) {
                return SerializationWrapper.wrap((Character) obj);
            } else if(obj instanceof Short) {
                return SerializationWrapper.wrap((Short) obj);
            }
        }
        return obj;
    }

    @SuppressWarnings("unused")
    public static Object unwrapIfNecessary(Object obj) {
        if(obj instanceof SerializationWrapper) {
            return ((SerializationWrapper)obj).unwrap();
        }
        return obj;
    }
}
