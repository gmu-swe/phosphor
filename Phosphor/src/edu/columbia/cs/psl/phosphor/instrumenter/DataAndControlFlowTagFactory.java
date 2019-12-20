package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;


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
                //T V T V
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                int tmp = lvs.getTmpLV(adapter.getTopOfStackType());
                mv.visitVarInsn(FSTORE, tmp);
                //T V T
                mv.visitInsn(SWAP);
                //T T V
                mv.visitVarInsn(FLOAD, tmp);
                //T T V V
                mv.visitInsn(opcode);
                //T T V
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                mv.visitInsn(SWAP);
                lvs.freeTmpLV(tmp);
                break;
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
                tmp = lvs.getTmpLV(Type.INT_TYPE);
                //T V T V
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(ISTORE, tmp);
                //T V T
                mv.visitInsn(SWAP);
                //T T V
                mv.visitVarInsn(ILOAD, tmp);
                lvs.freeTmpLV(tmp);
                //T T V V
                mv.visitInsn(opcode);
                //T T V
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                mv.visitInsn(SWAP);
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                boolean secondHas0Taint = adapter.secondHas0Taint();
                //Do it with LVs where it is less opcodes.
                //T VV T VV
                tmp = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(DSTORE, tmp);
                //T VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T T VV
                int tmp2 = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(DSTORE, tmp2);
                if(secondHas0Taint) {
                    //0 T
                    mv.visitInsn(SWAP);
                    mv.visitInsn(POP);
                } else {
                    //T T
                    if(Configuration.WITHOUT_PROPAGATION) {
                        mv.visitInsn(POP2);
                        NEW_EMPTY_TAINT.delegateVisit(mv);
                    } else {
                        COMBINE_TAGS.delegateVisit(mv);
                    }
                }
                //T
                mv.visitVarInsn(DLOAD, tmp2);
                mv.visitVarInsn(DLOAD, tmp);
                mv.visitInsn(opcode);
                lvs.freeTmpLV(tmp2);
                lvs.freeTmpLV(tmp);
                break;
            case Opcodes.LSHL:
            case Opcodes.LUSHR:
            case Opcodes.LSHR:
                //T VV T V
                tmp = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(ISTORE, tmp);
                //T VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T T VV
                mv.visitVarInsn(ILOAD, tmp);
                lvs.freeTmpLV(tmp);
                // T T VV V
                mv.visitInsn(opcode);
                // T T VV
                mv.visitInsn(DUP2_X2);
                mv.visitInsn(POP2);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                // VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
            break;
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LADD:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
                //T VV T VV
                tmp = lvs.getTmpLV();
                mv.visitVarInsn(LSTORE, tmp);
                //T VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T T VV
                mv.visitVarInsn(LLOAD, tmp);
                lvs.freeTmpLV(tmp);
                // T T VV VV
                mv.visitInsn(opcode);
                // T T VV
                mv.visitInsn(DUP2_X2);
                mv.visitInsn(POP2);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                // VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                break;
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                mv.visitInsn(opcode);
                break;
            case Opcodes.LCMP:
                //T VV T VV
                tmp = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(LSTORE, tmp);
                //T VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T T VV
                mv.visitVarInsn(LLOAD, tmp);
                lvs.freeTmpLV(tmp);
                // T T VV VV
                mv.visitInsn(opcode);
                // T T V
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                // V T
                mv.visitInsn(SWAP);
            break;
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                //T VV T VV
                tmp = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(DSTORE, tmp);
                //T VV T
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                //T T VV
                mv.visitVarInsn(DLOAD, tmp);
                lvs.freeTmpLV(tmp);
                // T T VV VV
                mv.visitInsn(opcode);
                // T T V
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                // V T
                mv.visitInsn(SWAP);
            break;
            // VV T
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                //T V T V
                tmp = lvs.getTmpLV();
                mv.visitInsn(TaintUtils.IS_TMP_STORE);
                mv.visitVarInsn(FSTORE, tmp);
                //T V T
                mv.visitInsn(SWAP);
                //T T V
                mv.visitVarInsn(FLOAD, tmp);
                lvs.freeTmpLV(tmp);
                //T T V V
                mv.visitInsn(opcode);
                //T T V
                mv.visitInsn(DUP_X2);
                mv.visitInsn(POP);
                if(Configuration.WITHOUT_PROPAGATION) {
                    mv.visitInsn(POP2);
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                } else {
                    COMBINE_TAGS.delegateVisit(mv);
                }
                mv.visitInsn(SWAP);
            break;
            case Opcodes.ARRAYLENGTH:
                Type arrType = TaintAdapter.getTypeForStackType(adapter.analyzer.stack.get(adapter.analyzer.stack.size() - 1));
                boolean loaded = false;
                if(arrType.getSort() != Type.ARRAY) {
                    //TA A
                    loaded = true;
                    if(adapter.topCarriesTaint()) {
                        mv.visitInsn(SWAP);
                        if (Configuration.ARRAY_LENGTH_TRACKING) {
                            mv.visitMethodInsn(INVOKEVIRTUAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "getLengthTaint", "()" + Configuration.TAINT_TAG_DESC, false);
                        } else {
                            mv.visitInsn(POP);
                            NEW_EMPTY_TAINT.delegateVisit(mv);
                        }
                        mv.visitInsn(SWAP);
                    } else{
                        NEW_EMPTY_TAINT.delegateVisit(mv);
                        mv.visitInsn(SWAP);
                    }
                    //A
                }
                if(!loaded) {
                    mv.visitInsn(DUP);
                    if((Configuration.IMPLICIT_TRACKING || Configuration.ARRAY_LENGTH_TRACKING)) {
                        GET_TAINT_OBJECT.delegateVisit(mv);
                    } else {
                        mv.visitInsn(POP);
                        NEW_EMPTY_TAINT.delegateVisit(mv);
                    }
                    mv.visitInsn(SWAP);
                }
                if (arrType.getSort() != Type.ARRAY) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, arrType.getInternalName(), "getLength", "()I", false);
                } else {
                    mv.visitInsn(opcode);
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
        mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    @Override
    public void tableSwitch(int min, int max, Label defaultLabel, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV) {
        mv.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void propagateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv) {
        int idx = 0;
        Type[] argTypes = Type.getArgumentTypes(newDesc);
        if((acc & Opcodes.ACC_STATIC) == 0) {
            idx++;
        }
        for (Type t : argTypes) {
            if (TaintUtils.isShadowedType(t)) {
                mv.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
                COMBINE_TAGS.delegateVisit(mv);
                idx++;
            }
            idx += t.getSize();
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
