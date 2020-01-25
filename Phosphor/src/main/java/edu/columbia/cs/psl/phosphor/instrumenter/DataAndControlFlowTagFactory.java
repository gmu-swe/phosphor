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
    public void methodOp(int opcode, String owner, String name, String desc, boolean isInterface, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {

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
    public void methodEntered(String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {

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
        switch(opcode) {
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
                //VT VT
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                int tmp = lvs.getTmpLV(adapter.getTopOfStackType());
                mv.visitVarInsn(ASTORE, tmp);
                // V T V
                mv.visitInsn(SWAP);
                //V V T
                mv.visitVarInsn(ALOAD, tmp);
                //V V T T
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                //VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T VV
                mv.visitInsn(opcode);
                //T V
                mv.visitInsn(SWAP);
                lvs.freeTmpLV(tmp);
                break;
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
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                //VV T VV T
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                tmp = lvs.getTmpLV(adapter.getTopOfStackType());
                mv.visitVarInsn(ASTORE, tmp);
                // VV T VV
                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);
                //VV VV T
                mv.visitVarInsn(ALOAD, tmp);
                //VV VV T T
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                //VV  VV T
                mv.visitVarInsn(ASTORE, tmp);
                mv.visitInsn(opcode);
                mv.visitVarInsn(ALOAD, tmp);
                lvs.freeTmpLV(tmp);
                break;
            case Opcodes.LSHL:
            case Opcodes.LUSHR:
            case Opcodes.LSHR:
                //VV T V T
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                tmp = lvs.getTmpLV(adapter.getTopOfStackType());
                mv.visitVarInsn(ASTORE, tmp);
                // VV T V
                mv.visitInsn(SWAP);
                //VV V T
                mv.visitVarInsn(ALOAD, tmp);
                //VV V T T
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                //VV V T
                mv.visitVarInsn(ASTORE, tmp);
                mv.visitInsn(opcode);
                //V
                mv.visitVarInsn(ALOAD, tmp);
                lvs.freeTmpLV(tmp);
                break;
            case Opcodes.I2D:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.I2L:
                mv.visitInsn(SWAP);
                mv.visitInsn(opcode);
                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);
                break;
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.I2F:
            case Opcodes.F2I:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                mv.visitInsn(SWAP);
                mv.visitInsn(opcode);
                mv.visitInsn(SWAP);
                break;
            case Opcodes.D2I:
            case Opcodes.D2F:
            case Opcodes.L2I:
            case Opcodes.L2F:
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                mv.visitInsn(opcode);
                mv.visitInsn(SWAP);
                break;
            case Opcodes.D2L:
            case Opcodes.L2D:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                mv.visitInsn(opcode);
                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);
                break;
            case Opcodes.ARRAYLENGTH:
                Type arrType = TaintAdapter.getTypeForStackType(adapter.analyzer.stack.get(adapter.analyzer.stack.size() - 2));
                mv.visitInsn(DUP2);
                if(arrType.getSort() != Type.ARRAY) {
                    mv.visitInsn(POP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, arrType.getInternalName(), "getLength", "()I", false);
                } else {
                    mv.visitInsn(POP);
                    mv.visitInsn(opcode);
                }
                //Array Taint Length
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //Length array taint
                if(arrType.getSort() != Type.ARRAY) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "getLengthTaint", "(" + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
                    //A
                } else {
                    mv.visitInsn(POP2); //TODO maybe use reference taint for arraylength?
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                }
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
    public void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta, boolean loadIsTracked) {

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
    public void lookupSwitch(Label defaultLabel, int[] keys, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV) {

    }

    @Override
    public void tableSwitch(int min, int max, Label defaultLabel, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV) {

    }

    @Override
    public void propagateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv) {
        int idx = 0;
        Type[] argTypes = Type.getArgumentTypes(newDesc);
        if((acc & Opcodes.ACC_STATIC) == 0) {
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
