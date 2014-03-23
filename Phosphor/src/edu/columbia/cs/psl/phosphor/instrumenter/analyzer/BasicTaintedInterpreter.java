package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import java.util.HashSet;
import java.util.List;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FieldInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.IntInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LdcInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.TypeInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.AnalyzerException;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.Interpreter;

public class BasicTaintedInterpreter extends Interpreter<BasicTaintedValue> implements Opcodes{
	static SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance(Instrumenter.callgraph);
	 public BasicTaintedInterpreter() {
		super(Opcodes.ASM5);
	}

	@Override
    public BasicTaintedValue newValue(final Type type) {
        if (type == null) {
            return BasicTaintedValue.UNINITIALIZED_VALUE();
        }
        switch (type.getSort()) {
        case Type.VOID:
            return null;
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            return BasicTaintedValue.INT_VALUE();
        case Type.FLOAT:
            return BasicTaintedValue.FLOAT_VALUE();
        case Type.LONG:
            return BasicTaintedValue.LONG_VALUE();
        case Type.DOUBLE:
            return BasicTaintedValue.DOUBLE_VALUE();
        case Type.ARRAY:
        case Type.OBJECT:
            return BasicTaintedValue.REFERENCE_VALUE();
        default:
            throw new Error("Internal error");
        }
    }

	 public BasicTaintedValue newTaintedValue(final Type type) {
	        if (type == null) {
	            return BasicTaintedValue.TAINTED_UNINITIALIZED_VALUE();
	        }
	        switch (type.getSort()) {
	        case Type.VOID:
	            return null;
	        case Type.BOOLEAN:
	        case Type.CHAR:
	        case Type.BYTE:
	        case Type.SHORT:
	        case Type.INT:
	            return BasicTaintedValue.TAINTED_INT_VALUE();
	        case Type.FLOAT:
	            return BasicTaintedValue.TAINTED_FLOAT_VALUE();
	        case Type.LONG:
	            return BasicTaintedValue.TAINTED_LONG_VALUE();
	        case Type.DOUBLE:
	            return BasicTaintedValue.TAINTED_DOUBLE_VALUE();
	        case Type.ARRAY:
	        case Type.OBJECT:
	            return BasicTaintedValue.TAINTED_REFERENCE_VALUE();
	        default:
	            throw new Error("Internal error");
	        }
	    }
	 
	    @Override
	    public BasicTaintedValue newOperation(final AbstractInsnNode insn)
	            throws AnalyzerException {
	        switch (insn.getOpcode()) {
	        case ACONST_NULL:
	            return newValue(Type.getObjectType("null"));
	        case ICONST_M1:
	        case ICONST_0:
	        case ICONST_1:
	        case ICONST_2:
	        case ICONST_3:
	        case ICONST_4:
	        case ICONST_5:
	            return BasicTaintedValue.INT_VALUE();
	        case LCONST_0:
	        case LCONST_1:
	            return BasicTaintedValue.LONG_VALUE();
	        case FCONST_0:
	        case FCONST_1:
	        case FCONST_2:
	            return BasicTaintedValue.FLOAT_VALUE();
	        case DCONST_0:
	        case DCONST_1:
	            return BasicTaintedValue.DOUBLE_VALUE();
	        case BIPUSH:
	        case SIPUSH:
	            return BasicTaintedValue.INT_VALUE();
	        case LDC:
	            Object cst = ((LdcInsnNode) insn).cst;
	            if (cst instanceof Integer) {
	                return BasicTaintedValue.INT_VALUE();
	            } else if (cst instanceof Float) {
	                return BasicTaintedValue.FLOAT_VALUE();
	            } else if (cst instanceof Long) {
	                return BasicTaintedValue.LONG_VALUE();
	            } else if (cst instanceof Double) {
	                return BasicTaintedValue.DOUBLE_VALUE();
	            } else if (cst instanceof String) {
	                return newValue(Type.getObjectType("java/lang/String"));
	            } else if (cst instanceof Type) {
	                int sort = ((Type) cst).getSort();
	                if (sort == Type.OBJECT || sort == Type.ARRAY) {
	                    return newValue(Type.getObjectType("java/lang/Class"));
	                } else if (sort == Type.METHOD) {
	                    return newValue(Type
	                            .getObjectType("java/lang/invoke/MethodType"));
	                } else {
	                    throw new IllegalArgumentException("Illegal LDC constant "
	                            + cst);
	                }
	            } else if (cst instanceof Handle) {
	                return newValue(Type
	                        .getObjectType("java/lang/invoke/MethodHandle"));
	            } else {
	                throw new IllegalArgumentException("Illegal LDC constant "
	                        + cst);
	            }
	        case JSR:
	            return BasicTaintedValue.RETURNADDRESS_VALUE();
	        case GETSTATIC:
	        	if(taintedFields.contains(((FieldInsnNode)insn).owner+"."+((FieldInsnNode)insn).name))
	        		return newTaintedValue(Type.getType(((FieldInsnNode) insn).desc));
        		return newValue(Type.getType(((FieldInsnNode) insn).desc));
	        case NEW:
	            return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
	        default:
	            throw new Error("Internal error.");
	        }
	    }
	    HashSet<String> taintedFields = new HashSet<String>();
	    @Override
	    public BasicTaintedValue copyOperation(final AbstractInsnNode insn,
	            final BasicTaintedValue value) throws AnalyzerException {
	    	if(value.isReference())
	    		return value;
	    	return new BasicTaintedValue(value.getType(), value.isTainted);
	    }

