package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.HardcodedBypassStore;

public class TaintTagFieldCastMV extends MethodVisitor implements Opcodes {

	public TaintTagFieldCastMV(MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if ((opcode == Opcodes.GETFIELD
					|| opcode == Opcodes.GETSTATIC)
				&& !TaintAdapter.canRawTaintAccess(owner)
				&& name.endsWith(TaintUtils.TAINT_FIELD)
				&& (desc.equals(Configuration.TAINT_TAG_DESC)
					|| desc.equals(Configuration.TAINT_TAG_ARRAYDESC))) {

			if (desc.equals(Configuration.TAINT_TAG_DESC)) {
				super.visitFieldInsn(opcode, owner, name, "I");
				super.visitMethodInsn(Opcodes.INVOKESTATIC, 
						Type.getInternalName(HardcodedBypassStore.class),
						"get", "(I)Ljava/lang/Object;", false);
				super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
			} else {
				super.visitFieldInsn(opcode, owner, name, "[I");
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(HardcodedBypassStore.class),
						"get", "([I)[Ljava/lang/Object;", false);
				super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
			}
		} else if ((opcode == Opcodes.PUTFIELD
					|| opcode == Opcodes.PUTSTATIC)
				&& !TaintAdapter.canRawTaintAccess(owner)
				&& name.endsWith(TaintUtils.TAINT_FIELD)
				&& (desc.equals(Configuration.TAINT_TAG_DESC)
					|| desc.equals("[" + Configuration.TAINT_TAG_DESC))) {
			if (desc.equals(Configuration.TAINT_TAG_DESC)) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(HardcodedBypassStore.class),
						"add", "(Ljava/lang/Object;)I", false);
				super.visitFieldInsn(opcode, owner, name, "I");
			} else {
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(HardcodedBypassStore.class),
						"add", "([Ljava/lang/Object;)[I", false);
				super.visitFieldInsn(opcode, owner, name, "[I");
			}
		} else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
