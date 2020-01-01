package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;
import org.objectweb.asm.util.Printer;

public class InstMethodSinkInterpreter extends BasicInterpreter {

    private Map<AbstractInsnNode, SinkableArrayValue> executed = new HashMap<>();

    public InstMethodSinkInterpreter(List<SinkableArrayValue> relevantValues, Map<AbstractInsnNode, Value> liveValues) {
        super(Configuration.ASM_VERSION);
    }

    @Override
    public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) throws AnalyzerException {
        if(value1 instanceof SinkableArrayValue && value3 instanceof SinkableArrayValue) {
            SinkableArrayValue arr = (SinkableArrayValue) value1;
            arr.addDep(((SinkableArrayValue) value3));
            if(((SinkableArrayValue) value1).isNewArray && !((SinkableArrayValue) value3).isConstant) {
                //Need to clear this value1 to no longer be a new array, and anything that was derived from it
                SinkableArrayValue v = (SinkableArrayValue) value1;
                while(v != null && v.isNewArray) {
                    v.isNewArray = false;
                    if(v.reverseDeps != null) {
                        for(SinkableArrayValue x : v.reverseDeps) {
                            if(x.isNewArray) {
                                x.isNewArray = false;
                                if(x.reverseDeps != null) {
                                    for(SinkableArrayValue y : x.reverseDeps) {
                                        y.isNewArray = false;
                                    }
                                }
                            }
                        }
                    }
                    v = v.copyOf;
                }
            }
        }
        return super.ternaryOperation(insn, value1, value2, value3);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue _v) throws AnalyzerException {
        if(_v instanceof SinkableArrayValue) {
            SinkableArrayValue ret = new SinkableArrayValue(_v.getType());
            ret.addDepCopy(((SinkableArrayValue) _v));
            ret.setSrc(insn);
            return ret;
        }
        SinkableArrayValue old = executed.put(insn, null);
        if (old != null) {
            old.disable();
        }
        return super.copyOperation(insn, _v);
    }

    public BasicValue copyOperationIgnoreOld(AbstractInsnNode insn, BasicValue _v) throws AnalyzerException {
        if(_v instanceof SinkableArrayValue) {
            SinkableArrayValue ret = new SinkableArrayValue(_v.getType());
            ret.addDepCopy(((SinkableArrayValue) _v));
            ret.setSrc(insn);
            return ret;
        }
        return super.copyOperation(insn, _v);
    }

    public BasicValue copyOperation(AbstractInsnNode insn, SinkableArrayValue underlying, SinkableArrayValue existing) throws AnalyzerException {
        BasicValue ret = copyOperationIgnoreOld(insn, underlying);
        if(ret instanceof SinkableArrayValue) {
            ((SinkableArrayValue) ret).masterDup = existing;
            existing.isBottomDup = true;
            existing.otherDups.add((SinkableArrayValue) ret);
        }
        return ret;
    }

    private BasicValue _unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        switch(insn.getOpcode()) {
            case INEG:
            case IINC:
            case L2I:
            case F2I:
            case D2I:
            case I2B:
            case I2C:
            case I2S:
                SinkableArrayValue ret = new SinkableArrayValue(Type.INT_TYPE);
                ret.addDep((SinkableArrayValue) value);
                return ret;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                ret = new SinkableArrayValue(Type.FLOAT_TYPE);
                ret.addDep((SinkableArrayValue) value);
                return ret;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                ret = new SinkableArrayValue(Type.LONG_TYPE);
                ret.addDep((SinkableArrayValue) value);
                return ret;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                ret = new SinkableArrayValue(Type.DOUBLE_TYPE);
                ret.addDep((SinkableArrayValue) value);
                return ret;
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
            case PUTSTATIC:
            case ATHROW:
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                return null;
            case GETFIELD:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEWARRAY:
                ret = null;
                switch(((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        ret = (SinkableArrayValue) newValue(Type.getType("[Z"));
                        break;
                    case T_CHAR:
                        ret = (SinkableArrayValue) newValue(Type.getType("[C"));
                        break;
                    case T_BYTE:
                        ret = (SinkableArrayValue) newValue(Type.getType("[B"));
                        break;
                    case T_SHORT:
                        ret = (SinkableArrayValue) newValue(Type.getType("[S"));
                        break;
                    case T_INT:
                        ret = (SinkableArrayValue) newValue(Type.getType("[I"));
                        break;
                    case T_FLOAT:
                        ret = (SinkableArrayValue) newValue(Type.getType("[F"));
                        break;
                    case T_DOUBLE:
                        ret = (SinkableArrayValue) newValue(Type.getType("[D"));
                        break;
                    case T_LONG:
                        ret = (SinkableArrayValue) newValue(Type.getType("[J"));
                        break;
                    default:
                        throw new AnalyzerException(insn, "Invalid array type");
                }
                ret.addDep((SinkableArrayValue) value);
                ret.isNewArray = true;
                return ret;
            case ANEWARRAY:
                String desc = ((TypeInsnNode) insn).desc;
                return newValue(Type.getType("[" + Type.getObjectType(desc)));
            case ARRAYLENGTH:
                ret = (SinkableArrayValue) newValue(Type.INT_TYPE);
                if(value instanceof SinkableArrayValue && value.getType() != null && TaintUtils.isPrimitiveArrayType(value.getType())) {
                    ret.addDep((SinkableArrayValue) value);
                }
                return ret;
            case CHECKCAST:
                desc = ((TypeInsnNode) insn).desc;
                BasicValue _ret = newValue(Type.getObjectType(desc));
                if(value instanceof SinkableArrayValue) {
                    if((_ret instanceof SinkableArrayValue)) {
                        ((SinkableArrayValue) _ret).addDep((SinkableArrayValue) value);
                    }
                }
                return _ret;
            case INSTANCEOF:
                return newValue(Type.INT_TYPE);
            default:
                throw new Error("Internal error.");
        }
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        BasicValue v = _unaryOperation(insn, value);
        if(v instanceof SinkableArrayValue) {
            ((SinkableArrayValue) v).setSrc(insn);
        }
        return v;
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
        SinkableArrayValue ret;
        switch(insn.getOpcode()) {
            case IALOAD:
                ret = (SinkableArrayValue) newValue(Type.INT_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case BALOAD:
                ret = (SinkableArrayValue) newValue(Type.BYTE_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case CALOAD:
                ret = (SinkableArrayValue) newValue(Type.CHAR_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case SALOAD:
                ret = (SinkableArrayValue) newValue(Type.SHORT_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case DALOAD:
                ret = (SinkableArrayValue) newValue(Type.DOUBLE_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case LALOAD:
                ret = (SinkableArrayValue) newValue(Type.LONG_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
            case FALOAD:
                ret = (SinkableArrayValue) newValue(Type.FLOAT_TYPE);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) value1);
                return ret;
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
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
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
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                SinkableArrayValue v1 = (SinkableArrayValue) value1;
                SinkableArrayValue v2 = (SinkableArrayValue) value2;
                SinkableArrayValue v3 = new SinkableArrayValue(v1.getType());
                v3.addDep(v1);
                v3.addDep(v2);
                return v3;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                v1 = (SinkableArrayValue) value1;
                v2 = (SinkableArrayValue) value2;
                v3 = new SinkableArrayValue(Type.INT_TYPE);
                v3.addDep(v1);
                v3.addDep(v2);
                return v3;
            case AALOAD:
                if(value1.getType() == null) {
                    ret = new SinkableArrayValue(null);
                    ret.setSrc(insn);
                    return ret;
                }

                if(value1 == BasicValue.REFERENCE_VALUE) {
                    return super.binaryOperation(insn, value1, value2);
                }

                Type t = Type.getType(value1.getType().getDescriptor().substring(1));
                if(TaintUtils.isPrimitiveArrayType(t)) {
                    ret = new SinkableArrayValue(t);
                    ret.setSrc(insn);
                    return ret;
                } else if(t.getSort() == Type.ARRAY) {
                    ret = new SinkableArrayValue(t);
                    ret.setSrc(insn);
                    return ret;
                } else {
                    return super.binaryOperation(insn, value1, value2);
                }
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new UnsupportedOperationException("Internal error.");
        }
    }

    @Override
    public BasicValue merge(BasicValue v, BasicValue w) {
        if(v == BasicValue.UNINITIALIZED_VALUE && w == BasicValue.UNINITIALIZED_VALUE) {
            return v;
        } else if(!(v instanceof SinkableArrayValue || w instanceof SinkableArrayValue)) {
            return super.merge(v, w);
        } else if(v.equals(w)) {
            return v;
        }
        if(v instanceof SinkableArrayValue && w instanceof SinkableArrayValue) {
            SinkableArrayValue sv = (SinkableArrayValue) v;
            SinkableArrayValue sw = (SinkableArrayValue) w;
            if(sv.deepEquals(sw)) {
                if(sw.reverseDeps != null) {
                    for(SinkableArrayValue r : sw.reverseDeps) {
                        r.deps.remove(sw);
                        r.addDep(sv);
                    }
                }
                if(sw.deps != null) {
                    if(sv.deps == null) {
                        sv.deps = new HashSet<>();
                    }
                    sv.deps.addAll(sw.deps);
                }
                sw.disable();
                return sv;
            }
            if(sv.getSrc() != null && sw.getSrc() != null && sv.getSrc().equals(sw.getSrc()) && sv.getSrc().getOpcode() != Opcodes.CHECKCAST) {
                if((sv.getType() == null && sw.getType() == null) || (sv.getType() != null && sw.getType() != null && sv.getType().equals(sw.getType()))) {
                    if(sv.reverseDeps == null && sw.reverseDeps != null) {
                        sv.reverseDeps = new HashSet<>();
                    }
                    if(sw.reverseDeps != null) {
                        for(SinkableArrayValue r : sw.reverseDeps) {
                            r.deps.remove(sw);
                            r.addDep(sv);
                            sv.reverseDeps.add(r);
                        }
                    }
                    if(sw.deps != null) {
                        if(sv.deps == null) {
                            sv.deps = new HashSet<>();
                        }
                        sv.deps.addAll(sw.deps);
                    }
                    sw.disable();
                    return sv;
                }
            }

            if((v.getType() == null || v.getType().getDescriptor().equals("Lnull;"))
                    && (w.getType() == null || w.getType().getDescriptor().equals("Lnull;"))) {
                if(sv.getSrc() != null && sv.getSrc().equals(sw.oldSrc)) {
                    return sv;
                } else if(sw.getSrc() != null && sv.deps != null && sv.deps.contains(sw)
                        || sw.getSrc() == null && sw.deps != null && sv.deps != null && sv.deps.containsAll(sw.deps)) {
                    return v;
                } else if(sv.deps != null && sv.deps.contains(sw)) {
                    return v;
                } else if(sw.getSrc() != null && sw.getSrc().equals(sv.oldSrc)) {
                    return sw;
                } else if(sv.getSrc() == null && sw.getSrc() == null) {
                    sv.addDep(sw);
                    return v;
                } else if(sw.deps != null && sw.deps.contains(sv)) {
                    return w;
                } else if(sv.getSrc() != null) {
                    sv.addDep(sw);
                    return v;
                } else if(sw.getSrc() != null) {
                    sw.addDep(sv);
                    return w;
                } else {
                    sv.addDep(sw);
                    return v;
                }
            }
            if(v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
                if(w.getType().getSort() == Type.ARRAY) {
                    sw.addDep(sv);
                    if(sv.oldSrc != null) {
                        sv.setSrc(sv.oldSrc);
                        sv.oldSrc = null;
                        sv.doNotPropagateToDeps = false;
                        sv.flowsToPrim = true;
                    }
                }
                return w;
            } else if(w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
                if(v.getType().getSort() == Type.ARRAY) {
                    sv.addDep(sw);
                    if(sw.oldSrc != null) {
                        sw.setSrc(sw.oldSrc);
                        sw.oldSrc = null;
                        sw.doNotPropagateToDeps = false;
                        sw.flowsToPrim = true;
                    }
                    if(sv.getSrc() != null && sv.getSrc().getOpcode() == Opcodes.CHECKCAST && sw.getSrc() != null && sw.getSrc().getOpcode() == Opcodes.ACONST_NULL) {
                        sv.okToPropagateToDeps = true;
                    }
                }
                return v;
            } else if(TaintUtils.isPrimitiveOrPrimitiveArrayType(v.getType())
                    && TaintUtils.isPrimitiveOrPrimitiveArrayType(w.getType())) {
                if(v.getType().equals(w.getType())) {
                    if(sv.flowsToInstMethodCall && !sw.flowsToInstMethodCall) {
                        return v;
                    } else if(sw.flowsToInstMethodCall && !sv.flowsToInstMethodCall) {
                        return v;
                    } else {
                        //At this point, we need to see if we are merging from 2 CHECKCAST.
                        //If so, we need to see if both inputs to the CHECKCAST were java/lang/object or NULL.
                        //If they both are, then we MUST NOT allow them to be marked as relevant, or the NULL will
                        //then load double NULL's
                        //however, if one is prim array and other is NULL, then we MUST allow them to be marked as relevant
                        //so stacks will match
                        if(sw.getSrc() != null && sv.getSrc() != null && sw.getSrc().getOpcode() == Opcodes.CHECKCAST && sv.getSrc().getOpcode() == Opcodes.CHECKCAST) {
                            if(sv.deps == null || sw.deps == null) {
                                if(sv.deps != null && sv.deps.size() == 1 && sw.deps == null) {
                                    if(!sv.okToPropagateToDeps) {
                                        sv.doNotPropagateToDeps = true;
                                    }
                                } else if(sw.deps != null && sw.deps.size() == 1 && sv.deps == null) {
                                    if(!sv.okToPropagateToDeps) {
                                        sv.doNotPropagateToDeps = true;
                                    }
                                }
                            }
                        }
                        sv.addDep(sw);
                        return v;
                    }
                }
            }
        }
        if(v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
            //By construction, where we came from couldn't have been flowing to an array
            if(v instanceof SinkableArrayValue && ((SinkableArrayValue) v).getSrc() != null && !((SinkableArrayValue) v).flowsToPrim) {
                ((SinkableArrayValue) v).disable();
                ((SinkableArrayValue) v).doNotPropagateToDeps = true;
            }
            return w;
        } else if(w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
            if(w instanceof SinkableArrayValue && ((SinkableArrayValue) w).getSrc() != null && !((SinkableArrayValue) w).flowsToPrim) {
                if(v.getType() == null || !v.getType().getInternalName().equals("java/lang/Object")) {
                    ((SinkableArrayValue) w).disable();
                    ((SinkableArrayValue) w).doNotPropagateToDeps = true;
                }
            }
            return v;
        }
        if(v.getType().getDescriptor().equals("Ljava/lang/Object;")) {
            return v;
        }
        if(v.getType().getSort() == Type.ARRAY && v.getType().getDimensions() > 1) {
            return v;
        }

        if(v.getType().equals(w.getType()) || (v.getType().getDescriptor().length() == 1
                && w.getType().getDescriptor().length() == 1)) {
            SinkableArrayValue r = new SinkableArrayValue(v.getType());
            r.addDep((SinkableArrayValue) w);
            r.addDep((SinkableArrayValue) v);
            return r;
        }
        return new BasicArrayValue(Type.getType(Object.class));
    }

    private BasicValue _newOperation(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                return newValue(Type.getObjectType("null"));
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case BIPUSH:
            case SIPUSH:
                return newValue(Type.INT_TYPE);
            case LCONST_0:
            case LCONST_1:
                return newValue(Type.LONG_TYPE);
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return newValue(Type.FLOAT_TYPE);
            case DCONST_0:
            case DCONST_1:
                return newValue(Type.DOUBLE_TYPE);
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if(cst instanceof Integer) {
                    return newValue(Type.INT_TYPE);
                } else if(cst instanceof Float) {
                    return newValue(Type.FLOAT_TYPE);
                } else if(cst instanceof Long) {
                    return newValue(Type.LONG_TYPE);
                } else if(cst instanceof Double) {
                    return newValue(Type.DOUBLE_TYPE);
                } else if(cst instanceof String) {
                    return newValue(Type.getObjectType("java/lang/String"));
                } else if(cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if(sort == Type.OBJECT || sort == Type.ARRAY) {
                        return newValue(Type.getObjectType("java/lang/Class"));
                    } else if(sort == Type.METHOD) {
                        return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
                    } else {
                        throw new IllegalArgumentException("Illegal LDC constant " + cst);
                    }
                } else if(cst instanceof Handle) {
                    return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
                } else {
                    throw new IllegalArgumentException("Illegal LDC constant " + cst);
                }
            case JSR:
                return BasicValue.RETURNADDRESS_VALUE;
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
            default:
                throw new Error("Internal error.");
        }
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) {
        BasicValue ret = _newOperation(insn);
        if(ret instanceof SinkableArrayValue) {
            ((SinkableArrayValue) ret).setSrc(insn);
        }
        return ret;
    }

    @Override
    public BasicValue newValue(Type type) {
        if(type == null) {
            return new SinkableArrayValue(null);
        }
        if (TaintUtils.isShadowedType(type)) {
            return new SinkableArrayValue(type);
        } else if (type.getSort() == Type.ARRAY && type.getElementType().getSort() != Type.OBJECT) {
            return new BasicArrayValue(type);
        } else if (type.getDescriptor().equals("Lnull;")) {
            return new SinkableArrayValue(null);
        } else {
            return super.newValue(type);
        }
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, java.util.List<? extends BasicValue> values) throws AnalyzerException {
        if(insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            Type retType = Type.getReturnType(min.desc);
            if(TaintUtils.isPrimitiveOrPrimitiveArrayType(retType)) {
                SinkableArrayValue ret = new SinkableArrayValue(retType);
                ret.setSrc(insn);
                return ret;
            } else if(min.name.equals("clone") && min.desc.equals("()Ljava/lang/Object;") && values.get(0) instanceof SinkableArrayValue) {
                SinkableArrayValue ret = new SinkableArrayValue(retType);
                ret.setSrc(insn);
                ret.addDep((SinkableArrayValue) values.get(0));
                return ret;
            }
        } else if(insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode min = (InvokeDynamicInsnNode) insn;
            Type retType = Type.getReturnType(min.desc);
            if(TaintUtils.isPrimitiveOrPrimitiveArrayType(retType)) {
                SinkableArrayValue ret = new SinkableArrayValue(retType);
                ret.setSrc(insn);
                return ret;
            }

        }
        return super.naryOperation(insn, values);
    }

    private static String toStringSrcs(Collection<SinkableArrayValue> c) {
        StringBuilder r = new StringBuilder("[");
        if(c != null) {
            for(SinkableArrayValue s : c) {
                if(s.getSrc() != null) {
                    r.append(Printer.OPCODES[s.getSrc().getOpcode()]).append(", ");
                }
            }
        }
        return r + "]";
    }
}
