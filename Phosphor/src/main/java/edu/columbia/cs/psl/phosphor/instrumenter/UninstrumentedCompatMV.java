package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class UninstrumentedCompatMV extends TaintAdapter {
    private NeverNullArgAnalyzerAdapter analyzer;
    private Type returnType;

    public UninstrumentedCompatMV(int access, String className, String name, String desc, String signature,
            String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer, boolean initializePhosphorStackFrame) {
        super(access, className, name, desc, signature, exceptions, mv, analyzer, initializePhosphorStackFrame);
        this.analyzer = analyzer;
        this.returnType = Type.getReturnType(desc);
    }

    ReferenceArrayTarget referenceArrayTarget;

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        Type arrayType = Type.getType(desc);
        StringBuilder methodToCall = new StringBuilder("MULTIANEWARRAY_");
        StringBuilder descToCall = new StringBuilder("(");
        pushPhosphorStackFrame();

        if (arrayType.getElementType().getSort() == Type.OBJECT) {
            methodToCall.append("REFERENCE");
            if (dims == arrayType.getDimensions()) {
                descToCall.append("Ljava/lang/Class;");
                super.visitLdcInsn(arrayType.getElementType());
                super.visitInsn(SWAP);
            }
        } else {
            methodToCall.append(arrayType.getElementType().getDescriptor());
        }
        methodToCall.append('_');
        methodToCall.append(arrayType.getDimensions());
        methodToCall.append("DIMS");
        descToCall.append(PhosphorStackFrame.DESCRIPTOR);
        descToCall.append(")");
        descToCall.append(Type.getDescriptor(TaggedReferenceArray.class));
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                methodToCall.toString(), descToCall.toString(), false);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.CHECKCAST) {
            if (!analyzer.stack.isEmpty() && "java/lang/Object".equals(analyzer.stack.get(analyzer.stack.size() - 1))
                    && type.startsWith("[")
                    && type.length() == 2) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                        "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
            Type t = Type.getObjectType(type);
            if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                type = MultiDArrayUtils.getTypeForType(t).getInternalName();
            }
        } else if (opcode == Opcodes.ANEWARRAY) {
            Type t = Type.getObjectType(type);
            if (t.getSort() == Type.ARRAY) {
                type = TaintUtils.getWrapperType(t).getInternalName();
            }
            super.visitTypeInsn(opcode, type);
            // 2D arrays are just 1D arrays, not wrapped 1Darrays
            Type arType = Type.getType(TaggedReferenceArray.class);
            super.visitMethodInsn(INVOKESTATIC, arType.getInternalName(), "factory",
                    "([Ljava/lang/Object;)" + arType.getDescriptor(),
                    false);
            return;
        } else if (opcode == Opcodes.INSTANCEOF) {
            Type t = Type.getObjectType(type);
            if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                type = MultiDArrayUtils.getTypeForType(t).getDescriptor();
            } else if (t.getSort() == Type.ARRAY && t.getDimensions() == 1
                    && t.getElementType().getSort() != Type.OBJECT
                    && getTopOfStackObject().equals("java/lang/Object")) {
                type = MultiDArrayUtils.getTypeForType(t).getInternalName();
            }
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type t = Type.getType(desc);
        switch (opcode) {
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
                if (TaintUtils.isWrappedTypeWithSeparateField(t)) {
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD,
                            TaintUtils.getWrapperType(t).getDescriptor());
                    // super.visitMethodInsn(INVOKEVIRTUAL,
                    // TaintUtils.getWrapperType(t).getInternalName(),
                    // "getVal","()Ljava/lang/Object;", false);
                    // super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
                if (TaintUtils.isWrappedTypeWithSeparateField(t)) {
                    if (opcode == Opcodes.PUTFIELD) {
                        super.visitInsn(DUP2);
                    } else {
                        super.visitInsn(DUP);
                    }
                    //Two possible cases here: We have the wrapped type already on the stack, or we don't.
                    Type onStack = getTopOfStackType();
                    Type taintFieldType = TaintUtils.getWrapperType(t);
                    if (TaintUtils.isWrapperType(onStack)){
                        //It is a wrapepr
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD,
                                taintFieldType.getDescriptor());

                        if (t.getDimensions() > 1) {
                            super.visitInsn(POP);
                            super.visitInsn(ACONST_NULL);
                        } else {
                            Type unwrapped = TaintUtils.getUnwrappedType(taintFieldType);

                            super.visitMethodInsn(INVOKESTATIC, onStack.getInternalName(), "unwrap", "(" + onStack.getDescriptor() + ")" + unwrapped.getDescriptor(), false);
                            if (unwrapped.getDescriptor().equals("[Ljava/lang/Object;")) {
                                super.visitTypeInsn(CHECKCAST, t.getInternalName());
                            }
                        }
                        super.visitFieldInsn(opcode, owner, name, desc);
                    } else {
                        //It's not wrapped
                        super.visitFieldInsn(opcode, owner, name, desc);
                        String factoryType = desc;
                        if (t.getElementType().getSort() == Type.OBJECT) {
                            factoryType = "[Ljava/lang/Object;";
                        }
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, taintFieldType.getInternalName(), "factory",
                                "(" + factoryType + ")" + taintFieldType, false);
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD,
                                taintFieldType.getDescriptor());
                    }
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof ReferenceArrayTarget) {
            referenceArrayTarget = (ReferenceArrayTarget) value;
        }
        super.visitLdcInsn(value);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (opcode <= 200) {
            super.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.IASTORE:
                super.visitInsn(opcode);
                break;
            case Opcodes.AASTORE:
                Object arType = analyzer.stack.get(analyzer.stack.size() - 3);
                Type _arType = TaintAdapter.getTypeForStackType(arType);
                if (_arType.getSort() == Type.OBJECT) {
                    // storing to a multid array...
                    super.visitMethodInsn(INVOKEVIRTUAL, _arType.getInternalName(), "setUninst",
                            "(ILjava/lang/Object;)V", false);
                    break;
                }
                Type elType = getTopOfStackType();
                if (arType.equals("[Ljava/lang/Object;") &&
                        ((elType.getSort() == Type.ARRAY && elType.getElementType().getSort() != Type.OBJECT)
                                || elType.getDescriptor().equals("Ljava/lang/Object;"))) {
                    BOX_IF_NECESSARY.delegateVisit(mv);
                } else if (arType instanceof String
                        && ((String) arType).contains("[Ledu/columbia/cs/psl/phosphor/struct/Tagged")) {
                    BOX_IF_NECESSARY.delegateVisit(mv);
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.AALOAD:
                Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
                Type t = getTypeForStackType(arrayType);
                if (t.getSort() != Type.ARRAY) {
                    super.visitMethodInsn(INVOKEVIRTUAL, t.getInternalName(), "get", "(I)Ljava/lang/Object;", false);
                    Type originalArrayType = Type.getType(referenceArrayTarget.getOriginalArrayType());
                    super.visitTypeInsn(CHECKCAST, originalArrayType.getElementType().getInternalName());
                    referenceArrayTarget = null;
                } else if (t.getDimensions() == 1
                        && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tagged")) {
                    super.visitInsn(opcode);
                    try {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                                "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        super.visitTypeInsn(Opcodes.CHECKCAST, "[" + MultiDArrayUtils.getPrimitiveTypeForWrapper(
                                Class.forName(t.getElementType().getInternalName().replace("/", "."))));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                if (getTopOfStackObject().equals("java/lang/Object")) {
                    // never allow monitor to occur on a multid type
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                            "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.ARRAYLENGTH:
                Type onStack = getStackTypeAtOffset(0);
                if (onStack.getSort() == Type.OBJECT) {
                    Type underlying = MultiDArrayUtils.getPrimitiveTypeForWrapper(onStack.getInternalName());
                    if (underlying != null) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                                "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        super.visitTypeInsn(Opcodes.CHECKCAST, "[" + underlying.getDescriptor());
                    }
                }
                super.visitInsn(opcode);

                break;
            case Opcodes.LALOAD:
            case Opcodes.DALOAD:
            case Opcodes.IALOAD:
            case Opcodes.FALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                onStack = Type.getObjectType((String) analyzer.stack.get(analyzer.stack.size() - 2));
                if (onStack.getSort() != Type.ARRAY) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, onStack.getInternalName(), "unwrap",
                            "(" + onStack.getDescriptor() + ")" + TaintUtils.getUnwrappedType(onStack).getDescriptor(),
                            false);
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.ARETURN:
                if (TaintUtils.isWrappedType(returnType) && !topOfStackIsNull()) {
                    //Need to return the unboxed, and save this for later
                    super.visitInsn(DUP);

                    //Save for later
                    pushPhosphorStackFrame();
                    super.visitInsn(SWAP);
                    SET_WRAPPED_RETURN.delegateVisit(mv);

                    t = getTopOfStackType();
                    //Unbox now:
                    //For 1-d arrays: still pass  the actual array
                    if (returnType.getDimensions() == 1) {
                        Type unwrapped = TaintUtils.getUnwrappedType(t);
                        super.visitMethodInsn(INVOKESTATIC, t.getInternalName(), "unwrap", "(" + t.getDescriptor() + ")" + unwrapped.getDescriptor(), false);
                        if (unwrapped.getDescriptor().equals("[Ljava/lang/Object;")) {
                            super.visitTypeInsn(CHECKCAST, returnType.getInternalName());
                        }
                    } else {
                        // Greater than 1d: can't make it line up...
                        super.visitInsn(POP);
                        super.visitInsn(ACONST_NULL);
                    }
                }
                super.visitInsn(opcode);
                break;
            default:
                super.visitInsn(opcode);
                break;
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (Configuration.WITH_UNBOX_ACMPEQ) {
            switch (opcode) {
                case IF_ACMPEQ:
                case IF_ACMPNE:
                    ENSURE_UNBOXED.delegateVisit(mv);
                    super.visitInsn(SWAP);
                    ENSURE_UNBOXED.delegateVisit(mv);
                    super.visitInsn(SWAP);
                    break;
            }
        }
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        prepareMetadataLocalVariables();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (Instrumenter.isIgnoredClass(owner)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        Type ownerType = Type.getObjectType(owner);
        if (opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY
                && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
            owner = MultiDArrayUtils.getTypeForType(ownerType).getInternalName();
        }
        if ((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem")
                || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
                && !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
            owner = Type.getInternalName(TaintUtils.class);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        unwrapArraysForCallTo(owner, name, desc);
        pushPhosphorStackFrame();
        if(Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
            super.visitLdcInsn(name + desc.substring(0, 1 + desc.indexOf(')')));
            PREPARE_FOR_CALL_DEBUG.delegateVisit(mv);
        } else {
            push(PhosphorStackFrame.hashForDesc(name + desc.substring(0, 1 + desc.indexOf(')'))));
            PREPARE_FOR_CALL_FAST.delegateVisit(mv);
        }
        Type[] args = Type.getArgumentTypes(desc);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

}
