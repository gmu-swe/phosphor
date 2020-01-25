package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class UninstrumentedCompatMV extends TaintAdapter {
    private NeverNullArgAnalyzerAdapter analyzer;
    private boolean skipFrames;
    private Type returnType;
    private Type originalReturnType;

    public UninstrumentedCompatMV(int access, String className, String name, String desc, Type originalReturnType, String signature, String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer,
                                  boolean skipFrames) {
        super(access, className, name, desc, signature, exceptions, mv, analyzer);
        this.analyzer = analyzer;
        this.skipFrames = skipFrames;
        this.returnType = Type.getReturnType(desc);
        this.originalReturnType = originalReturnType;
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        Object[] newLocal = new Object[local.length];
        Object[] newStack = new Object[stack.length];
        for(int i = 0; i < local.length; i++) {
            if(local[i] instanceof String) {
                Type t = Type.getObjectType(((String) local[i]));
                if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                    newLocal[i] = MultiDTaintedArray.getTypeForType(t).getInternalName();
                } else {
                    newLocal[i] = local[i];
                }
            } else {
                newLocal[i] = local[i];
            }
        }
        for(int i = 0; i < stack.length; i++) {
            if(stack[i] instanceof String) {
                Type t = Type.getObjectType(((String) stack[i]));
                if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                    newStack[i] = MultiDTaintedArray.getTypeForType(t).getInternalName();
                } else {
                    newStack[i] = stack[i];
                }
            } else {
                newStack[i] = stack[i];
            }
        }
        super.visitFrame(type, nLocal, newLocal, nStack, newStack);
    }

    ReferenceArrayTarget referenceArrayTarget;

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        Type arrayType = Type.getType(desc);
        Type origType = arrayType;
        boolean needToHackDims = false;
        int tmp = 0;
        if(arrayType.getElementType().getSort() != Type.OBJECT) {
            if(dims == arrayType.getDimensions()) {
                needToHackDims = true;
                dims--;
                tmp = lvs.getTmpLV(Type.INT_TYPE);
                super.visitVarInsn(Opcodes.ISTORE, tmp);
            }
            arrayType = MultiDTaintedArray.getTypeForType(arrayType);
            //Type.getType(MultiDTaintedArray.getClassForComponentType(arrayType.getElementType().getSort()));
            desc = arrayType.getInternalName();
        }
        if(dims == 1) {
            //It's possible that we dropped down to a 1D object type array
            super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
        } else {
            super.visitMultiANewArrayInsn(desc, dims);
        }
        if(needToHackDims) {
            super.visitInsn(DUP);
            super.visitVarInsn(ILOAD, tmp);
            lvs.freeTmpLV(tmp);
            super.visitIntInsn(BIPUSH, origType.getElementType().getSort());
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "initLastDim", "([Ljava/lang/Object;II)V", false);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(opcode == Opcodes.CHECKCAST) {
            if(!analyzer.stack.isEmpty() && "java/lang/Object".equals(analyzer.stack.get(analyzer.stack.size() - 1)) && type.startsWith("[")
                    && type.length() == 2) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
            Type t = Type.getObjectType(type);
            if(t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                type = MultiDTaintedArray.getTypeForType(t).getInternalName();
            }
        } else if(opcode == Opcodes.ANEWARRAY) {
            Type t = Type.getObjectType(type);
            if(t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
                //e.g. [I for a 2 D array -> MultiDTaintedIntArray
                type = MultiDTaintedArray.getTypeForType(t).getInternalName();
            }
        } else if(opcode == Opcodes.INSTANCEOF) {
            Type t = Type.getObjectType(type);
            if(t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
            } else if(t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT
                    && getTopOfStackObject().equals("java/lang/Object")) {
                type = MultiDTaintedArray.getTypeForType(t).getInternalName();
            }
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type t = Type.getType(desc);
        switch(opcode) {
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
                if(TaintUtils.isWrappedTypeWithSeparateField(t)) {
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, TaintUtils.getWrapperType(t).getDescriptor());
                    // super.visitMethodInsn(INVOKEVIRTUAL, TaintUtils.getWrapperType(t).getInternalName(), "getVal","()Ljava/lang/Object;", false);
                    // super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
                if(TaintUtils.isWrappedTypeWithSeparateField(t)) {
                    Type taintFieldType = TaintUtils.getWrapperType(t);
                    //1d prim array - need to make sure that there is some taint here
                    FrameNode fn = getCurrentFrameNode();
                    fn.type = Opcodes.F_NEW;
                    super.visitInsn(Opcodes.DUP);
                    Label ok = new Label();
                    super.visitJumpInsn(IFNULL, ok);
                    if(opcode == Opcodes.PUTFIELD) {
                        //O A
                        super.visitInsn(DUP2);
                        //O A O A
                    } else {
                        super.visitInsn(Opcodes.DUP);
                    }
                    String factoryType = desc;
                    if(t.getElementType().getSort() == Type.OBJECT) {
                        factoryType = "[Ljava/lang/Object;";
                    }
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                    super.visitInsn(SWAP);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, taintFieldType.getInternalName(), "factory", "(" + Configuration.TAINT_TAG_DESC + factoryType + ")" + taintFieldType, false);
                    // O A O W
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, taintFieldType.getDescriptor());
                    super.visitLabel(ok);
                    if(!skipFrames) {
                        fn.accept(this);
                    }
                }
                super.visitFieldInsn(opcode, owner, name, desc);
                break;
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        if(value instanceof ReferenceArrayTarget) {
            referenceArrayTarget = (ReferenceArrayTarget) value;
        }
        super.visitLdcInsn(value);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if(opcode <= 200) {
            super.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch(opcode) {
            case Opcodes.IASTORE:
                super.visitInsn(opcode);
                break;
            case Opcodes.AASTORE:
                Object arType = analyzer.stack.get(analyzer.stack.size() - 3);
                Type _arType = TaintAdapter.getTypeForStackType(arType);
                if(_arType.getSort() == Type.OBJECT) {
                    //storing to a multid array...
                    super.visitMethodInsn(INVOKEVIRTUAL, _arType.getInternalName(), "setUninst", "(ILjava/lang/Object;)V", false);
                    break;
                }
                Type elType = getTopOfStackType();
                if(arType.equals("[Ljava/lang/Object;") &&
                        ((elType.getSort() == Type.ARRAY && elType.getElementType().getSort() != Type.OBJECT)
                                || elType.getDescriptor().equals("Ljava/lang/Object;"))) {
                    visit(BOX_IF_NECESSARY);
                } else if(arType instanceof String && ((String) arType).contains("[Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    visit(BOX_IF_NECESSARY);
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.AALOAD:
                Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
                Type t = getTypeForStackType(arrayType);
                if(t.getSort() != Type.ARRAY) {
                    super.visitMethodInsn(INVOKEVIRTUAL, t.getInternalName(), "get", "(I)Ljava/lang/Object;", false);
                    super.visitTypeInsn(CHECKCAST, referenceArrayTarget.getOriginalArrayType());
                    referenceArrayTarget = null;
                } else if(t.getDimensions() == 1 && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    super.visitInsn(opcode);
                    try {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        super.visitTypeInsn(Opcodes.CHECKCAST, "[" + MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", "."))));
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                if(getTopOfStackObject().equals("java/lang/Object")) {
                    //never allow monitor to occur on a multid type
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.ARRAYLENGTH:
                Type onStack = getTopOfStackType();
                if(onStack.getSort() == Type.OBJECT) {
                    Type underlying = MultiDTaintedArray.getPrimitiveTypeForWrapper(onStack.getInternalName());
                    if(underlying != null) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        super.visitTypeInsn(Opcodes.CHECKCAST, "[" + underlying.getDescriptor());
                    }
                }
                super.visitInsn(opcode);

                break;
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
                wrapPrimitiveReturn(false);
                break;
            case Opcodes.DRETURN:
            case Opcodes.LRETURN:
                wrapPrimitiveReturn(true);
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
                    super.visitInsn(SWAP);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, onStack.getInternalName(), "unwrap", "(" + onStack.getDescriptor() + ")" + TaintUtils.getUnwrappedType(onStack).getDescriptor(), false);
                    super.visitInsn(SWAP);
                }
                super.visitInsn(opcode);
                break;
            default:
                super.visitInsn(opcode);
                break;
        }
    }

    /**
     * stack_pre = primitive
     * stack_post = empty
     *
     * @param widePrimitive true if the primitive to be wrapped is of size two (i.e., a double or long)
     */
    private void wrapPrimitiveReturn(boolean widePrimitive) {
        int lv = lvs.getPreAllocatedReturnTypeVar(returnType);
        super.visitVarInsn(Opcodes.ALOAD, lv);
        super.visitInsn(DUP);
        // primitive wrapper wrapper
        NEW_EMPTY_TAINT.delegateVisit(mv);
        super.visitFieldInsn(Opcodes.PUTFIELD, returnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
        // primitive wrapper
        if(widePrimitive) {
            super.visitInsn(DUP_X2);
            super.visitInsn(POP);
        } else {
            super.visitInsn(SWAP);
        }
        super.visitFieldInsn(Opcodes.PUTFIELD, returnType.getInternalName(), "val", originalReturnType.getDescriptor());
        super.visitVarInsn(Opcodes.ALOAD, lv);
        super.visitInsn(ARETURN);
    }

    private void ensureBoxedAt(int n, Type t) {
        switch(n) {
            case 0:
                visit(BOX_IF_NECESSARY);
                super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                break;
            case 1:
                Object top = analyzer.stack.get(analyzer.stack.size() - 1);
                if(top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    super.visitInsn(DUP2_X1);
                    super.visitInsn(POP2);
                    visit(BOX_IF_NECESSARY);
                    super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitInsn(SWAP);
                    visit(BOX_IF_NECESSARY);
                    super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                    super.visitInsn(SWAP);
                }
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n);
                visit(BOX_IF_NECESSARY);
                super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                for(int i = n - 1; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if(Configuration.WITH_UNBOX_ACMPEQ) {
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
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if(Instrumenter.isIgnoredClass(owner)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        Type ownerType = Type.getObjectType(owner);
        if(opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
            owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
        }
        Type origReturnType = Type.getReturnType(desc);
        boolean isCalledOnArrayType = false;
        if((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
                && !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
            owner = Type.getInternalName(TaintUtils.class);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        if(Instrumenter.isIgnoredClass(owner) || Instrumenter.isIgnoredMethod(owner, name, desc)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        if((opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) && !analyzer.stack.isEmpty()) {
            int argsize = 0;
            for(Type t : Type.getArgumentTypes(desc)) {
                argsize += t.getSize();
            }
            Object calledOn = analyzer.stack.get(analyzer.stack.size() - argsize - 1);
            if(calledOn instanceof String && ((String) calledOn).startsWith("[")) {
                isCalledOnArrayType = true;
            }
        }

        if(!Instrumenter.isIgnoredClass(owner) && !owner.startsWith("[") && !isCalledOnArrayType) {
            //maybe, call into the instrumented version
            Type newReturnType = TaintUtils.getContainerReturnType(origReturnType);
            boolean hasWrappedReturnType = origReturnType.getSort() != Type.ARRAY && !newReturnType.equals(origReturnType);
            //Find out if there are any variablest hat may need to be boxed.
            Type[] args = Type.getArgumentTypes(desc);
            int k = 0;
            boolean hasArgsToBox = false;
            int offset = 0;
            for(int i = args.length - 1; i >= 0; i--) {
                offset += args[i].getSize();
                Type onStack = TaintAdapter.getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - offset));
                if(args[i].getDescriptor().equals("Ljava/lang/Object;") && (onStack.getDescriptor().equals("Ljava/lang/Object;") || TaintUtils.isPrimitiveArrayType(onStack))) {
                    hasArgsToBox = true;
                }
            }
            if(hasArgsToBox) {
                //Calling an instrumented method possibly!
                int argsSize = 0;
                for(int i = 0; i < args.length; i++) {
                    argsSize += args[args.length - i - 1].getSize();
                    //TODO optimize
                    if(args[args.length - i - 1].getDescriptor().endsWith("java/lang/Object;")) {
                        ensureBoxedAt(i, args[args.length - i - 1]);
                    }
                }
            } else if(hasArgsToBox) {
                String newDesc = TaintUtils.remapMethodDesc(opcode != INVOKESTATIC, desc);
                int[] argStorage = new int[args.length];
                for(int i = 0; i < args.length; i++) {
                    Type t = args[args.length - i - 1];
                    int lv = lvs.getTmpLV(t);
                    super.visitVarInsn(t.getOpcode(Opcodes.ISTORE), lv);
                    argStorage[args.length - i - 1] = lv;
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);

        } else {
            if(!name.equals("clone") && !name.equals("equals")) {
                System.out.println("Call UNTOUCHED" + owner + name + desc);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    /**
     * Visits a method instruction for the specified method.
     *
     * @param method the method to be visited
     */
    private void visit(TaintMethodRecord method) {
        super.visitMethodInsn(method.getOpcode(), method.getOwner(), method.getName(), method.getDescriptor(), method.isInterface());
    }
}
