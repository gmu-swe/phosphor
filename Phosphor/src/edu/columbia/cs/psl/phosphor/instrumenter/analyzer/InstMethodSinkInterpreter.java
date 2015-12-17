package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;

public class InstMethodSinkInterpreter extends BasicInterpreter {
	LinkedList<SinkableArrayValue> relevant;

	public InstMethodSinkInterpreter(LinkedList<SinkableArrayValue> relevantValues) {
		this.relevant = relevantValues;
	}

	@Override
	public BasicValue copyOperation(AbstractInsnNode insn, BasicValue _v) throws AnalyzerException {
		if (_v instanceof SinkableArrayValue) {
			SinkableArrayValue ret = new SinkableArrayValue(_v.getType());
			ret.addDep(((SinkableArrayValue) _v));
//			if (insn.getOpcode() == Opcodes.ALOAD || insn.getOpcode() == Opcodes.ASTORE || insns) {
				ret.src = insn;
//			} else
//				ret.src = ((SinkableArrayValue) _v).src;
			if (ret.src == null && insn.getType() == Opcodes.ALOAD)
				throw new NullPointerException();
			return ret;
		}
		return _v;
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		BasicValue v = super.unaryOperation(insn, value);
		if (v instanceof SinkableArrayValue)
			((SinkableArrayValue) v).src = insn;
		return v;
	}

	@Override
	public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
		if (insn.getOpcode() == Opcodes.AALOAD) {
			System.out.println("AALOAD " + value1.getType());
			Type t = Type.getType(value1.getType().getDescriptor().substring(1));
			if (TaintUtils.isPrimitiveArrayType(t)) {
				SinkableArrayValue ret = new SinkableArrayValue(t);
				ret.src = insn;
				return ret;
			}
		}
		return super.binaryOperation(insn, value1, value2);
	}

	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		if (v == BasicValue.UNINITIALIZED_VALUE && w == BasicValue.UNINITIALIZED_VALUE)
			return v;
		if (!(v instanceof SinkableArrayValue || w instanceof SinkableArrayValue))
			return super.merge(v, w);

		if (v.equals(w))
			return v;
		System.out.println("Merge " + v + w);

		if (v instanceof SinkableArrayValue && w instanceof SinkableArrayValue) {

			SinkableArrayValue sv = (SinkableArrayValue) v;
			SinkableArrayValue sw = (SinkableArrayValue) w;
			if ((v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) && (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")))
			{
				if ((sw.src != null && sv.deps != null && sw != null && sv.deps.contains(sw)) || (sw.src == null && sw.deps != null &&sv.deps != null && sv.deps.containsAll(sw.deps)))
					return v;
				else {
					sv.addDep(sw);
					return v;
				}
			}
			if (v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
				sw.addDep(sv);
				return w;
			}
			else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;"))
			{
				sv.addDep(sw);
				return v;
			}
			else if (TaintUtils.isPrimitiveArrayType(v.getType()) && TaintUtils.isPrimitiveArrayType(w.getType())) {
				if (v.getType().equals(w.getType())) {
					if (sv.flowsToInstMethodCall && !sw.flowsToInstMethodCall) {
						relevant.addAll(sw.tag());
						System.out.println("R1");
						return v;
					} else if (sw.flowsToInstMethodCall && !sv.flowsToInstMethodCall) {
						relevant.addAll(sv.tag());
						System.out.println("R2");
						return v;
					} else {
						//					if (sw.deps == null)
						//						return v;
						//					else if (sv.deps == null)
						//						return w;
						if (sv.getType().equals(sw.getType())){
//							if ((sw.src != null && sv.deps.contains(sw)) )//|| (sw.src == null && sw.deps != null && sv.deps.containsAll(sw.deps)))
//								return v;
//							else {
								sv.addDep(sw);
								return v;
							}
						//					System.out.println(System.identityHashCode(sv) + " and " + System.identityHashCode(sw));
						//					System.out.println("Merge "  + v +  w);
						//					System.out.println(sv.src +  " " +sw.src);
						//					System.out.print("V");
						//					for(SinkableArrayValue a : sw.deps)
						//						System.out.print(System.identityHashCode(a) + " " );
						//					System.out.println();
						//					System.out.print("w");
						//					for(SinkableArrayValue a : sv.deps)
						//						System.out.print(System.identityHashCode(a) + " " );
						//					System.out.println();
						SinkableArrayValue ret = new SinkableArrayValue(v.getType());
						ret.addDep(sv);
						ret.addDep(sw);
						//					System.out.println("Ret is " + System.identityHashCode(ret));
						//					System.out.print("r");
						//					for(SinkableArrayValue a : ret.deps)
						//						System.out.print(System.identityHashCode(a) + " " );
						//					System.out.println();
						return ret;
					}
				}
			}
		}
		//		if(v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()) && !((SinkableArrayValue)v).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)v).tag());
		//		}
		//		if(w instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(w.getType()) && !((SinkableArrayValue)w).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)w).tag());
		//		}
		return super.merge(v, w);
	}

	@Override
	public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		BasicValue ret = super.newOperation(insn);
		if (ret instanceof SinkableArrayValue)
			((SinkableArrayValue) ret).src = insn;
		return ret;
	}

	@Override
	public BasicValue newValue(Type type) {
		if (type == null) {
			return new SinkableArrayValue(null);
		}
		if (TaintUtils.isPrimitiveArrayType(type)) {
			SinkableArrayValue ret = new SinkableArrayValue(type);
			return ret;
		} else if (type.getSort() == Type.ARRAY && type.getElementType().getSort() != Type.OBJECT) {
			return new BasicValue(type);
		} else if (type.getDescriptor().equals("Lnull;"))
			return new SinkableArrayValue(null);
		else
			return super.newValue(type);
	}

	@Override
	public BasicValue naryOperation(AbstractInsnNode insn, List values) throws AnalyzerException {
		if (insn instanceof MethodInsnNode) {
			MethodInsnNode min = (MethodInsnNode) insn;

			Type retType = Type.getReturnType(min.desc);
			if (TaintUtils.isPrimitiveArrayType(retType)) {
				SinkableArrayValue ret = new SinkableArrayValue(retType);
				ret.src = insn;
				return ret;
			}
		}
		return super.naryOperation(insn, values);
	}

}
