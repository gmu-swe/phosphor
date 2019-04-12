package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer;
import edu.columbia.cs.psl.phosphor.struct.analysis.Field;
import edu.columbia.cs.psl.phosphor.struct.analysis.ForceControlStoreAdvice;
import edu.columbia.cs.psl.phosphor.struct.analysis.LVAccess;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;
import java.util.Objects;

public class BasicArrayInterpreter extends BasicInterpreter{
	public abstract class OriginTrackedValue extends BasicValue{
		public OriginTrackedValue(Type type) {
			super(type);
		}
		public abstract ForceControlStoreAdvice toAdvice();

	}

	PrimitiveArrayAnalyzer an;
	int remapVar(int var){
		if(an != null)
			return an.methodArgReindexer.remap(var);
		return var;
	}
	public class BasicLVValue extends OriginTrackedValue {
		private int lv;
		private BasicValue wrapped;


		public int getLv() {
			return lv;
		}

		public BasicValue getWrapped() {
			return wrapped;
		}

		@Override
		public ForceControlStoreAdvice toAdvice() {
			return new LVAccess(remapVar(lv), (this.getType() == null ? "Ljava/lang/Object;" : this.getType().getDescriptor()));
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;

			BasicLVValue that = (BasicLVValue) o;

			if (lv != that.lv) return false;
			return wrapped != null ? wrapped.equals(that.wrapped) : that.wrapped == null;
		}

		@Override
		public String toString() {
			return "BasicLVValue{" +
					"lv=" + lv +
					"} " + super.toString();
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + lv;
			result = 31 * result + (wrapped != null ? wrapped.hashCode() : 0);
			return result;
		}

		public BasicLVValue(Type type, int lv, BasicValue wrapped) {
			super(type);
			this.lv = lv;
			this.wrapped = wrapped;
		}
	}
	public class BasicFieldValue extends OriginTrackedValue{
		private Field field;

		private BasicValue parent;

		public Field getField() {
			return field;
		}


		@Override
		public ForceControlStoreAdvice toAdvice() {
			return field;
		}

		@Override
		public String toString() {
			return "BasicFieldValue{" +
					"field=" + field +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			BasicFieldValue that = (BasicFieldValue) o;
			return Objects.equals(field, that.field) && Objects.equals(parent, that.parent);
		}

		@Override
		public int hashCode() {

			return Objects.hash(super.hashCode(), field, parent);
		}

		public BasicFieldValue(Type t, Field f, BasicValue parent){
			super(t);
			this.field =f;
			this.parent = parent;
		}
	}
	private boolean isStaticMethod;
	public BasicArrayInterpreter(boolean isStaticMethod, PrimitiveArrayAnalyzer an){
		super(Opcodes.ASM5);
		this.isStaticMethod = isStaticMethod;
		this.an = an;
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
			return new BasicFieldValue(Type.getType((fin.desc)),new Field(fin, null, null),null);
		}
		return super.newOperation(insn);
	}

	public static final BasicValue THIS_VALUE = new BasicValue(Type.getType("Ljava/lang/Object;"));

	@Override
	public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		BasicValue v = super.copyOperation(insn, value);
		switch(insn.getOpcode()){
			case Opcodes.ILOAD:
			case Opcodes.ALOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
			case Opcodes.FLOAD:
				return new BasicLVValue(v.getType(),((VarInsnNode)insn).var,v);
		}
		return v;
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.GETFIELD){
			FieldInsnNode fin = (FieldInsnNode) insn;
			if(value instanceof BasicFieldValue){
				return new BasicFieldValue(Type.getType((fin.desc)), new Field(fin, ((BasicFieldValue) value).field, null), value);
			}else if( value instanceof BasicLVValue){
				return new BasicFieldValue(Type.getType((fin.desc)), new Field(fin, new LVAccess(remapVar(((BasicLVValue) value).lv),(value.getType() == null ? "Ljava/lang/Object;" : value.getType().getDescriptor())), null), value);
			}
		}
		return super.unaryOperation(insn, value);
	}

	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		if(v.equals(w))
			return v;
		if(v instanceof BasicLVValue)
			v = ((BasicLVValue) v).wrapped;
		if(w instanceof BasicLVValue)
			w = ((BasicLVValue) w).wrapped;
		if(v == BasicValue.UNINITIALIZED_VALUE || w==BasicValue.UNINITIALIZED_VALUE)
			return BasicValue.UNINITIALIZED_VALUE;
		if((v instanceof BasicFieldValue && ! (w instanceof BasicFieldValue)) || (w instanceof BasicFieldValue && ! (v instanceof BasicFieldValue)))
		{
			if(v.getType().equals(w.getType())){
				if(v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY) {
					return BasicValue.REFERENCE_VALUE;
				}
				else
					return newValue(v.getType());
			}
			return BasicValue.UNINITIALIZED_VALUE;
		}
		else if(v instanceof BasicFieldValue && w instanceof BasicFieldValue){
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
			if(value1 instanceof OriginTrackedValue)
				return BasicValue.REFERENCE_VALUE;
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
