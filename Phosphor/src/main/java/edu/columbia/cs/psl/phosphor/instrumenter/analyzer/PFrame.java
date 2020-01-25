package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

public class PFrame extends Frame<BasicValue> {

    public PFrame(Frame<? extends BasicValue> src) {
        super(src);
    }

    public PFrame(final int nLocals, final int nStack) {
        super(nLocals, nStack);
    }

    public PFrame(final int nLocals, final int nStack, int nArgs) {
        super(nLocals, nStack);
    }

    @Override
    public void setLocal(int i, BasicValue v) throws IndexOutOfBoundsException {
        super.setLocal(i, v);
    }

    @Override
    public void execute(AbstractInsnNode insn, Interpreter<BasicValue> interpreter) throws AnalyzerException {
        if(insn.getOpcode() > 200) {
            return;
        } else if((insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof PhosphorInstructionInfo)) {
            return;
        }
        switch(insn.getOpcode()) {
            case Opcodes.DUP:
                BasicValue value1 = pop();
                if(value1.getSize() != 1) {
                    throw new AnalyzerException(insn, "Illegal use of DUP");
                }
                if(value1 instanceof SinkableArrayValue) {
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                    push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                } else {
                    push(value1);
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                }
                break;
            case Opcodes.DUP2:
                value1 = pop();
                if(value1.getSize() == 1) {
                    BasicValue value2 = pop();
                    if(value1 instanceof SinkableArrayValue || value2 instanceof SinkableArrayValue) {
                        if(value1 instanceof SinkableArrayValue && value2 instanceof SinkableArrayValue) {
                            BasicValue v = interpreter.copyOperation(insn, value2);
                            push(v);
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value2, (SinkableArrayValue) v));
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                        } else if(value1 instanceof SinkableArrayValue) {
                            push(interpreter.copyOperation(insn, value2));
                            BasicValue v = ((InstMethodSinkInterpreter) interpreter).copyOperationIgnoreOld(insn, value1);
                            push(v);
                            push(value2);
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    } else {
                        push(value1);
                        push(value2);
                        super.execute(insn, interpreter);
                    }
                } else {
                    if(value1 instanceof SinkableArrayValue) {
                        BasicValue v = interpreter.copyOperation(insn, value1);
                        push(v);
                        push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                    } else {
                        push(value1);
                        BasicValue v = interpreter.copyOperation(insn, value1);
                        push(v);
                    }
                    break;
                }
                break;
            case Opcodes.DUP_X1:
                value1 = pop();
                if(value1.getSize() != 1) {
                    throw new AnalyzerException(insn, "Illegal use of DUP");
                }
                BasicValue value2 = pop();
                if(value2.getSize() != 1) {
                    throw new AnalyzerException(insn, "Illegal use of DUP");
                }
                if(value1 instanceof SinkableArrayValue) {
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                    push(value2);
                    push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                } else {
                    push(value1);
                    push(value2);
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                }
                break;
            case Opcodes.DUP_X2:
                value1 = pop();
                if(value1.getSize() != 1) {
                    throw new AnalyzerException(insn, "Illegal use of DUP");
                }
                value2 = pop();
                BasicValue value3 = null;
                if(value2.getSize() == 1) {
                    value3 = pop();
                }
                if(value1 instanceof SinkableArrayValue) {
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                    if(value3 != null) {
                        push(value3);
                    }
                    push(value2);
                    push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                } else {
                    push(value1);
                    if(value3 != null) {
                        push(value3);
                    }
                    push(value2);
                    BasicValue v = interpreter.copyOperation(insn, value1);
                    push(v);
                }
                break;
            case Opcodes.DUP2_X1:
                value1 = pop();
                if(value1.getSize() == 1) {
                    if(value1 instanceof SinkableArrayValue) {
                        value2 = pop();
                        if(value2 instanceof SinkableArrayValue) {
                            throw new IllegalStateException();
                        } else {
                            BasicValue v = interpreter.copyOperation(insn, value1);
                            value3 = pop();
                            push(value2);
                            push(v);
                            push(value3);
                            push(value2);
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                        }
                    } else {
                        value2 = pop();
                        if(value2 instanceof SinkableArrayValue) {
                            throw new UnsupportedOperationException();
                        }
                        push(value1);
                        push(value2);
                        super.execute(insn, interpreter);
                    }
                } else {
                    //value2 is null
                    value3 = pop();
                    if(value1 instanceof SinkableArrayValue) {
                        BasicValue v = interpreter.copyOperation(insn, value1);
                        push(v);
                        push(value3);
                        push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                    } else {
                        if(value3 instanceof SinkableArrayValue) {
                            throw new UnsupportedOperationException();
                        }
                        push(value1);
                        push(value3);
                        BasicValue v = interpreter.copyOperation(insn, value1);
                        push(v);
                    }
                }
                break;
            case Opcodes.DUP2_X2:
                value1 = pop();
                if(value1.getSize() == 1) {
                    if(value1 instanceof SinkableArrayValue) {
                        throw new UnsupportedOperationException();
                    }
                    value2 = pop();
                    if(value2 instanceof SinkableArrayValue) {
                        throw new UnsupportedOperationException();
                    }
                    push(value1);
                    push(value2);
                    super.execute(insn, interpreter);
                } else {
                    value3 = pop();
                    if(value3.getSize() == 1) {
                        BasicValue value4 = pop();
                        if(value1 instanceof SinkableArrayValue) {
                            BasicValue v = interpreter.copyOperation(insn, value1);
                            push(v);
                            push(value4);
                            push(value3);
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    } else {
                        // Two words over 2 words
                        if(value1 instanceof SinkableArrayValue) {
                            BasicValue v = interpreter.copyOperation(insn, value1);
                            push(v);
                            push(value3);
                            push(((InstMethodSinkInterpreter) interpreter).copyOperation(insn, (SinkableArrayValue) value1, (SinkableArrayValue) v));
                        } else {
                            if(value3 instanceof SinkableArrayValue) {
                                throw new UnsupportedOperationException();
                            }
                            push(value1);
                            push(value3);
                            BasicValue v = interpreter.copyOperation(insn, value1);
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
                BasicValue v = interpreter.newOperation(insn);
                if(v instanceof SinkableArrayValue) {
                    ((SinkableArrayValue) v).isConstant = true;
                }
                push(v);
                break;
            default:
                super.execute(insn, interpreter);
        }

    }
}
