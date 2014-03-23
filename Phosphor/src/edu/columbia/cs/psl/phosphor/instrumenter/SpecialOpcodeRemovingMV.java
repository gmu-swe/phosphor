package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

	public SpecialOpcodeRemovingMV(MethodVisitor sup) {
		super(Opcodes.ASM5, sup);
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		Type descType = Type.getType(desc);
		if(descType.getSort() == Type.ARRAY && descType.getDimensions() > 1 && descType.getElementType().getSort() != Type.OBJECT)
		{
			//remap!
			desc = MultiDTaintedArray.getTypeForType(descType).getDescriptor();
		}
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}
	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case TaintUtils.RAW_INSN:
		case TaintUtils.NO_TAINT_STORE_INSN:
		case TaintUtils.IGNORE_EVERYTHING:
		case TaintUtils.DONT_LOAD_TAINT:
		case TaintUtils.GENERATETAINTANDSWAP:
		case TaintUtils.IS_TMP_STORE:
		case TaintUtils.ALWAYS_BOX_JUMP:
			break;
		default:
			super.visitInsn(opcode);
		}
	}
}
