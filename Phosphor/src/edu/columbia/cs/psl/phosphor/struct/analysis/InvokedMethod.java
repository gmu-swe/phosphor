package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;

import static org.objectweb.asm.Opcodes.*;

public class InvokedMethod extends ForceControlStoreAdvice {
	public String owner;
	public String desc;
	public int opcode;
	public ForceControlStoreAdvice receiver;
	public LinkedList<ForceControlStoreAdvice> args;
	boolean itf;
	boolean isStatic;
	int collectorIdx = -1;
	LinkedList<MethodNode> methodCollector;
	String name;

	public InvokedMethod(MethodInsnNode mn, ForceControlStoreAdvice receiver, LinkedList<ForceControlStoreAdvice> args, LinkedList<MethodNode> methodCollector) {
		this.isStatic = mn.getOpcode() == INVOKESTATIC;
		this.methodCollector = methodCollector;
		this.opcode = mn.getOpcode();
		this.itf = mn.itf;
		this.owner = mn.owner;
		this.name = mn.name;
		this.desc = mn.desc;
		this.receiver = receiver;
		this.args = args;
	}

	@Override
	protected void loadValueAndItsTaintOrNull(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className, String defaultType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void apply(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className) {

		if (name.startsWith("<")) //no constructor summaries, sorry?
			return;

		String summaryMethodName = name + TaintUtils.METHOD_SUFFIX_SUMMARY;
		String summaryMethodDesc = TaintUtils.remapMethodDesc(desc);
		if(Configuration.IMPLICIT_EXCEPTION_FLOW){
			//Also need to pass the current method's exception data to the wrapper, to emulate as if that exception were being thrown immediately under this branch
			Type[] args = Type.getArgumentTypes(summaryMethodDesc);
			Type returnType = Type.getReturnType(summaryMethodDesc);
			summaryMethodDesc = "(";
			for(Type t : args){
				summaryMethodDesc += t.getDescriptor();
			}
			summaryMethodDesc += Type.getDescriptor(ExceptionalTaintData.class);
			summaryMethodDesc+=')'+returnType.getDescriptor();
		}
		Label bail = new Label();
		FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
		fn.type = Opcodes.F_NEW;
		if (!isStatic) {
			if (receiver instanceof LVAccess || receiver instanceof Field) {
				if(receiver instanceof LVAccess && ! ((LVAccess)receiver).isValid(analyzer,lvs, owner))
					return;
				if (receiver instanceof Field)
					((Field) receiver).fixMethodCollector(methodCollector);
				receiver.loadValueAndItsTaintOrNull(mv, lvs, analyzer, className, "L" + owner + ";");
				mv.visitJumpInsn(IFNULL, bail);
				receiver.loadValueAndItsTaintOrNull(mv, lvs, analyzer, className, "L" + owner + ";");
			} else {

				mv.visitInsn(ACONST_NULL);
				mv.visitJumpInsn(IFNULL, bail);
				mv.visitInsn(ACONST_NULL);
			}
		}
		Type[] argTypes = Type.getArgumentTypes(desc);
		for (int i = 0; i < argTypes.length; i++) {
			Type argType = argTypes[i];
			boolean hasTaint = TaintUtils.getShadowTaintType(argType.getDescriptor()) != null;
			ForceControlStoreAdvice advice = args.get(i);

			if (advice instanceof LVAccess || advice instanceof Field) {
				if (advice instanceof Field)
					((Field) advice).fixMethodCollector(methodCollector);
				advice.loadValueAndItsTaintOrNull(mv, lvs, analyzer, className, argType.getDescriptor());
			} else {
				if (hasTaint)
					mv.visitInsn(ACONST_NULL);
				mv.visitInsn(TaintUtils.getNullOrZero(argType));
			}
		}
		//load ControlTaintTagStack
		mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
		if (Configuration.IMPLICIT_EXCEPTION_FLOW)
			mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterExceptionLV());
		int idx = this.collectorIdx;
		if (idx < 0)
			idx = methodCollector.size();
		String wrapperName = "$$$phosphorwrapMethod" + idx + "$" + summaryMethodName;
		String wrapperDesc = summaryMethodDesc;
		boolean isInvokeSpecial = opcode == INVOKESPECIAL;

		if (opcode != INVOKESTATIC && !isInvokeSpecial) //everything becomes static except for INVOKESPECIAL
			wrapperDesc = "(" + Type.getObjectType(owner).getDescriptor() + wrapperDesc.substring(1);
		Type returnType = Type.getReturnType(summaryMethodDesc);
		if (this.collectorIdx < 0) {
			//need to generate the wrappe rmethod
			MethodNode wrapper = new MethodNode(isInvokeSpecial ? ACC_PRIVATE : ACC_STATIC, wrapperName, wrapperDesc, null, null);
			wrapper.visitCode();
			Label start = new Label();
			Label end = new Label();
			Label handler = new Label();
			wrapper.visitTryCatchBlock(start, end, handler, null);

			wrapper.visitLabel(start);
			int argIdx = 0;
			if (isInvokeSpecial) {
				wrapper.visitVarInsn(ALOAD, 0);
				argIdx++;
			}
			for (Type t : Type.getArgumentTypes(wrapperDesc)) {
				wrapper.visitVarInsn(t.getOpcode(ILOAD), argIdx);
				argIdx += t.getSize();
			}
			wrapper.visitMethodInsn(opcode, owner, summaryMethodName, summaryMethodDesc, itf);
			wrapper.visitInsn(returnType.getOpcode(IRETURN));
			wrapper.visitLabel(end);
			wrapper.visitLabel(handler);
			wrapper.visitFrame(F_NEW, 0, new Object[0], 1, new Object[]{"java/lang/Throwable"});
			wrapper.visitInsn(POP);
			if (returnType.getSort() != Type.VOID)
				wrapper.visitInsn(TaintUtils.getNullOrZero(returnType));
			wrapper.visitInsn(returnType.getOpcode(IRETURN));
			wrapper.visitMaxs(0, 0);
			wrapper.visitEnd();
			this.collectorIdx = idx;
			methodCollector.add(wrapper);
		}

		mv.visitMethodInsn(isInvokeSpecial ? INVOKESPECIAL : INVOKESTATIC, className, wrapperName, wrapperDesc, false);
		if (returnType.getSort() != Type.VOID) {
			mv.visitInsn(POP);
		}
		mv.visitLabel(bail);
		fn.accept(mv);

	}

	@Override
	public String toString() {
		return "InvokedMethod{" +
				"owner='" + owner + '\'' +
				", isStatic=" + isStatic +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", receiver=" + receiver +
				", args=" + args +
				"} " + super.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		InvokedMethod that = (InvokedMethod) o;

		if (isStatic != that.isStatic) return false;
		if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
		if (receiver != null ? !receiver.equals(that.receiver) : that.receiver != null) return false;
		return args != null ? args.equals(that.args) : that.args == null;
	}

	@Override
	public int hashCode() {
		int result = owner != null ? owner.hashCode() : 0;
		result = 31 * result + (isStatic ? 1 : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (desc != null ? desc.hashCode() : 0);
		result = 31 * result + (receiver != null ? receiver.hashCode() : 0);
		result = 31 * result + (args != null ? args.hashCode() : 0);
		return result;
	}
}