	    @Override
	    public BasicTaintedValue unaryOperation(final AbstractInsnNode insn,
	            final BasicTaintedValue value) throws AnalyzerException {
	        switch (insn.getOpcode()) {
	        case INEG:
	        case IINC:
	        case L2I:
	        case F2I:
	        case D2I:
	        case I2B:
	        case I2C:
	        case I2S:
	        	if(value.isTainted)
	        		return BasicTaintedValue.TAINTED_INT_VALUE();
	        	return BasicTaintedValue.INT_VALUE();
	        case FNEG:
	        case I2F:
	        case L2F:
	        case D2F:
	        	if(value.isTainted)
	        		return BasicTaintedValue.TAINTED_FLOAT_VALUE();
	            return BasicTaintedValue.FLOAT_VALUE();
	        case LNEG:
	        case I2L:
	        case F2L:
	        case D2L:
	        	if(value.isTainted)
	        		return BasicTaintedValue.TAINTED_LONG_VALUE();
	            return BasicTaintedValue.LONG_VALUE();
	        case DNEG:
	        case I2D:
	        case L2D:
	        case F2D:
	        	if(value.isTainted)
	        		return BasicTaintedValue.TAINTED_DOUBLE_VALUE();
	            return BasicTaintedValue.DOUBLE_VALUE();
	        case IFEQ:
	        case IFNE:
	        case IFLT:
	        case IFGE:
	        case IFGT:
	        case IFLE:
	        case TABLESWITCH:
	        case LOOKUPSWITCH:
	        case IRETURN:
	        case LRETURN:
	        case FRETURN:
	        case DRETURN:
	        case ARETURN:
	        	return null;
	        case PUTSTATIC:
	        	if(value.isTainted)
	        		taintedFields.add(((FieldInsnNode)insn).owner+"."+ ((FieldInsnNode)insn).name);
	            return null;
	        case GETFIELD:
	        	if(taintedFields.contains(((FieldInsnNode)insn).owner+"."+ ((FieldInsnNode)insn).name))
	        		return newValue(Type.getType(((FieldInsnNode) insn).desc));
	        	else
	        		return newTaintedValue(Type.getType(((FieldInsnNode) insn).desc));
	        case NEWARRAY:
	            switch (((IntInsnNode) insn).operand) {
	            case T_BOOLEAN:
	                return newValue(Type.getType("[Z"));
	            case T_CHAR:
	                return newValue(Type.getType("[C"));
	            case T_BYTE:
	                return newValue(Type.getType("[B"));
	            case T_SHORT:
	                return newValue(Type.getType("[S"));
	            case T_INT:
	                return newValue(Type.getType("[I"));
	            case T_FLOAT:
	                return newValue(Type.getType("[F"));
	            case T_DOUBLE:
	                return newValue(Type.getType("[D"));
	            case T_LONG:
	                return newValue(Type.getType("[J"));
	            default:
	                throw new AnalyzerException(insn, "Invalid array type");
	            }
	        case ANEWARRAY:
	            String desc = ((TypeInsnNode) insn).desc;
	            return newValue(Type.getType("[" + Type.getObjectType(desc)));
	        case ARRAYLENGTH:
	            return BasicTaintedValue.INT_VALUE();
	        case ATHROW:
	        	this.throwsTainted = this.throwsTainted || value.isTainted;
	            return null;
	        case CHECKCAST:
	            desc = ((TypeInsnNode) insn).desc;
	            return newValue(Type.getObjectType(desc));
	        case INSTANCEOF:
	            return BasicTaintedValue.INT_VALUE();
	        case MONITORENTER:
	        case MONITOREXIT:
	        case IFNULL:
	        case IFNONNULL:
	            return null;
	        default:
	            throw new Error("Internal error.");
	        }
	    }
	    boolean throwsTainted;
	    @Override
	    public BasicTaintedValue binaryOperation(final AbstractInsnNode insn,
	            final BasicTaintedValue value1, final BasicTaintedValue value2)
	            throws AnalyzerException {
	        switch (insn.getOpcode()) {
	        case IALOAD:
	        case BALOAD:
	        case CALOAD:
	        case SALOAD:
	        case IADD:
	        case ISUB:
	        case IMUL:
	        case IDIV:
	        case IREM:
	        case ISHL:
	        case ISHR:
	        case IUSHR:
	        case IAND:
	        case IOR:
	        case IXOR:
	        	if(value1.isTainted || value2.isTainted)
		            return BasicTaintedValue.TAINTED_INT_VALUE();
	            return BasicTaintedValue.INT_VALUE();
	        case FALOAD:
	        case FADD:
	        case FSUB:
	        case FMUL:
	        case FDIV:
	        case FREM:
	        	if(value1.isTainted || value2.isTainted)
		            return BasicTaintedValue.TAINTED_FLOAT_VALUE();
	            return BasicTaintedValue.FLOAT_VALUE();
	        case LALOAD:
	        case LADD:
	        case LSUB:
	        case LMUL:
	        case LDIV:
	        case LREM:
	        case LSHL:
	        case LSHR:
	        case LUSHR:
	        case LAND:
	        case LOR:
	        case LXOR:
	        	if(value1.isTainted || value2.isTainted)
		            return BasicTaintedValue.TAINTED_LONG_VALUE();
	            return BasicTaintedValue.LONG_VALUE();
	        case DALOAD:
	        case DADD:
	        case DSUB:
	        case DMUL:
	        case DDIV:
	        case DREM:
	        	if(value1.isTainted || value2.isTainted)
		            return new BasicTaintedValue(Type.DOUBLE_TYPE,true);
	            return new BasicTaintedValue(Type.DOUBLE_TYPE,false);
	        case AALOAD:
	        	if(value1.isTainted || value2.isTainted)
		            return BasicTaintedValue.TAINTED_REFERENCE_VALUE();
	            return BasicTaintedValue.REFERENCE_VALUE();
	        case LCMP:
	        case FCMPL:
	        case FCMPG:
	        case DCMPL:
	        case DCMPG:
	        	if(value1.isTainted || value2.isTainted)
		            return BasicTaintedValue.TAINTED_INT_VALUE();
	            return BasicTaintedValue.INT_VALUE();
	        case IF_ICMPEQ:
	        case IF_ICMPNE:
	        case IF_ICMPLT:
	        case IF_ICMPGE:
	        case IF_ICMPGT:
	        case IF_ICMPLE:
	        case IF_ACMPEQ:
	        case IF_ACMPNE:
	        case PUTFIELD:
	        	if(value1.isTainted || value2.isTainted)
	        		this.taintedFields.add((((FieldInsnNode)insn).owner+"."+(((FieldInsnNode)insn).name)));
	            return null;
	        default:
	            throw new Error("Internal error.");
	        }
	    }

