package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import edu.columbia.cs.psl.phosphor.TaintUtils;

public class PFrame extends Frame {

	public PFrame(Frame src) {
		super(src);
	}

	public PFrame(final int nLocals, final int nStack) {
		super(nLocals, nStack);
	}
	int nArgs = -1;
	public boolean isChangePoint;
	public HashSet<Integer> upcastLocals;
	public HashSet<Integer> upcastStack;
	public PFrame(final int nLocals, final int nStack, final int nArgs)
	{
		super(nLocals,nStack);
		this.nArgs = nArgs;
	}
	
//	@Override
//	public boolean merge(Frame frame, Interpreter interpreter) throws AnalyzerException {
//		//Check for upcasting
//		HashSet<Integer> upcastLocals = new HashSet<Integer>();
//		HashSet<Integer> upcastStack = new HashSet<Integer>();
//		PFrame f = (PFrame) frame;
//		for(int i = 0; i < Math.min(frame.getLocals(), this.getLocals());i++)
//		{
//			Type t1 = ((BasicValue)frame.getLocal(i)).getType();
//			Type t2 = ((BasicValue)this.getLocal(i)).getType();
//			if(t1 == null || t2 == null)
//				continue;//TODO probably a problem when extending this to deal wiht all scenarios
//			if(!t1.equals(t2) && TaintUtils.isPrimitiveArrayType(t1) && TaintUtils.isPrimitiveArrayType(t2))
//			{
//				upcastLocals.add(i);
//			}
//		}
//		for(int i = 0; i < Math.min(frame.getStackSize(), this.getStackSize());i++)
//		{
//			Type t1 = ((BasicValue)frame.getStack(i)).getType();
//			Type t2 = ((BasicValue)this.getStack(i)).getType();
//			if(t1 == null || t2 == null)
//				continue; //TODO probably a problem when extending this to deal with all scenarios
//			if(!t1.equals(t2) && TaintUtils.isPrimitiveArrayType(t1) && TaintUtils.isPrimitiveArrayType(t2))
//			{
//				upcastStack.add(i);
//			}
//		}
//		if(upcastLocals.size() > 0 || upcastStack.size() > 0)
//		{
//			if(this.upcastLocals == null)
//			{
//				this.upcastLocals = new HashSet<Integer>();
//				this.upcastStack = new HashSet<Integer>();
//				this.isChangePoint = true;
//			}
//			this.upcastLocals.addAll(upcastLocals);
//			this.upcastStack.addAll(upcastStack);
//		}
//		return super.merge(frame, interpreter);
//
//	}

	@Override
	public void setLocal(int i, Value v) throws IndexOutOfBoundsException {
		super.setLocal(i,v);
	}
	@Override
	public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {
		if(insn.getOpcode() > 200)
			return;
		super.execute(insn, interpreter);
	}
}
