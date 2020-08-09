package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

    private String clazz;
    private boolean fixLdcClass;
    private LocalVariableManager lvs;
    private int localIdxOfControlTag;

    public SpecialOpcodeRemovingMV(MethodVisitor sup, boolean ignoreFrames, int acc, String clazz, String desc, boolean fixLdcClass) {
        super(Configuration.ASM_VERSION, sup);
        this.clazz = clazz;
        this.fixLdcClass = fixLdcClass;
        int n = 0;
        if((acc & Opcodes.ACC_STATIC) == 0) {
            n++;
        }
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getDescriptor().equals(CONTROL_STACK_DESC)) {
                this.localIdxOfControlTag = n;
            }
            n += t.getSize();
        }
    }

    /**
     * Visits a field instruction. A field instruction is an instruction that
     * loads or stores the value of a field of an object.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode
     *               is either GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
     * @param owner  the internal name of the field's owner class (see
     *               {@link Type#getInternalName() getInternalName}).
     * @param name   the field's name.
     * @param desc   the field's descriptor (see {@link Type Type}).
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if(opcode < 200) {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public void setLVS(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if(opcode != TaintUtils.IGNORE_EVERYTHING) {
            super.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        Type descType = Type.getType(desc);
        if(descType.getSort() == Type.ARRAY && descType.getDimensions() > 1 && descType.getElementType().getSort() != Type.OBJECT) {
            //remap!
            desc = MultiDTaintedArray.getTypeForType(descType).getDescriptor();
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(opcode > 200) {
            return;
        }
        //TODO delete this check
        if(opcode == Opcodes.CHECKCAST && type.startsWith("[Ledu/columbia/cs/psl/phosphor/struct/LazyReference")) {
            throw new IllegalStateException();
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        if(type == TaintUtils.RAW_INSN) {
            type = Opcodes.F_NEW;
        }
        //At this point, make sure that there are no TaggedValue's sitting around.
        Object[] newLocal = new Object[local.length];
        Object[] newStack = new Object[stack.length];
        for(int i = 0; i < local.length; i++) {
            if(local[i] instanceof TaggedValue) {
                newLocal[i] = ((TaggedValue) local[i]).v;
            } else {
                newLocal[i] = local[i];
            }
        }
        for(int i = 0; i < stack.length; i++) {
            if(stack[i] instanceof TaggedValue) {
                newStack[i] = ((TaggedValue) stack[i]).v;
            } else {
                newStack[i] = stack[i];
            }
        }
        super.visitFrame(type, nLocal, newLocal, nStack, newStack);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof Type && fixLdcClass) {
            int prealloc = lvs.getPreAllocatedReturnTypeVar(Type.getType(TaintedReferenceWithObjTag.class));

            super.visitLdcInsn(((Type) cst).getInternalName().replace("/", "."));
            NEW_EMPTY_TAINT.delegateVisit(mv);
            super.visitInsn(Opcodes.ICONST_0);
            NEW_EMPTY_TAINT.delegateVisit(mv);
            super.visitLdcInsn(clazz.replace("/", "."));
            NEW_EMPTY_TAINT.delegateVisit(mv);

            if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                if(this.localIdxOfControlTag < 0) {
                    localIdxOfControlTag = lvs.getIndexOfMasterControlLV();
                }
                String ctrlDesc = CONTROL_STACK_DESC;
                //Str Taint 0 taint ThisClazz Taint
                super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + ctrlDesc + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
                NEW_EMPTY_TAINT.delegateVisit(mv);
                super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader$$PHOSPHORTAGGED", "(" + Configuration.TAINT_TAG_DESC + ctrlDesc + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/Classloader;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/ClassLoader");
                NEW_EMPTY_TAINT.delegateVisit(mv);
                super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + "Z" + Configuration.TAINT_TAG_DESC + "Ljava/lang/ClassLoader;" + Configuration.TAINT_TAG_DESC + ctrlDesc + Type.getDescriptor(TaintedReferenceWithObjTag.class)
                        + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
            } else {
                //Str Taint 0 taint ThisClazz Taint
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
                NEW_EMPTY_TAINT.delegateVisit(mv);
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader$$PHOSPHORTAGGED", "(" + Configuration.TAINT_TAG_DESC + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/ClassLoader;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/ClassLoader");
                NEW_EMPTY_TAINT.delegateVisit(mv);
                super.visitVarInsn(Opcodes.ALOAD, prealloc);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + "Z" + Configuration.TAINT_TAG_DESC + "Ljava/lang/ClassLoader;" + Configuration.TAINT_TAG_DESC + Type.getDescriptor(TaintedReferenceWithObjTag.class)
                        + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
            }
        } else if(!(cst instanceof PhosphorInstructionInfo)) {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch(opcode) {
            case TaintUtils.FOLLOWED_BY_FRAME:
            case TaintUtils.RAW_INSN:
            case TaintUtils.NO_TAINT_STORE_INSN:
            case TaintUtils.IGNORE_EVERYTHING:
            case TaintUtils.IS_TMP_STORE:
                break;
            default:
                super.visitInsn(opcode);
        }
    }
}
