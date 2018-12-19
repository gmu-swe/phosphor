package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;
import java.util.Objects;

public class BasicArrayInterpreter extends BasicInterpreter{

	public class BasicThisFieldValue extends BasicValue{
		private Field field;

		public Field getField() {
			return field;
		}


		@Override
		public String toString() {
			return "BasicThisFieldValue{" +
					"field=" + field +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			BasicThisFieldValue that = (BasicThisFieldValue) o;
			return Objects.equals(field, that.field);
		}

		@Override
		public int hashCode() {

			return Objects.hash(super.hashCode(), field);
		}

		public BasicThisFieldValue(Type t, Field f){
			super(t);
			this.field =f;
		}
	}
	private boolean isStaticMethod;
	public BasicArrayInterpreter(boolean isStaticMethod){
		super(Opcodes.ASM5);
		this.isStaticMethod = isStaticMethod;
	}

	@Override
	public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.ACONST_NULL)
		{
			return BasicArrayValue.NULL_VALUE;
		}
		if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING){
			String t = null;
			if(insn.getOpcode() == Opcodes.NEW){
				t = ((TypeInsnNode)insn).desc;
			}
			if(t != null && (t.contains("Exception") || t.contains("Error"))){
				return new BasicValue(Type.getObjectType(t));
			}
		}
		if(insn.getOpcode() == Opcodes.GETSTATIC){
			FieldInsnNode fin = (FieldInsnNode) insn;
			return new BasicThisFieldValue(Type.getType((fin.desc)),new Field(true, fin.owner,fin.name,fin.desc));
		}
		return super.newOperation(insn);
	}

	public static final BasicValue THIS_VALUE = new BasicValue(Type.getType("Ljava/lang/Object;"));

	@Override
	public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		if(!isStaticMethod && insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode)insn).var == 0)
		{
			return THIS_VALUE;
		}
		return super.copyOperation(insn, value);
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.GETFIELD && value == THIS_VALUE){
			FieldInsnNode fin = (FieldInsnNode) insn;
			return new BasicThisFieldValue(Type.getType((fin.desc)),new Field(false, fin.owner,fin.name,fin.desc));
		}
		return super.unaryOperation(insn, value);
	}

	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		if(v == BasicValue.UNINITIALIZED_VALUE || w==BasicValue.UNINITIALIZED_VALUE)
			return BasicValue.UNINITIALIZED_VALUE;
		if((v instanceof BasicThisFieldValue && ! (w instanceof BasicThisFieldValue)) || (w instanceof BasicThisFieldValue && ! (v instanceof BasicThisFieldValue)))
		{
			if(v.getType().equals(w.getType())){
				if(v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY)
					return BasicValue.REFERENCE_VALUE;
				else
					return newValue(v.getType());
			}
			return BasicValue.UNINITIALIZED_VALUE;
		}
		else if(v instanceof BasicThisFieldValue && w instanceof  BasicThisFieldValue){
			if(v.equals(w))
				return v;
			return BasicValue.UNINITIALIZED_VALUE;
		}
		return super.merge(v, w);
	}

	@Override
	public BasicValue naryOperation(AbstractInsnNode insn, List values) throws AnalyzerException {
		String t = null;
		if(insn.getType() == AbstractInsnNode.METHOD_INSN){
			Type typ = Type.getReturnType(((MethodInsnNode)insn).desc);
			if(typ.getSort() == Type.OBJECT)
				t = typ.getInternalName();
		}
		if(t != null && (t.contains("Exception") || t.contains("Error"))){
			return new BasicValue(Type.getObjectType(t));
		}
		return super.naryOperation(insn, values);
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
		else if(Configuration.IMPLICIT_EXCEPTION_FLOW && (type.getDescriptor().contains("Error") || type.getDescriptor().contains("Exception")))
			return new BasicArrayValue(type);
		else
			return super.newValue(type);
	}
}
