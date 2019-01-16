package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public class PFrame extends Frame {

	public PFrame(Frame src) {
		super(src);
	}

	public PFrame(final int nLocals, final int nStack) {
		super(nLocals, nStack);
	}
	public boolean isChangePoint;
	public HashSet<Integer> upcastLocals;
	public HashSet<Integer> upcastStack;
	public PFrame(final int nLocals, final int nStack, int nArgs)
	{
		super(nLocals,nStack);
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
		switch(insn.getOpcode())
		{
		case Opcodes.DUP:
			Value value1 = pop();
            if (value1.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP");
            }
            if(value1 instanceof SinkableArrayValue)
            {
                Value v = interpreter.copyOperation(insn, value1);
                push(v);
                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
            }
            else
			{
				push(value1);
				Value v = interpreter.copyOperation(insn, value1);
				push(v);
            }
			break;
		case Opcodes.DUP2:
			value1 = pop();
			if (value1.getSize() == 1) {
				Value value2 = pop();
				if (value1 instanceof SinkableArrayValue || value2 instanceof SinkableArrayValue) {
					if(value1 instanceof SinkableArrayValue && value2 instanceof SinkableArrayValue)
					{
		                Value v = interpreter.copyOperation(insn, value2);
		                push(v);
		                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
		                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value2, (SinkableArrayValue) v));
		                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
					}
					else if(value1 instanceof SinkableArrayValue)
					{
		                push(interpreter.copyOperation(insn, value2));
		                Value v = ((InstMethodSinkInterpreter)interpreter).copyOperationIgnoreOld(insn, (BasicValue) value1);
	                    push(v);
		                push(value2);
		                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));

//						System.out.println(value1 + " -> " + value2);
//						throw new UnsupportedOperationException();
					}
					else
					{
						throw new UnsupportedOperationException();
					}
				} else {
					push(value1);
					push(value2);
					super.execute(insn, interpreter);
				}
			} else {
				if(value1 instanceof SinkableArrayValue)
	            {
	                Value v = interpreter.copyOperation(insn, value1);
	                push(v);
	                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
	            }
	            else
				{
					push(value1);
					Value v = interpreter.copyOperation(insn, value1);
					push(v);
	            }
				break;
			}
			break;
		case Opcodes.DUP_X1:
			value1 = pop();
            if (value1.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP");
            }
            Value value2 = pop();
            if (value2.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP");
            }
            if(value1 instanceof SinkableArrayValue)
            {
                Value v = interpreter.copyOperation(insn, value1);
                push(v);
                push(value2);
                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
            }
            else
			{
				push(value1);
				push(value2);
				Value v = interpreter.copyOperation(insn, value1);
				push(v);
            }
            break;
		case Opcodes.DUP_X2:
			value1 = pop();
            if (value1.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP");
            }
            value2 = pop();
            Value value3 = null;
            if(value2.getSize() == 1)
            	value3 = pop();
            if(value1 instanceof SinkableArrayValue)
            {
                Value v = interpreter.copyOperation(insn, value1);
                push(v);
                if(value3 != null)
                	push(value3);
                push(value2);
                push(((InstMethodSinkInterpreter)interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
            }
            else
			{
				push(value1);
                if(value3 != null)
                	push(value3);
				push(value2);
				Value v = interpreter.copyOperation(insn, value1);
				push(v);
			}
            break;
		case Opcodes.DUP2_X1:
			value1 = pop();
			value2 = null;
			if (value1.getSize() == 1) {
				if (value1 instanceof SinkableArrayValue)
				{
					value2 = pop();
					if(value2 instanceof SinkableArrayValue)
					{
						throw new IllegalStateException();
					} else {
						Value v = interpreter.copyOperation(insn, value1);
						value3 = pop();
						push(value2);
						push(v);
						push(value3);
						push(value2);
						push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
					}
				} else {
					value2 = pop();
					if (value2 instanceof SinkableArrayValue)
						throw new UnsupportedOperationException();
					push(value1);
					push(value2);
					super.execute(insn, interpreter);
				}
			} else {
				//value2 is null
				value3 = pop();
				if (value1 instanceof SinkableArrayValue) {
					// if(value2 != null)
					Value v = interpreter.copyOperation(insn, value1);
					push(v);
					push(value3);
					push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
				} else {
					if (value3 != null && value3 instanceof SinkableArrayValue)
						throw new UnsupportedOperationException();
					push(value1);
					push(value3);
					Value v = interpreter.copyOperation(insn, value1);
					push(v);
				}
			}
			break;
		case Opcodes.DUP2_X2:
			value1 = pop();
			value2 = null;
			if (value1.getSize() == 1) {
				if (value1 instanceof SinkableArrayValue)
					throw new UnsupportedOperationException();
				value2 = pop();
				if (value2 instanceof SinkableArrayValue)
					throw new UnsupportedOperationException();
				push(value1);
				push(value2);
				super.execute(insn, interpreter);
			} else {
				value3 = pop();
				if(value3.getSize() == 1)
				{
					Value value4 = pop();
					if(value1 instanceof SinkableArrayValue)
					{
						Value v = interpreter.copyOperation(insn, value1);
						push(v);
						push(value4);
						push(value3);
						push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
					}
					else
						throw new UnsupportedOperationException();
				} else {
					// Two words over 2 words
					if (value1 instanceof SinkableArrayValue) {
						// if(value2 != null)
						Value v = interpreter.copyOperation(insn, value1);
						push(v);
						push(value3);
						push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
					} else {
						if (value3 != null && value3 instanceof SinkableArrayValue)
							throw new UnsupportedOperationException();
						push(value1);
						push(value3);
						Value v = interpreter.copyOperation(insn, value1);
						push(v);
					}
				}
			}
			break;
        case Opcodes.ACONST_NULL:
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
        case Opcodes.BIPUSH:
        case Opcodes.SIPUSH:
        case Opcodes.LDC:
        	Value v = interpreter.newOperation(insn);
        	if(v instanceof SinkableArrayValue)
        		((SinkableArrayValue) v).isConstant = true;
            push(v);
            break;
		default:
			super.execute(insn, interpreter);
		}

	}
}
