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

public class InstGetFieldSourceInterpreter extends BasicInterpreter {

	public InstGetFieldSourceInterpreter() {
	}

	@Override
	public BasicValue copyOperation(AbstractInsnNode insn, BasicValue _v) throws AnalyzerException {
//		if(insn instanceof VarInsnNode)
//		System.out.println(Printer.OPCODES[insn.getOpcode()] +((VarInsnNode)insn).var+ " " + _v);
		if (_v instanceof SinkableArrayValue) {
			SinkableArrayValue ret = new SinkableArrayValue(_v.getType());
			ret.addDep(((SinkableArrayValue) _v));
			//			if (insn.getOpcode() == Opcodes.ALOAD || insn.getOpcode() == Opcodes.ASTORE || insns) {
			ret.src = insn;
			//			} else
			//				ret.src = ((SinkableArrayValue) _v).src;
			if (ret.src == null && insn.getType() == Opcodes.ALOAD)
				throw new NullPointerException();
			//			((SinkableArrayValue)ret).srcVal=(SinkableArrayValue) _v;
			return ret;
		}
		return super.copyOperation(insn, _v);
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		if(insn.getOpcode() == Opcodes.CHECKCAST)
		{
			//are we checkcasting from a prim array to a prim array?
			if(value instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(value.getType()))
				return value;
		}
		BasicValue v = super.unaryOperation(insn, value);
		if(insn.getOpcode() == Opcodes.GETFIELD && TaintUtils.isPrimitiveArrayType(Type.getType(((FieldInsnNode) insn).desc)))
			v = new SinkableArrayValue(Type.getType(((FieldInsnNode) insn).desc));
		if (v instanceof SinkableArrayValue)
			((SinkableArrayValue) v).src = insn;
		return v;
	}

	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		if (v == BasicValue.UNINITIALIZED_VALUE && w == BasicValue.UNINITIALIZED_VALUE)
		{
			return v;
		}
		if (!(v instanceof SinkableArrayValue || w instanceof SinkableArrayValue))
			return super.merge(v, w);
//				System.out.println("Merge " + v + w);

		if (v.equals(w))
		{
//			System.out.println("EQ");
			return v;
		}

		if (v instanceof SinkableArrayValue && w instanceof SinkableArrayValue) {
//			System.out.println("Both sinkable");
			SinkableArrayValue sv = (SinkableArrayValue) v;
			SinkableArrayValue sw = (SinkableArrayValue) w;
			if ((v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) && (w.getType() == null || w.getType().getDescriptor().equals("Lnull;"))) {
				if ((sw.src != null && sv.deps != null && sw != null && sv.deps.contains(sw)) || (sw.src == null && sw.deps != null && sv.deps != null && sv.deps.containsAll(sw.deps)))
					return v;
				else {
					sv.addDep(sw);
					return v;
				}
			}
			if (v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
				sw.addDep(sv);
				return w;
			} else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
				sv.addDep(sw);
				return v;
			} else if (TaintUtils.isPrimitiveArrayType(v.getType()) && TaintUtils.isPrimitiveArrayType(w.getType())) {
				if (v.getType().equals(w.getType())) {
					if (sv.flowsToInstMethodCall && !sw.flowsToInstMethodCall) {
//						relevant.addAll(sw.tag());
//												System.out.println("R1");
						return v;
					} else if (sw.flowsToInstMethodCall && !sv.flowsToInstMethodCall) {
//						relevant.addAll(sv.tag());
						//						System.out.println("R2");
						return v;
					} else {
						sv.addDep(sw);
						return v;
					}
				}
			}
		}
		if (v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
//			System.out.println("V null");
			return w;
		} else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
//			System.out.println("W null");
			return v;
		}
		//		if(v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()) && !((SinkableArrayValue)v).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)v).tag());
		//		}
		//		if(w instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(w.getType()) && !((SinkableArrayValue)w).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)w).tag());
		//		}
		if(v.getType().getDescriptor().equals("Ljava/lang/Object;"))
			return v;
		BasicValue r = new SinkableArrayValue(Type.getType(Object.class));
//		System.out.println("Super merge");
//		BasicValue r = super.merge(v, w);
//		System.out.println("Ret " + r);
		return r;
	}

	@Override
	public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		//		System.out.println(Printer.OPCODES[insn.getOpcode()]);
		BasicValue ret = super.newOperation(insn);
		if(insn.getOpcode() == Opcodes.GETSTATIC && TaintUtils.isPrimitiveArrayType(Type.getType(((FieldInsnNode) insn).desc)))
			ret = new SinkableArrayValue(Type.getType(((FieldInsnNode) insn).desc));
		if (ret instanceof SinkableArrayValue)
			((SinkableArrayValue) ret).src = insn;
		return ret;
	}
}
