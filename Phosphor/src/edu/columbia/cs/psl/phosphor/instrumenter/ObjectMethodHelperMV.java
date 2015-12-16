package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.runtime.ObjectMethodHelper;

public class ObjectMethodHelperMV extends MethodVisitor{
	public ObjectMethodHelperMV(MethodVisitor parent) {
		super(Opcodes.ASM5, parent);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if(opcode == Opcodes.INVOKEVIRTUAL && owner.equals("java/lang/Object") && (name.equals("hashCode") || name.equals("equals") || name.equals("toString")))
		{
			opcode = Opcodes.INVOKESTATIC;
			desc = "(Ljava/lang/Object;"+desc.substring(1);
			owner = Type.getInternalName(ObjectMethodHelper.class);
		}
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
}
