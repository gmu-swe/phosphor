package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.HardcodedBypassStore;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TaintTagFieldCastMV extends MethodVisitor implements Opcodes {

    private String name;

    public TaintTagFieldCastMV(MethodVisitor mv, String name) {
        super(Configuration.ASM_VERSION, mv);
        this.name = name;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {

        boolean taintDesc = desc.equals(Configuration.TAINT_TAG_DESC) || desc.startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy");
        if((opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) && !TaintAdapter.canRawTaintAccess(owner) && name.endsWith(TaintUtils.TAINT_FIELD) && taintDesc) {
            super.visitFieldInsn(opcode, owner, name, "I");
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HardcodedBypassStore.class), "get", "(I)Ljava/lang/Object;", false);
            super.visitTypeInsn(CHECKCAST, Type.getType(desc).getInternalName());
        } else if((opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) && !TaintAdapter.canRawTaintAccess(owner) && name.endsWith(TaintUtils.TAINT_FIELD)
                && taintDesc) {
            if(this.name.equals("set" + TaintUtils.TAINT_FIELD) && desc.equals(Configuration.TAINT_TAG_DESC)) {
                if(opcode == PUTFIELD) {
                    super.visitInsn(POP2);
                } else {
                    super.visitInsn(POP);
                }
            } else {
                if(opcode == Opcodes.PUTSTATIC) {
                    super.visitInsn(ACONST_NULL);
                } else {
                    super.visitInsn(SWAP);
                    super.visitInsn(DUP_X1);
                }
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HardcodedBypassStore.class), "add", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
                if(name.equals("valuePHOSPHOR_TAG")) {
                    //Also set the object's taint tag
                    super.visitInsn(DUP2);
                    super.visitFieldInsn(opcode, owner, "PHOSPHOR_TAG", "I");
                }
                super.visitFieldInsn(opcode, owner, name, "I");
            }
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}
