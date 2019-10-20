package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;

public class PrimitiveBoxingFixer extends TaintAdapter implements Opcodes {
    boolean followedByFrame = false;

    public PrimitiveBoxingFixer(int access, String className, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
        super(access, className, name, desc, signature, exceptions, mv, analyzer);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        if(followedByFrame) {
            followedByFrame = false;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(opcode == TaintUtils.FOLLOWED_BY_FRAME) {
            followedByFrame = true;
        } else if(followedByFrame) {
            followedByFrame = false;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        int nArgs = Type.getArgumentTypes(desc).length;
        boolean argIsStr = false;
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.OBJECT && t.getDescriptor().equals("Ljava/lang/String;")) {
                argIsStr = true;
            }
        }
        //Get an extra copy of the taint
        if(Configuration.WITH_ENUM_BY_VAL && opcode == INVOKESTATIC && owner.equals(Type.getInternalName(Enum.class))) {
            super.visitMethodInsn(opcode, Type.getInternalName(TaintUtils.class), "enumValueOf", desc, itfc);
            return;
        } else if((owner.equals(Type.getInternalName(Integer.class))
                //				|| owner.equals(Type.getInternalName(Byte.class))
                //				|| owner.equals(Type.getInternalName(Character.class))
                //				|| owner.equals(Type.getInternalName(Short.class)) ||  owner.equals(Type.getInternalName(Float.class))
                || owner.equals(Type.getInternalName(Long.class)) || owner.equals(Type.getInternalName(Double.class))) && name.equals("valueOf$$PHOSPHORTAGGED") && nArgs == (Configuration.IMPLICIT_TRACKING ? 3 : 2) && !argIsStr) {
            Type argT = Type.getArgumentTypes(desc)[1];
            int argSize = argT.getSize();
            if(argSize == 1) {
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitInsn(POP);
                }
//				System.out.println(analyzer.stack);
                //stack is currently T I <top>
                //we'll support (Integer) 1 == (Integer) 1 as long as there is no taint on it.
                super.visitInsn(SWAP);
                FrameNode fn = getCurrentFrameNode();
                super.visitInsn(DUP);
                Label makeNew = new Label();
                Label isOK = new Label();
                super.visitJumpInsn(IFNONNULL, makeNew);
                super.visitInsn(SWAP);
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
                super.visitJumpInsn(GOTO, isOK);
                super.visitLabel(makeNew);
                acceptFn(fn);
                super.visitInsn(SWAP);
                super.visitTypeInsn(Opcodes.NEW, owner);
                super.visitInsn(Opcodes.DUP);
                //T I N N
                super.visitInsn(Opcodes.DUP2_X2);
                super.visitInsn(Opcodes.POP2);
                //N N T I
                super.visitInsn(Opcodes.ACONST_NULL);
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    super.visitInsn(SWAP);
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "(" + Configuration.TAINT_TAG_DESC + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(ControlTaintTagStack.class) + Type.getDescriptor(TaintSentinel.class) + ")V", false);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "(" + Configuration.TAINT_TAG_DESC + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(TaintSentinel.class) + ")V", false);
                }
                super.visitInsn(DUP);
                super.visitInsn(DUP);
                super.visitFieldInsn(GETFIELD, owner, "value" + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
                super.visitFieldInsn(PUTFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
                FrameNode fn2 = getCurrentFrameNode();
                super.visitLabel(isOK);
                if(!followedByFrame) {
                    acceptFn(fn2);
                }
            } else if(argT.getSort() == Type.LONG) {
                super.visitMethodInsn(INVOKESTATIC, "edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator", "valueOf", desc, false);
                return;
            } else {
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitInsn(POP);
                }
                //T V V <top>
                super.visitInsn(DUP2_X1);
                super.visitInsn(POP2);
                //VV T
                FrameNode fn = getCurrentFrameNode();
                super.visitInsn(DUP);
                Label makeNew = new Label();
                Label isOK = new Label();
                super.visitJumpInsn(IFNONNULL, makeNew);
                //T VV
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                }

                super.visitMethodInsn(opcode, owner, name, desc, false);
                super.visitJumpInsn(GOTO, isOK);
                super.visitLabel(makeNew);
                acceptFn(fn);

                Type taintType = Type.getType(Configuration.TAINT_TAG_DESC);

                //VV T
                int tmp = lvs.getTmpLV(argT);
                int tmpT = lvs.getTmpLV(taintType);
                super.visitVarInsn(taintType.getOpcode(ISTORE), tmpT);
                super.visitVarInsn(argT.getOpcode(ISTORE), tmp);
                super.visitTypeInsn(Opcodes.NEW, owner);
                super.visitInsn(Opcodes.DUP);
                //T I N N

                super.visitVarInsn(taintType.getOpcode(ILOAD), tmpT);
                super.visitVarInsn(argT.getOpcode(ILOAD), tmp);

                super.visitInsn(Opcodes.ACONST_NULL);
                if(Configuration.IMPLICIT_TRACKING) {
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    super.visitInsn(SWAP);
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "(" + Configuration.TAINT_TAG_DESC + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(ControlTaintTagStack.class) + Type.getDescriptor(TaintSentinel.class) + ")V", false);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "(" + Configuration.TAINT_TAG_DESC + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(TaintSentinel.class) + ")V", false);
                }
                super.visitInsn(DUP);
                super.visitInsn(DUP);
                super.visitFieldInsn(GETFIELD, owner, "value" + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
                super.visitFieldInsn(PUTFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

                lvs.freeTmpLV(tmp);
                lvs.freeTmpLV(tmpT);
                FrameNode fn2 = getCurrentFrameNode();
                super.visitLabel(isOK);
                if(!followedByFrame) {
                    acceptFn(fn2);
                }
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itfc);
        }
        //TODO boxing the other way maybe too?
        //TODO handle situations with radix param
        if(followedByFrame) {
            followedByFrame = false;
        }
    }
}
