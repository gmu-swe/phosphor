package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.COMBINE_TAGS;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;

public class DataAndControlFlowTagFactory implements TaintTagFactory, Opcodes {

    @Override
    public Taint<?> getAutoTaint(String source) {
        return Taint.withLabel(source);
    }

    @Override
    public void methodOp(int opcode, String owner, String name, String desc, boolean isInterface, MethodVisitor mv,
            LocalVariableManager lvs, TaintPassingMV ta) {

    }

    @Override
    public void instrumentationStarting(int access, String methodName, String methodDesc) {

    }

    @Override
    public void insnIndexVisited(int offset) {

    }

    @Override
    public void lineNumberVisited(int line) {

    }

    @Override
    public void methodEntered(String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs,
            TaintPassingMV ta) {

    }

    @Override
    public void signalOp(int signal, Object option) {

    }

    @Override
    public void generateEmptyTaint(MethodVisitor mv) {
        NEW_EMPTY_TAINT.delegateVisit(mv);
    }

    @Override
    public void generateEmptyTaintArray(Object[] array, int dims) {

    }

    @Override
    public void intOp(int opcode, int arg, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV adapter) {

    }

    @Override
    public void stackOp(int opcode, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV adapter) {
        switch (opcode) {
            case Opcodes.FADD:
            case Opcodes.FREM:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IOR:
            case Opcodes.IAND:
            case Opcodes.IXOR:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LADD:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LSHL:
            case Opcodes.LUSHR:
            case Opcodes.LSHR:
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                int destinationTagSlot = lvs.getStackShadowVarFromTop(1);
                if (Configuration.WITHOUT_PROPAGATION) {
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    int t1 = lvs.getStackShadowVarFromTop(0);
                    int t2 = lvs.getStackShadowVarFromTop(1);
                    mv.visitVarInsn(ALOAD, t2);
                    mv.visitVarInsn(ALOAD, t1);
                    COMBINE_TAGS.delegateVisit(mv);
                }
                mv.visitVarInsn(ASTORE, destinationTagSlot);
                mv.visitInsn(opcode);
                break;
            case Opcodes.I2D:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.I2L:
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.I2F:
            case Opcodes.F2I:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.D2I:
            case Opcodes.D2F:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.D2L:
            case Opcodes.L2D:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
                mv.visitInsn(opcode);
                break;
            case Opcodes.ARRAYLENGTH:
                Type arrType = TaintAdapter
                        .getTypeForStackType(adapter.analyzer.stack.get(adapter.analyzer.stack.size() - 1));
                mv.visitInsn(DUP);
                if (arrType.getSort() != Type.ARRAY) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, arrType.getInternalName(), "getLength", "()I", false);
                } else {
                    mv.visitInsn(opcode);
                }
                // Array Length
                mv.visitInsn(SWAP);
                // Length array
                if (arrType.getSort() != Type.ARRAY) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "getLengthTaint",
                            "()" + Configuration.TAINT_TAG_DESC, false);
                    // A
                } else {
                    mv.visitInsn(POP); // TODO maybe use reference taint for arraylength?
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                }
                // Length taint
                adapter.storeStackTopShadowVar();
                break;
        }
    }

    @Override
    public void jumpOp(int opcode, Label label, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {

    }

    @Override
    public void typeOp(int opcode, String type, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {

    }

    @Override
    public void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {

    }

    @Override
    public void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs,
            TaintPassingMV ta, boolean loadIsTracked) {

    }

    @Override
    public boolean isIgnoredClass(String className) {
        return false;
    }

    @Override
    public void instrumentationStarting(String className) {

    }

    @Override
    public void instrumentationEnding(String className) {

    }

    @Override
    public boolean isInternalTaintingClass(String className) {
        return false;
    }

    @Override
    public void lookupSwitch(Label defaultLabel, int[] keys, Label[] labels, MethodVisitor mv, LocalVariableManager lvs,
            TaintPassingMV taintPassingMV) {

    }

    @Override
    public void tableSwitch(int min, int max, Label defaultLabel, Label[] labels, MethodVisitor mv,
            LocalVariableManager lvs, TaintPassingMV taintPassingMV) {

    }

    @Override
    public void propagateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv) {
        int idx = 0;
        Type[] argTypes = Type.getArgumentTypes(newDesc);
        if ((acc & Opcodes.ACC_STATIC) == 0) {
            idx++;
            idx++;
        }
        for (Type t : argTypes) {
            idx += t.getSize();
            if (TaintUtils.isShadowedType(t)) {
                mv.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
                COMBINE_TAGS.delegateVisit(mv);
                idx++;
            }
        }
    }

    @Override
    public void generateSetTag(MethodVisitor mv, String className) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
    }
}
