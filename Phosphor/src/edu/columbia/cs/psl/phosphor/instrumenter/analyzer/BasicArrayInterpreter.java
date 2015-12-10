package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class BasicArrayInterpreter extends BasicInterpreter{
	
	@Override
	public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.ACONST_NULL)
		{
			return BasicArrayValue.NULL_VALUE;
		}
		return super.newOperation(insn);
	}
	
	@Override
	public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.AALOAD)
		{
			return value1;
		}
		else
			return super.binaryOperation(insn, value1, value2);
	}
	@Override
	public BasicValue newValue(Type type) {
		if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
		if(type.getSort() == Type.ARRAY)
		{
			if(type.getDimensions() > 1)
			{
				return new BasicArrayValue(type);
			}
			else
				switch(type.getElementType().getSort())
				{
				case Type.BOOLEAN:
					return BasicArrayValue.BOOLEAN_ARRAY;
				case Type.BYTE:
					return BasicArrayValue.BYTE_ARRAY;
				case Type.CHAR:
					return BasicArrayValue.CHAR_ARRAY;
				case Type.DOUBLE:
					return BasicArrayValue.DOUBLE_ARRAY;
				case Type.FLOAT:
					return BasicArrayValue.FLOAT_ARRAY;
				case Type.INT:
					return BasicArrayValue.INT_ARRAY;
				case Type.LONG:
					return BasicArrayValue.LONG_ARRAY;
				case Type.OBJECT:
					return BasicArrayValue.REFERENCE_VALUE;
				case Type.SHORT:
					return BasicArrayValue.SHORT_ARRAY;
					default:
						throw new IllegalArgumentException();
				}
		}
		else if(type.equals("Lnull;"))
			return BasicArrayValue.NULL_VALUE;
		else
			return super.newValue(type);
	}
}
