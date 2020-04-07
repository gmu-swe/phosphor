package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import org.objectweb.asm.*;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_TYPE;
import static edu.columbia.cs.psl.phosphor.SourceSinkManager.remapMethodDescToRemoveTaintsAndReturnType;

public class SerializationFixingCV extends ClassVisitor implements Opcodes {

    // ObjectInputStream class name
    private static final String INPUT_STREAM_NAME = "java/io/ObjectInputStream";
    // ObjectOutputStream class name
    private static final String OUTPUT_STREAM_NAME = "java/io/ObjectOutputStream";
    // ObjectStreamClass class name
    private static final String STREAM_CLASS_NAME = "java/io/ObjectStreamClass";
    // Header byte for serialized objects
    private static final  byte TC_OBJECT = (byte) 0x73;
    // Header byte serialized null values
    private static final byte TC_NULL = (byte) 0x70;

    // Name of class being visited
    private final String className;

    public SerializationFixingCV(ClassVisitor cv, String className) {
        super(Configuration.ASM_VERSION, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(STREAM_CLASS_NAME.equals(className)) {
            return new StreamClassMV(mv);
        } else {
            switch(name) {
                case "readObject$$PHOSPHORTAGGED":
                case "readObject0$$PHOSPHORTAGGED":
                    return new ObjectReadMV(mv);
                case "writeObject$$PHOSPHORTAGGED":
                case "writeObject0$$PHOSPHORTAGGED":
                    return new ObjectWriteMV(mv);
                case "writeInt$$PHOSPHORTAGGED":
                case "writeLong$$PHOSPHORTAGGED":
                case "writeBoolean$$PHOSPHORTAGGED":
                case "writeShort$$PHOSPHORTAGGED":
                case "writeDouble$$PHOSPHORTAGGED":
                case "writeByte$$PHOSPHORTAGGED":
                case "writeChar$$PHOSPHORTAGGED":
                case "writeFloat$$PHOSPHORTAGGED":
                    return new PrimitiveWriteMV(mv, Type.getArgumentTypes(desc)[1].getSize());
                case "readInt$$PHOSPHORTAGGED":
                case "readLong$$PHOSPHORTAGGED":
                case "readBoolean$$PHOSPHORTAGGED":
                case "readShort$$PHOSPHORTAGGED":
                case "readDouble$$PHOSPHORTAGGED":
                case "readByte$$PHOSPHORTAGGED":
                case "readChar$$PHOSPHORTAGGED":
                case "readFloat$$PHOSPHORTAGGED":
                case "readUnsignedByte$$PHOSPHORTAGGED":
                case "readUnsignedShort$$PHOSPHORTAGGED":
                    return new PrimitiveReadMV(mv, Type.getReturnType(desc));
                default:
                    return mv;
            }
        }
    }

    /* Returns whether this class visitor should be applied to the class with the specified name. */
    public static boolean isApplicable(String className) {
        return INPUT_STREAM_NAME.equals(className) || OUTPUT_STREAM_NAME.equals(className)
                || STREAM_CLASS_NAME.equals(className);
    }

    @SuppressWarnings("unused")
    public static Object wrapIfNecessary(Object obj, Taint tag) {
        if(tag != null && !tag.isEmpty()) {
            return new TaintedReferenceWithObjTag(tag, obj);
        }
        return obj;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag unwrapIfNecessary(TaintedReferenceWithObjTag ret) {
        if(ret.val instanceof TaintedReferenceWithObjTag) {
            return (TaintedReferenceWithObjTag) ret.val;
        }
        return ret;
    }

    private static class StreamClassMV extends MethodVisitor {

        StreamClassMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean isInterface) {
            if(OUTPUT_STREAM_NAME.equals(owner) && name.startsWith("write")) {
                Type[] args = Type.getArgumentTypes(desc);
                if(args.length > 0 && Type.getType(Configuration.TAINT_TAG_DESC).equals(args[0])) {
                    String untaintedMethod = name.replace(TaintUtils.METHOD_SUFFIX, "");
                    String untaintedDesc = remapMethodDescToRemoveTaintsAndReturnType(desc) + "V";
                    boolean widePrimitive = Type.DOUBLE_TYPE.equals(args[1]) || Type.LONG_TYPE.equals(args[1]);
                    if(args.length == 2) {
                        //TODO this is not at all set up for reference tags... but tests pass anyway?
                        // stream, taint, primitive
                        super.visitInsn(widePrimitive ? DUP2_X1 : DUP_X1);
                        super.visitInsn(widePrimitive ? POP2 : POP);
                        super.visitInsn(POP);
                        super.visitMethodInsn(opcode, owner, untaintedMethod, untaintedDesc, isInterface);
                        return;
                    } else if(args.length == 4 && args[3].equals(CONTROL_STACK_TYPE)) {
                        //Taint primitive taint ControlFlowStack
                        super.visitInsn(POP);
                        super.visitInsn(POP);
                        //Taint primitive
                        super.visitInsn(widePrimitive ? DUP2_X1 : DUP_X1);
                        super.visitInsn(widePrimitive ? POP2 : POP);
                        super.visitInsn(POP);
                        super.visitMethodInsn(opcode, owner, untaintedMethod, untaintedDesc, isInterface);
                        return;
                    }
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        }
    }

    private static class PrimitiveWriteMV extends MethodVisitor {

        int sizeOfArg;

        PrimitiveWriteMV(MethodVisitor mv, int sizeOfArg) {
            super(Configuration.ASM_VERSION, mv);
            this.sizeOfArg = sizeOfArg;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label label1 = new Label();
            // Note - used to be a null check here, but taints should not be null.
            // Check that taint is non-empty
            super.visitVarInsn(ALOAD, 2 + sizeOfArg);
            super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "isEmpty", "(" + Configuration.TAINT_TAG_DESC + ")Z", false);
            super.visitJumpInsn(IFNE, label1);
            // Write the taint if non-null and non-empty
            super.visitVarInsn(ALOAD, 0);
            super.visitVarInsn(ALOAD, 2 + sizeOfArg); // Load taint onto stack
            super.visitMethodInsn(INVOKEVIRTUAL, OUTPUT_STREAM_NAME, "writeObject", "(Ljava/lang/Object;)V", false);
            super.visitLabel(label1);
        }
    }

    private static class PrimitiveReadMV extends MethodVisitor {

        private final Type returnType;

        PrimitiveReadMV(MethodVisitor mv, Type returnType) {
            super(Configuration.ASM_VERSION, mv);
            this.returnType = returnType;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label label1 = new Label();
            Label label2 = new Label();
            Label label3 = new Label();
            Label label4 = new Label();
            Label label5 = new Label();
            super.visitVarInsn(ALOAD, 0);
            super.visitFieldInsn(GETFIELD, INPUT_STREAM_NAME, "bin", "Ljava/io/ObjectInputStream$BlockDataInputStream;");
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "getBlockDataMode", "()Z", false);
            super.visitJumpInsn(IFEQ, label1);
            //
            super.visitVarInsn(ALOAD, 0);
            super.visitFieldInsn(GETFIELD, INPUT_STREAM_NAME, "bin", "Ljava/io/ObjectInputStream$BlockDataInputStream;");
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "currentBlockRemaining", "()I", false);
            super.visitJumpInsn(IFNE, label2);
            // Get current value of block data mode
            super.visitLabel(label1);
            super.visitVarInsn(ALOAD, 0);
            super.visitFieldInsn(GETFIELD, INPUT_STREAM_NAME, "bin", "Ljava/io/ObjectInputStream$BlockDataInputStream;");
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "getBlockDataMode", "()Z", false);
            // Set block data mode to false
            super.visitVarInsn(ALOAD, 0);
            super.visitFieldInsn(GETFIELD, INPUT_STREAM_NAME, "bin", "Ljava/io/ObjectInputStream$BlockDataInputStream;");
            super.visitInsn(ICONST_0);
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "setBlockDataMode", "(Z)Z", false);
            super.visitInsn(POP);
            // Peek at next byte
            super.visitVarInsn(ALOAD, 0);
            super.visitFieldInsn(GETFIELD, INPUT_STREAM_NAME, "bin", "Ljava/io/ObjectInputStream$BlockDataInputStream;");
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "peek", "()I", false);
            // Restore previous block data mode
            super.visitInsn(DUP_X2);
            super.visitInsn(POP);
            super.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream$BlockDataInputStream", "setBlockDataMode", "(Z)Z", false);
            super.visitInsn(POP);
            // Compare next byte to byte for objects
            super.visitInsn(DUP); // Duplicate the "peeked" value
            super.visitIntInsn(BIPUSH, TC_OBJECT);
            super.visitJumpInsn(IF_ICMPEQ, label4);
            super.visitIntInsn(BIPUSH, TC_NULL);
            super.visitJumpInsn(IF_ICMPEQ, label5);
            super.visitJumpInsn(GOTO, label2);
            super.visitLabel(label4);
            super.visitInsn(POP); // Remove unused "peeked" values
            // Read the tag
            super.visitLabel(label5);
            super.visitVarInsn(ALOAD, 0);
            super.visitMethodInsn(INVOKEVIRTUAL, INPUT_STREAM_NAME, "readObject", "()Ljava/lang/Object;", false);
            super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
            super.visitJumpInsn(GOTO, label3);
            // Push null onto stack
            super.visitLabel(label2);
            super.visitInsn(ACONST_NULL);
            super.visitLabel(label3);
        }

        @Override
        public void visitInsn(int opcode) {
            if(OpcodesUtil.isReturnOpcode(opcode)) {
                super.visitInsn(DUP_X1);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
            }
            super.visitInsn(opcode);
        }
    }

    private static class ObjectWriteMV extends MethodVisitor {

        ObjectWriteMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitVarInsn(ALOAD, 2);
            super.visitVarInsn(ALOAD, 3);

            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SerializationFixingCV.class), "wrapIfNecessary", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")Ljava/lang/Object;", false);
            super.visitVarInsn(ASTORE, 2);
        }
    }

    private static class ObjectReadMV extends MethodVisitor {

        ObjectReadMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if(OpcodesUtil.isReturnOpcode(opcode)) {
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SerializationFixingCV.class), "unwrapIfNecessary", "(" + Type.getDescriptor(TaintedReferenceWithObjTag.class) + ")" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
            }
            super.visitInsn(opcode);
        }
    }
}
