package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class UninstrumentedCompatMV extends MethodVisitor {
	private NeverNullArgAnalyzerAdapter analyzer;
	public UninstrumentedCompatMV(MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
		super(Opcodes.ASM5, mv);
		this.analyzer = analyzer;
	}

	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if(opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF)
		{
			if(analyzer.stack.size() > 0 && "java/lang/Object".equals(analyzer.stack.get(analyzer.stack.size() - 1)) && type.startsWith("["))
			{
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;",false);
			}
		}
		super.visitTypeInsn(opcode, type);
	}
	
}