	    @Override
	    public BasicTaintedValue ternaryOperation(final AbstractInsnNode insn,
	            final BasicTaintedValue value1, final BasicTaintedValue value2,
	            final BasicTaintedValue value3) throws AnalyzerException {
	    	if(value1.isTainted || value2.isTainted || value3.isTainted)
	    	{
	    		value1.isTainted = true;
	    		value2.isTainted = true;
	    		value3.isTainted = true;
	    	}
	        return null;
	    }

	    @Override
	    public BasicTaintedValue naryOperation(final AbstractInsnNode insn,
	            final List<? extends BasicTaintedValue> values) throws AnalyzerException {
	        int opcode = insn.getOpcode();
	        if (opcode == MULTIANEWARRAY) {
	            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
	        } else if (opcode == INVOKEDYNAMIC) {
	            return newValue(Type
	                    .getReturnType(((InvokeDynamicInsnNode) insn).desc));
	        } else {
	        	MethodInsnNode min = (MethodInsnNode) insn;
	        	boolean hasTaint = false;
	        	for(BasicTaintedValue v : values)
	        		hasTaint = hasTaint || v.isTainted;
	        	if(hasTaint || sourceSinkManager.isSource(min))
	        		return newTaintedValue(Type.getReturnType(((MethodInsnNode) insn).desc));
	        	else
	        		return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
	        }
	    }

	    boolean returnsTainted;
	    @Override
	    public void returnOperation(final AbstractInsnNode insn,
	            final BasicTaintedValue value, final BasicTaintedValue expected)
	            throws AnalyzerException {
	    	this.returnsTainted = this.returnsTainted || value.isTainted;
	    }

	    @Override
	    public BasicTaintedValue merge(final BasicTaintedValue v, final BasicTaintedValue w) {
	        if (!v.equals(w)) {
	        	if(v.isTainted || w.isTainted)
	        		return new BasicTaintedValue(null,true);
	            return new BasicTaintedValue(null,false);
	        }
	        return v;
	    }
}
