package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.NativeHelper;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManager;
import static edu.columbia.cs.psl.phosphor.Configuration.taintTagFactory;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_TYPE;

public class TaintPassingMV extends TaintAdapter implements Opcodes {

    static final String BYTE_NAME = "java/lang/Byte";
    static final String BOOLEAN_NAME = "java/lang/Boolean";
    static final String INTEGER_NAME = "java/lang/Integer";
    static final String FLOAT_NAME = "java/lang/Float";
    static final String LONG_NAME = "java/lang/Long";
    static final String CHARACTER_NAME = "java/lang/Character";
    static final String DOUBLE_NAME = "java/lang/Double";
    static final String SHORT_NAME = "java/lang/Short";
    private final String name;
    private final boolean isStatic;
    private final String owner;
    private final String descriptor;
    private final MethodVisitor passThroughMV;
    private final boolean rewriteLVDebug;
    private final boolean isObjOutputStream;
    private final ControlFlowPropagationPolicy controlFlowPolicy;
    private final List<MethodNode> wrapperMethodsToAdd;
    private final Set<Label> exceptionHandlers = new HashSet<>();
    ReferenceArrayTarget referenceArrayTarget;
    int line = 0;
    private boolean isIgnoreAllInstrumenting;
    private boolean isRawInstruction = false;
    private boolean isTaintlessArrayStore = false;
    private boolean doNotUnboxTaints;
    private boolean isAtStartOfExceptionHandler;
    private boolean isRewrittenMethodDescriptorOrName;

    public TaintPassingMV(MethodVisitor mv, int access, String owner, String name, String descriptor, String signature,
                          String[] exceptions, NeverNullArgAnalyzerAdapter analyzer,
                          MethodVisitor passThroughMV, LinkedList<MethodNode> wrapperMethodsToAdd,
                          ControlFlowPropagationPolicy controlFlowPolicy) {
        super(access, owner, name, descriptor, signature, exceptions, mv, analyzer);
        taintTagFactory.instrumentationStarting(access, name, descriptor);
        this.name = name;
        this.owner = owner;
        this.wrapperMethodsToAdd = wrapperMethodsToAdd;
        this.rewriteLVDebug = owner.equals("java/lang/invoke/MethodType");
        this.passThroughMV = passThroughMV;
        this.descriptor = descriptor;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.isObjOutputStream = (owner.equals("java/io/ObjectOutputStream") && name.startsWith("writeObject0"))
                || (owner.equals("java/io/ObjectInputStream") && name.startsWith("defaultReadFields"));
        this.controlFlowPolicy = controlFlowPolicy;
        this.isRewrittenMethodDescriptorOrName = false;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        prepareMetadataLocalVariables();
        //There are some methods that might be called from native code, and get passed regular arrays as type java.lang.Object,
        //but we will assume from here-on out that they are wrapped arrays. Check that here.
        boolean ensureObjsAreWrapped = false;
        if (this.className.equals("java/lang/invoke/MethodHandleNatives")) {
            if (this.name.equals("linkCallSite")) {
                ensureObjsAreWrapped = true; //TODO should we always do this? just in some places? performance is what? If try everywhere, it crashes...
            }
        }
        boolean pullWrapperFromPrev = false;
        if(this.className.startsWith("java/lang/invoke/VarHandleGuards") && this.methodName.startsWith("guard_")){
            pullWrapperFromPrev = true;
        }

        //Retrieve the taint tags for all arguments
        Type[] args = Type.getArgumentTypes(descriptor);
        int idxOfLV = 0;
        int idxOfArgPosition = 0;

        StringBuilder argTaintPositions = new StringBuilder("PhosphorArgTaintIndices=");
        pushPhosphorStackFrame();
        if (!isStatic) {
            super.visitInsn(DUP);
            push(idxOfLV);
            GET_ARG_TAINT.delegateVisit(mv);
            int shadow = getShadowVar(idxOfLV, ASTORE);
            argTaintPositions.append(shadow);
            argTaintPositions.append(',');
            super.visitVarInsn(ASTORE, shadow);
            idxOfArgPosition++;
            idxOfLV++;
        }
        super.visitInsn(NOP);
        for (int i = 0; i < args.length; i++) {
            super.visitInsn(DUP);
            push(idxOfArgPosition);
            GET_ARG_TAINT.delegateVisit(mv);
            int shadow = getShadowVar(idxOfLV, ASTORE);
            super.visitVarInsn(ASTORE, shadow);
            idxOfArgPosition++;
            argTaintPositions.append(shadow);
            argTaintPositions.append(',');

            //also retrieve wrappers for any vals as needed
            if (TaintUtils.isWrappedType(args[i])) {
                super.visitInsn(DUP);
                push(i);
                super.visitVarInsn(ALOAD, idxOfLV);
                TaintMethodRecord.getArgWrapperMethod(args[i]).delegateVisit(mv);
                this.visitTypeInsn(CHECKCAST, args[i].getInternalName());
                super.visitVarInsn(ASTORE, idxOfLV);
            } else if (ensureObjsAreWrapped && args[i].getInternalName().equals("java/lang/Object")) {
                super.visitVarInsn(ALOAD, idxOfLV);
                BOX_IF_NECESSARY.delegateVisit(mv);
                super.visitVarInsn(ASTORE, idxOfLV);
            } else if (pullWrapperFromPrev && args[i].getInternalName().equals("java/lang/Object")) {
                super.visitInsn(DUP);
                super.visitFieldInsn(GETFIELD, PhosphorStackFrame.INTERNAL_NAME, "prevFrame", PhosphorStackFrame.DESCRIPTOR);
                push(i - 1);
                super.visitVarInsn(ALOAD, idxOfLV);
                GET_ARG_WRAPPER_GENERIC.delegateVisit(mv);
                super.visitVarInsn(ASTORE, idxOfLV);
            }

            idxOfLV += args[i].getSize();
        }
        super.visitLdcInsn(argTaintPositions.toString());
        super.visitInsn(POP);
        super.visitInsn(POP);

        taintTagFactory.methodEntered(owner, name, descriptor, passThroughMV, lvs, this);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        exceptionHandlers.add(handler);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (!isIgnoreAllInstrumenting && !isRawInstruction) {
            // If accessing an argument, then map it to its taint argument
            int shadowVar = lvs.varToShadowVar.get(var);
            controlFlowPolicy.visitingIncrement(var, shadowVar);
        }
        taintTagFactory.iincOp(var, increment, mv, lvs, this);
        mv.visitIincInsn(var, increment);
    }

    @Override
    public void visitLabel(Label label) {
        if (!isIgnoreAllInstrumenting && Configuration.READ_AND_SAVE_BCI && label instanceof OffsetPreservingLabel) {
            taintTagFactory.insnIndexVisited(((OffsetPreservingLabel) label).getOriginalPosition());
        }
        if (exceptionHandlers.contains(label)) {
            isAtStartOfExceptionHandler = true;
        }
        super.visitLabel(label);
    }

    private static Object[] replaceArraysWithWrappedTypes(Object[] in) {
        boolean makeChange = false;
        for (Object eachEntry : in) {
            if (eachEntry instanceof String && ((String) eachEntry).charAt(0) == '[') {
                makeChange = true;
                break;
            }
        }
        if (!makeChange) {
            return in;
        }
        Object[] ret = new Object[in.length];
        System.arraycopy(in, 0, ret, 0, in.length);
        for (int i = 0; i < ret.length; i++) {
            Object eachEntry = ret[i];
            if (eachEntry instanceof String && ((String) eachEntry).charAt(0) == '[') {
                ret[i] = TaintUtils.getWrapperType(getTypeForStackType(eachEntry)).getInternalName();
            }
        }
        return ret;
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        //Replace any wrapped types with their wrappers
        local = replaceArraysWithWrappedTypes(local);
        stack = replaceArraysWithWrappedTypes(stack);

        super.visitFrame(type, numLocal, local, numStack, stack);
        if (isAtStartOfExceptionHandler) {
            isAtStartOfExceptionHandler = false;
            controlFlowPolicy.generateEmptyTaint(); // TODO exception reference taint is here
            storeStackTopShadowVar();
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        checkStackForTaints();
        if (isIgnoreAllInstrumenting) {
            super.visitVarInsn(opcode, var);
            return;
        }
        int shadowVar = getShadowVar(var, opcode);
        switch (opcode) {
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                super.visitVarInsn(opcode, var);
                super.visitVarInsn(Opcodes.ALOAD, shadowVar);
                storeStackTopShadowVar();
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                controlFlowPolicy.visitingLocalVariableStore(opcode, var);
                loadStackTopShadowVar();
                super.visitVarInsn(ASTORE, shadowVar);
                super.visitVarInsn(opcode, var);
        }
    }

    private int getShadowVar(int local, int opcode) {
        int shadowVar;
        if (!lvs.varToShadowVar.containsKey(local)) {
            lvs.varToShadowVar.put(local, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_DESC), local));
            NEW_EMPTY_TAINT.delegateVisit(mv);
            shadowVar = lvs.varToShadowVar.get(local);
            super.visitVarInsn(ASTORE, shadowVar);
        }
        shadowVar = lvs.varToShadowVar.get(local);
        return shadowVar;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type descType = Type.getType(desc);
        if (isIgnoreAllInstrumenting) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        if (descType.getSort() == Type.ARRAY && descType.getDimensions() > 1) {
            desc = MultiDArrayUtils.getTypeForType(descType).getDescriptor();
        }
        boolean isIgnoredTaint = Instrumenter.isIgnoredClass(owner);
        //|| Instrumenter.isIgnoredClassWithStubsButNoTracking(owner);
        if (Instrumenter.isUninstrumentedField(owner, name) || isIgnoredTaint) {
            switch (opcode) {
                case GETFIELD:
                case GETSTATIC:
                    // need to turn into a wrapped type
                    super.visitFieldInsn(opcode, owner, name, desc);
                    if (descType.getSort() == Type.ARRAY) {
                        Type wrapperType = TaintUtils.getWrapperType(descType);
                        BOX_IF_NECESSARY.delegateVisit(mv);
                        super.visitTypeInsn(CHECKCAST, wrapperType.getInternalName());
                    } else if (desc.equals("Ljava/lang/Object;")) {
                        BOX_IF_NECESSARY.delegateVisit(mv);
                    }
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                    storeStackTopShadowVar();
                    return;
                case PUTFIELD:
                    // obj val
                    if (descType.getSort() == Type.ARRAY || descType.getSort() == Type.OBJECT) {
                        ENSURE_UNBOXED.delegateVisit(mv);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, descType.getInternalName());
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                    return;
                case PUTSTATIC:
                    if (descType.getSort() == Type.ARRAY || descType.getSort() == Type.OBJECT) {
                        ENSURE_UNBOXED.delegateVisit(mv);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, descType.getInternalName());
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                    return;
            }
        }

        if (opcode == PUTFIELD || opcode == PUTSTATIC) {
            controlFlowPolicy.visitingFieldStore(opcode, owner, name, desc);
        }
        taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, true);
        switch (opcode) {
            case Opcodes.GETSTATIC:
                if (TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    super.visitLdcInsn(new ReferenceArrayTarget(desc));
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD,
                        TaintUtils.getShadowTaintType(descType.getDescriptor()));
                storeStackTopShadowVar();
                break;
            case Opcodes.GETFIELD:
                // [objectref]
                loadStackTopShadowVar();
                super.visitInsn(SWAP);
                super.visitInsn(DUP);
                if (TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    super.visitLdcInsn(new ReferenceArrayTarget(desc));
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                // [taint1 objectref value]
                if (descType.getSize() == 1) {
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitInsn(DUP2_X2);
                    super.visitInsn(POP2);
                }
                // [value taint1 objectref]
                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(desc));
                // [value taint1 taint2]
                controlFlowPolicy.visitingInstanceFieldLoad(owner, name, desc);
                COMBINE_TAGS.delegateVisit(mv);
                // [value taint]
                storeStackTopShadowVar();
                break;
            case Opcodes.PUTSTATIC:
                String shadowType = TaintUtils.getShadowTaintType(desc);
                loadStackTopShadowVar();
                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                if (TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    if (descType.getDimensions() == 1) {
                        super.visitInsn(DUP);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "unwrap", "("
                                        + wrapper.getDescriptor() + ")" + TaintUtils.getUnwrappedType(wrapper).getDescriptor(),
                                false);
                        if (descType.getSort() != Type.OBJECT) {
                            super.visitTypeInsn(CHECKCAST, descType.getInternalName());
                        }
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            case Opcodes.PUTFIELD:
                loadStackTopShadowVar();
                // R V T
                shadowType = TaintUtils.getShadowTaintType(desc);
                if (Type.getType(desc).getSize() == 2) {
                    // R VV T
                    int tmp = lvs.getTmpLV(Type.getType(Configuration.TAINT_TAG_DESC));
                    super.visitVarInsn(ASTORE, tmp);
                    // R VV
                    super.visitInsn(DUP2_X1);
                    // VV R VV
                    super.visitInsn(POP2);
                    // VV R
                    super.visitInsn(DUP_X2);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                    // R R VV
                    super.visitFieldInsn(opcode, owner, name, desc);
                    // R T
                    super.visitVarInsn(ALOAD, tmp);
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                    lvs.freeTmpLV(tmp);
                    // System.exit(-1);
                    return;
                } else {
                    // R V T
                    super.visitInsn(DUP2_X1);
                    // V T R V T
                    super.visitInsn(POP2);
                    // V T R
                    super.visitInsn(DUP_X2);
                    // R V T R
                    super.visitInsn(SWAP);
                    // R V R T
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                    // R V
                    if (TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                        Type wrapper = TaintUtils.getWrapperType(descType);
                        if (descType.getDimensions() == 1) {
                            // Owner ArrayWrapper
                            super.visitInsn(DUP2);
                            // Owner ArrayWrapper Owner ArrayWrapper
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "unwrap", "("
                                            + wrapper.getDescriptor() + ")"
                                            + TaintUtils.getUnwrappedType(wrapper).getDescriptor(),
                                    false);
                            if (descType.getElementType().getSort() == Type.OBJECT) {
                                super.visitTypeInsn(CHECKCAST, descType.getInternalName());
                            }
                            super.visitFieldInsn(opcode, owner, name, desc);
                        }
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD,
                                wrapper.getDescriptor());
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        checkStackForTaints();
        if (isIgnoreAllInstrumenting) {
            super.visitIntInsn(opcode, operand);
            return;
        }
        switch (opcode) {
            case TaintUtils.IGNORE_EVERYTHING:
                isIgnoreAllInstrumenting = true;
                break;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                super.visitIntInsn(opcode, operand);
                controlFlowPolicy.generateEmptyTaint();
                storeStackTopShadowVar();
                break;
            case Opcodes.NEWARRAY:
                int lvWithLengthTaint = lvs.getStackShadowVarFromTop(0);
                super.visitIntInsn(opcode, operand);
                super.visitVarInsn(ALOAD, lvWithLengthTaint);
                String arType = MultiDArrayUtils.getTaintArrayInternalName(operand);
                String arrayDescriptor = MultiDArrayUtils.getArrayDescriptor(operand);
                super.visitMethodInsn(INVOKESTATIC, arType, "factory",
                        "(" + arrayDescriptor + Configuration.TAINT_TAG_DESC + ")L" + arType + ";", false);
                controlFlowPolicy.generateEmptyTaint();
                storeStackTopShadowVar();
                break;
            default:
                throw new IllegalArgumentException();
        }
        checkStackForTaints();
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (isIgnoreAllInstrumenting) {
            super.visitMultiANewArrayInsn(desc, dims);
            return;
        }
        Type arrayType = Type.getType(desc);
        StringBuilder methodToCall = new StringBuilder("MULTIANEWARRAY_");
        StringBuilder descToCall = new StringBuilder("(");
        int firstArgOffsetFromBottom = analyzer.stack.size() - dims;
        pushPhosphorStackFrame();
        for (int i = 0; i < dims; i++) {
            descToCall.append('I');
            super.visitInsn(DUP);
            super.visitVarInsn(ALOAD, lvs.getStackShadowVar(firstArgOffsetFromBottom + i));
            push(i);
            SET_ARG_TAINT.delegateVisit(mv);
        }
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

        controlFlowPolicy.generateEmptyTaint(); // TODO array reference taint?
        storeStackTopShadowVar();
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof ReferenceArrayTarget) {
            this.referenceArrayTarget = (ReferenceArrayTarget) cst;
        } else if (cst instanceof PhosphorInstructionInfo) {
            controlFlowPolicy.visitingPhosphorInstructionInfo((PhosphorInstructionInfo) cst);
            super.visitLdcInsn(cst);
        } else {
            super.visitLdcInsn(cst);
            if (!isIgnoreAllInstrumenting) {
                controlFlowPolicy.generateEmptyTaint();
                storeStackTopShadowVar();
            }
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        checkStackForTaints();
        if (isIgnoreAllInstrumenting) {
            super.visitTypeInsn(opcode, type);
            return;
        }
        switch (opcode) {
            case Opcodes.ANEWARRAY:
                if (!Configuration.WITHOUT_PROPAGATION) {
                    Type t = Type.getObjectType(type);
                    if (t.getSort() == Type.ARRAY) {
                        type = TaintUtils.getWrapperType(t).getInternalName();
                    }
                    int lvWithTaint = lvs.getStackShadowVarFromTop(0);
                    super.visitTypeInsn(opcode, type);
                    super.visitVarInsn(ALOAD, lvWithTaint);
                    // 2D arrays are just 1D arrays, not wrapped 1Darrays
                    Type arType = Type.getType(TaggedReferenceArray.class);
                    super.visitMethodInsn(INVOKESTATIC, arType.getInternalName(), "factory",
                            "([Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")" + arType.getDescriptor(),
                            false);
                    // TODO what should we make the reference taint be here? how shoudl we use the
                    // array length?
                    controlFlowPolicy.generateEmptyTaint();
                    storeStackTopShadowVar();
                }
                break;
            case Opcodes.NEW:
                super.visitTypeInsn(opcode, type);
                controlFlowPolicy.generateEmptyTaint();
                storeStackTopShadowVar();
                break;
            case Opcodes.CHECKCAST:
                checkCast(type);
                break;
            case Opcodes.INSTANCEOF:
                instanceOf(type);
                break;
            default:
                throw new IllegalArgumentException();
        }
        checkStackForTaints();
    }

    private void instanceOf(String type) {
        // [ref]
        Type t = Type.getObjectType(type);
        if (TaintUtils.isWrappedType(t)) {
            if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT) {
                // Need to get the underlying type
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                super.visitTypeInsn(INSTANCEOF, type);
                // [z]
                controlFlowPolicy.visitingInstanceOf();
                return;
            }
            type = TaintUtils.getWrapperType(t).getInternalName();
        }
        super.visitTypeInsn(INSTANCEOF, type);
        controlFlowPolicy.visitingInstanceOf();
    }

    private void checkCast(String type) {
        Type t = Type.getObjectType(type);
        if (TaintUtils.isWrappedType(t)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, TaintUtils.getWrapperType(t).getInternalName());
        } else {
            super.visitTypeInsn(CHECKCAST, type);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {


        Type[] args = Type.getArgumentTypes(desc);
        int nParams = args.length;
        // load taint tags into stack frame
        for (int i = 0; i < nParams; i++) {
            int varWithTagForThisVar = lvs.getStackShadowVarFromTop(nParams - i - 1);
            pushPhosphorStackFrame();
            super.visitVarInsn(ALOAD, varWithTagForThisVar);
            push(i);
            SET_ARG_TAINT.delegateVisit(mv);
        }
        unwrapArraysForCallTo(owner, name, desc);
        pushPhosphorStackFrame();
        if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
            super.visitLdcInsn(name + desc.substring(0, 1 + desc.indexOf(')')));
            PREPARE_FOR_CALL_DEBUG.delegateVisit(mv);
        } else {
            push(PhosphorStackFrame.hashForDesc(name + desc.substring(0, 1 + desc.indexOf(')'))));
            PREPARE_FOR_CALL_FAST.delegateVisit(mv);
        }
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        Type returnType = Type.getReturnType(desc);
        if (returnType.getSort() != Type.VOID) {
            // retrieve taint tags
            pushPhosphorStackFrame();
            GET_RETURN_TAINT.delegateVisit(mv);
            storeStackTopShadowVar();
        }
        if (TaintUtils.isWrappedType(returnType)) {
            pushPhosphorStackFrame();
            super.visitInsn(SWAP);
            TaintMethodRecord.getReturnWrapperMethod(returnType).delegateVisit(mv);
            this.visitTypeInsn(CHECKCAST, returnType.getInternalName());
        }
    }

    public static void defensivelyCopyControlStack(GeneratorAdapter ga) {
        if ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
            Type[] instArgTypes = ga.getArgumentTypes();
            for (int i = 0; i < instArgTypes.length; i++) {
                if (instArgTypes[i].equals(CONTROL_STACK_TYPE)) {
                    ga.loadArg(i);
                    CONTROL_STACK_COPY_TOP.delegateVisit(ga);
                    ga.storeArg(i);
                    return;
                }
            }
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        controlFlowPolicy.visitingMaxs();
        if (rewriteLVDebug) {
            Label end = new Label();
            super.visitLabel(end);
        }
        super.visitMaxs(0, maxLocals);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (isIgnoreAllInstrumenting || isRawInstruction) {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            return;
        }
        if (owner.equals("java/lang/Object") && name.equals("clone")) {
            //On VERY OLD (e.g. 1.1) class files, we aren't allowed to use java/lang/object as owner for clone on a reference array...
            Type onStack = getTopOfStackType();
            if (TaintUtils.isWrapperType(onStack)) {
                owner = onStack.getInternalName();
            }
        }
        if (name.equals("getProperty") && this.owner.equals("org/eclipse/jdt/core/tests/util/Util")) {
            // Workaround for eclipse benchmark
            owner = Type.getInternalName(ReflectionMasker.class);
            name = "getPropertyHideBootClasspath";
        }

        /*
        Some methods should never get a stack frame prepared - in particular, a call to
        a native or intrinsic method that will then immediately jump to the method that
        some prior caller was trying to reach. In particular, this clearly is needed for
        MethodHandles.linkTo*, which are used by, e.g. VarHandles.
         */
        boolean usePrevFrameInsteadOfPreparingNew = false;
        if(className.startsWith("java/lang/invoke") && owner.equals("java/lang/invoke/MethodHandle") && name.startsWith("linkTo")){
            usePrevFrameInsteadOfPreparingNew = true;
        }
        /*
        When creating a new primitive wrapper (java.lang.Integer etc), assign the reference taint
        for the new object to be the taint of the value being wrapped
            TODO can we refactor this to somewhere that makes more sense?
         */
        if (((owner.equals(INTEGER_NAME) || owner.equals(BYTE_NAME) ||
                owner.equals(BOOLEAN_NAME)
                || owner.equals(CHARACTER_NAME)
                || owner.equals(SHORT_NAME) || owner.equals(LONG_NAME) ||
                owner.equals(FLOAT_NAME)
                || owner.equals(DOUBLE_NAME))) && name.equals("<init>")) {
            Type[] args = Type.getArgumentTypes(desc);
            if (args.length == 1 && args[0].getSort() != Type.OBJECT) {
                // [uninitThis (boxed type), val (primitive)]
                int primitiveSize = args[0].getSize();
                // Check that a duplicate of boxed type being initialized is actually on the
                // stack
                if (analyzer.stack.size() >= 2 + primitiveSize
                        && analyzer.stack.get(analyzer.stack.size() - (1 + primitiveSize)) instanceof
                        Label
                        && analyzer.stack.get(analyzer.stack.size() - (2 + primitiveSize)) ==
                        analyzer.stack
                                .get(analyzer.stack.size() - (1 + primitiveSize))) {
                    int lvWithSourceTaint = lvs.getStackShadowVarFromTop(0);
                    int destOne = lvs.getStackShadowVarFromTop(1);
                    int destTwo = lvs.getStackShadowVarFromTop(2);
                    super.visitVarInsn(ALOAD, lvWithSourceTaint);
                    super.visitInsn(DUP);
                    super.visitVarInsn(ASTORE, destOne);
                    super.visitVarInsn(ASTORE, destTwo);
                }
            }
        }

        Type ownerType = Type.getObjectType(owner);
        //TODO what is this doing now?
        if (owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints")
                && !name.equals("hasNoDependencies") && !desc.equals("(I)V") && !owner.endsWith("Tainter")
                && !owner.endsWith("CharacterUtils")
                && !name.equals("getPHOSPHOR_TAG") && !name.equals("setPHOSPHOR_TAG")
                && !owner.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator")
                && !owner.equals(Type.getInternalName(PowerSetTree.class))
                && !owner.equals("edu/columbia/cs/psl/phosphor/util/IgnoredTestUtil")
                && !owner.equals(Configuration.TAINT_TAG_INTERNAL_NAME)
                && !owner.equals(TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME)) {
            taintTagFactory.methodOp(opcode, owner, name, desc, isInterface, mv, lvs, this);
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if (Type.getReturnType(desc).getSort() != Type.VOID) {
                NEW_EMPTY_TAINT.delegateVisit(mv);
                storeStackTopShadowVar();
            }
            return;
        }
        if (opcode == INVOKEVIRTUAL && TaintUtils.isWrappedType(ownerType)) {
            owner = MultiDArrayUtils.getTypeForType(ownerType).getInternalName();
        }
        if (opcode == INVOKEVIRTUAL && name.equals("getClass") && desc.equals("()Ljava/lang/Class;")) {
            if (isObjOutputStream) {
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            } else {
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(NativeHelper.class), "getClassOrWrapped",
                        "(Ljava/lang/Object;)Ljava/lang/Class;", false);
            }
            return;
        }
        if ((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem")
                || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
                && !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
            owner = Type.getInternalName(TaintUtils.class);
        }
        Type[] args = Type.getArgumentTypes(desc);
        int nParams = args.length;

        boolean isStaticCall = (opcode == Opcodes.INVOKESTATIC);
        if (!isStaticCall) {
            int varWithThisTag = lvs.getStackShadowVarFromTop(nParams);
            pushPhosphorStackFrame();
            super.visitVarInsn(ALOAD, varWithThisTag);
            push(0);
            SET_ARG_TAINT.delegateVisit(mv);
        }
        // load taint tags into stack frame
        for (int i = 0; i < nParams; i++) {
            int varWithTagForThisVar = lvs.getStackShadowVarFromTop(nParams - i - 1);
            pushPhosphorStackFrame();
            super.visitVarInsn(ALOAD, varWithTagForThisVar);
            push(i + (isStaticCall ? 0 : 1));
            SET_ARG_TAINT.delegateVisit(mv);
        }
        unwrapArraysForCallTo(owner, name, desc);

        pushPhosphorStackFrame();
        if (usePrevFrameInsteadOfPreparingNew) {
            PREPARE_FOR_CALL_PREV.delegateVisit(mv);
        } else {
            String methodKey = getMethodKeyForStackFrame(name, desc, Instrumenter.isPolymorphicSignatureMethod(owner, name));
            if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                mv.visitLdcInsn(methodKey);
                PREPARE_FOR_CALL_DEBUG.delegateVisit(mv);
            } else {
                push(PhosphorStackFrame.hashForDesc(methodKey));
                PREPARE_FOR_CALL_FAST.delegateVisit(mv);
            }
        }

        if (isBoxUnboxMethodToWrap(owner, name)) {
            if (name.equals("valueOf")) {
                switch (owner) {
                    case BYTE_NAME:
                        name = "valueOfB";
                        break;
                    case BOOLEAN_NAME:
                        name = "valueOfZ";
                        break;
                    case CHARACTER_NAME:
                        name = "valueOfC";
                        break;
                    case SHORT_NAME:
                        name = "valueOfS";
                        break;
                    case INTEGER_NAME:
                        name = "valueOfI";
                        break;
                    case FLOAT_NAME:
                        name = "valueOfF";
                        break;
                    case LONG_NAME:
                        name = "valueOfJ";
                        break;
                    case DOUBLE_NAME:
                        name = "valueOfD";
                        break;
                    default:
                        throw new UnsupportedOperationException(owner);
                }
            }
            if (opcode != INVOKESTATIC) {
                opcode = INVOKESTATIC;
                desc = "(L" + owner + ";" + desc.substring(1);
            }
            owner = "edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator";
        }
        if (owner.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator")) {
            desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv); //TODO refactor these valueOf things to the reflection hiding MV
        }

        taintTagFactory.methodOp(opcode, owner, name, desc, isInterface, mv, lvs, this);
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);

        Type returnType = Type.getReturnType(desc);
        pushPhosphorStackFrame();
        GET_RETURN_TAINT.delegateVisit(mv);
        if (returnType.getSort() != Type.VOID) {
            // retrieve taint tags
            storeStackTopShadowVar();
        } else {
            super.visitInsn(POP);
        }
        if (TaintUtils.isWrappedType(returnType)) {
            pushPhosphorStackFrame();
            super.visitInsn(SWAP);
            TaintMethodRecord.getReturnWrapperMethod(returnType).delegateVisit(mv);
            this.visitTypeInsn(CHECKCAST, returnType.getInternalName());
        }
    }

    private void checkStackForTaints() {
        //if(analyzer.stack != null)
        //for(Object o : analyzer.stack){
        //    if(o.equals(Configuration.TAINT_TAG_INTERNAL_NAME))
        //        throw new IllegalStateException();
        //}
    }

    @Override
    public void visitInsn(int opcode) {
        checkStackForTaints();
        if (opcode == TaintUtils.RAW_INSN) {
            isRawInstruction = !isRawInstruction;
        } else if (opcode == TaintUtils.IGNORE_EVERYTHING) {
            isIgnoreAllInstrumenting = !isIgnoreAllInstrumenting;
            taintTagFactory.signalOp(opcode, null);
            super.visitInsn(opcode);
        } else if (opcode == TaintUtils.NO_TAINT_STORE_INSN) {
            isTaintlessArrayStore = true;
        } else if (isIgnoreAllInstrumenting || isRawInstruction || opcode == NOP
                || opcode == TaintUtils.FOLLOWED_BY_FRAME) {
            super.visitInsn(opcode);
        } else if (OpcodesUtil.isArrayLoad(opcode)) {
            visitArrayLoad(opcode);
        } else if (OpcodesUtil.isArrayStore(opcode)) {
            visitArrayStore(opcode);
        } else if (OpcodesUtil.isPushConstantOpcode(opcode)) {
            super.visitInsn(opcode);
            controlFlowPolicy.generateEmptyTaint();
            storeStackTopShadowVar();
        } else if (OpcodesUtil.isReturnOpcode(opcode)) {
            visitReturn(opcode);
        } else if (OpcodesUtil.isArithmeticOrLogicalInsn(opcode) || opcode == ARRAYLENGTH) {
            taintTagFactory.stackOp(opcode, mv, lvs, this);
        } else if (opcode >= POP && opcode <= SWAP) {
            visitPopDupOrSwap(opcode);
        } else if (opcode == MONITORENTER || opcode == MONITOREXIT) {
            super.visitInsn(opcode);
        } else if (opcode == ATHROW) {
            popPhosphorStackFrameIfNeeded();
            controlFlowPolicy.onMethodExit(opcode);
            super.visitInsn(opcode);
        } else {
            throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        checkStackForTaints();
    }

    /**
     * @param opcode the opcode of the instruction originally to be visited either
     *               POP, POP2, DUP, DUP2, DUP_X1, DUP_X2,
     *               DUP2_X1, or SWAP
     */
    private void visitPopDupOrSwap(int opcode) {
        switch (opcode) {
            case POP:
            case POP2:
                super.visitInsn(opcode);
                break;
            case DUP:
                int stackShadowVar = lvs.getStackShadowVarFromTop(0);
                super.visitInsn(opcode);
                super.visitVarInsn(ALOAD, stackShadowVar);
                super.visitVarInsn(ASTORE, stackShadowVar + 1);
                break;
            case DUP2:
                Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
                if (getStackElementSize(topOfStack) == 1) {
                    stackShadowVar = lvs.getStackShadowVarFromTop(0);
                    int stackShadowVar2 = lvs.getStackShadowVarFromTop(1);
                    super.visitInsn(opcode);
                    super.visitVarInsn(ALOAD, stackShadowVar);
                    super.visitVarInsn(ASTORE, stackShadowVar + 2);
                    super.visitVarInsn(ALOAD, stackShadowVar2);
                    super.visitVarInsn(ASTORE, stackShadowVar2 + 2);
                } else {
                    stackShadowVar = lvs.getStackShadowVarFromTop(0);
                    super.visitInsn(opcode);
                    super.visitVarInsn(ALOAD, stackShadowVar);
                    super.visitVarInsn(ASTORE, stackShadowVar + 1);
                }
                break;
            case DUP_X1:
                // A B -> B A B
                DUPN_XU(1, 1);
                super.visitInsn(opcode);
                break;
            case DUP_X2:
                // X?X? V -> VXX?VT
                if (getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 2)) == 2) {
                    // With long/double under - effectively DUP_X1 for taint propogation
                    // XX V -> V XX V
                    DUPN_XU(1, 1);
                } else {
                    // With 2 x 1 word under
                    // X Y V -> V X Y V
                    DUPN_XU(1, 2);
                }
                super.visitInsn(opcode);
                break;
            case DUP2_X1:
                topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
                if (getStackElementSize(topOfStack) == 1) {
                    DUPN_XU(2, 1);
                } else {
                    DUPN_XU(1, 1);
                }
                super.visitInsn(opcode);
                break;
            case DUP2_X2:
                topOfStack = analyzer.stack.get(analyzer.stack.size() - 1);
                if (getStackElementSize(topOfStack) == 1) {
                    // Second must be a single word
                    // ??TBTCT ->
                    Object third = analyzer.stack.get(analyzer.stack.size() - 3);
                    if (getStackElementSize(third) == 1) {
                        DUPN_XU(2, 2);
                    } else {
                        // Two single words above a double word
                        DUPN_XU(2, 1);
                    }
                } else {
                    // Top is 2 words
                    Object third = analyzer.stack.get(analyzer.stack.size() - 2);
                    if (getStackElementSize(third) == 1) {
                        DUPN_XU(1, 2);
                    } else {
                        DUPN_XU(1, 1);
                    }
                }
                super.visitInsn(opcode);
                break;
            case Opcodes.SWAP:
                stackShadowVar = lvs.getStackShadowVarFromTop(0);
                super.visitVarInsn(ALOAD, stackShadowVar);
                super.visitVarInsn(ALOAD, stackShadowVar - 1);
                super.visitVarInsn(ASTORE, stackShadowVar);
                super.visitVarInsn(ASTORE, stackShadowVar - 1);
                super.visitInsn(opcode);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void popPhosphorStackFrameIfNeeded() {
        pushPhosphorStackFrame();
        super.visitVarInsn(ILOAD, this.lvs.getLocalVariableAdder().getIndexOfStackDataNeedsPoppingLV());
        POP_STACK_FRAME.delegateVisit(mv);
    }

    /**
     * stack_pre = [value taint] or [] if opcode is RETURN
     * stack_post = []
     *
     * @param opcode the opcode of the instruction originally to be visited either
     *               RETURN, ARETURN, IRETURN, DRETURN,
     *               FRETURN, or LRETURN
     */
    private void visitReturn(int opcode) {
        if (this.className.equals("java/lang/Thread") && name.equals("<clinit>")) {
            START_STACK_FRAME_TRACKING.delegateVisit(mv);
        }
        controlFlowPolicy.onMethodExit(opcode);
        popPhosphorStackFrameIfNeeded();
        if (opcode == RETURN) {
            taintTagFactory.stackOp(opcode, mv, lvs, this);
            super.visitInsn(opcode);
            return;
        }

        pushReturnTaint();
        Type returnType = Type.getReturnType(this.descriptor);
        if (TaintUtils.isWrappedType(returnType) && !topOfStackIsNull()) {
            //Need to return the unboxed, and save this for later
            super.visitInsn(DUP);

            //Save for later
            pushPhosphorStackFrame();
            super.visitInsn(SWAP);
            SET_WRAPPED_RETURN.delegateVisit(mv);

            Type t = getTopOfStackType();
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
        taintTagFactory.stackOp(opcode, mv, lvs, this);
        super.visitInsn(opcode);
    }

    /**
     * stack_pre = [arrayref, reference-taint, index, index-taint, value,
     * value-taint]
     * stack_post = []
     *
     * @param opcode the opcode of the instruction originally to be visited, either
     *               IASTORE, LASTORE,
     *               FASTORE,DASTORE, BASTORE, CASTORE, SASTORE, or AASTORE.
     */
    private void visitArrayStore(int opcode) {
        // A I V
        controlFlowPolicy.visitingArrayStore(opcode);
        int valuePosition = analyzer.stack.size() - (opcode == LASTORE || opcode == DASTORE ? 2 : 1);
        int indexPosition = valuePosition - 1;
        int arrayRefPosition = indexPosition - 1;
        MethodRecord setMethod;
        if (analyzer.stack.get(arrayRefPosition) == Opcodes.NULL) {
            setMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, "");
        } else {
            String arrayReferenceType = (String) analyzer.stack.get(arrayRefPosition);
            if (arrayReferenceType.startsWith("[") || !arrayReferenceType.contains("Tagged")) {
                throw new IllegalStateException("Calling XASTORE on " + arrayReferenceType);
            }
            setMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, arrayReferenceType);
        }
        int idxTaint = lvs.getStackShadowVarFromTop(1);
        int valueTaint = lvs.getStackShadowVarFromTop(0);
        super.visitVarInsn(ALOAD, idxTaint);
        super.visitVarInsn(ALOAD, valueTaint);
        pushPhosphorStackFrame();
        setMethod.delegateVisit(mv);
        isTaintlessArrayStore = false;
    }

    /**
     * stack_pre = [arrayref, index]
     * stack_post = [value]
     *
     * @param opcode the opcode of the instruction originally to be visited, either
     *               LALOAD, DALOAD,
     *               IALOAD, FALOAD, BALOAD, CALOAD, SALOAD, or AALOAD.
     */
    private void visitArrayLoad(int opcode) {
        int arrayRefPosition = analyzer.stack.size() - 2;
        MethodRecord getMethod;
        if (analyzer.stack.get(arrayRefPosition) == Opcodes.NULL) {
            getMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, "");
        } else {
            String arrayReferenceType = (String) analyzer.stack.get(arrayRefPosition);
            if (arrayReferenceType.startsWith("[") || !arrayReferenceType.contains("Tagged")) {
                throw new IllegalStateException("Calling XALOAD on " + arrayReferenceType);
            }
            getMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, arrayReferenceType);
        }
        loadStackTopShadowVar();
        pushPhosphorStackFrame();
        getMethod.delegateVisit(mv);
        if (Object.class.equals(getMethod.getReturnType()) && referenceArrayTarget != null) {
            Type originalArrayType = Type.getType(referenceArrayTarget.getOriginalArrayType());
            String castTo = Type.getType(originalArrayType.getDescriptor().substring(1)).getInternalName();
            if (originalArrayType.getDimensions() == 2) {
                castTo = TaintUtils.getWrapperType(Type.getType(castTo)).getInternalName();
            } else if (originalArrayType.getDimensions() > 2) {
                castTo = Type.getInternalName(TaggedReferenceArray.class);
            }
            super.visitTypeInsn(CHECKCAST, castTo);
        }
        retrieveReturnTaint();
        storeStackTopShadowVar();

        // controlFlowPolicy.visitingArrayLoad(opcode); //TODO why is this here and not
        // in the tainted array load method?
        // COMBINE_TAGS.delegateVisit(mv);//TODO why is this here and not in the tainted
        // array load method?
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label[] labels) {
        if (!isIgnoreAllInstrumenting) {
            taintTagFactory.tableSwitch(min, max, defaultLabel, labels, mv, lvs, this);
            controlFlowPolicy.visitTableSwitch(min, max, defaultLabel, labels);
        }
        super.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultLabel, int[] keys, Label[] labels) {
        if (!isIgnoreAllInstrumenting) {
            taintTagFactory.lookupSwitch(defaultLabel, keys, labels, mv, lvs, this);
            controlFlowPolicy.visitLookupSwitch(defaultLabel, keys, labels);
        }
        super.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (!isIgnoreAllInstrumenting) {
            unboxForReferenceCompare(opcode);
            taintTagFactory.jumpOp(opcode, label, mv, lvs, this);
            controlFlowPolicy.visitingJump(opcode, label);
        }
        super.visitJumpInsn(opcode, label);
    }

    private void unboxForReferenceCompare(int opcode) {
        if ((opcode == IF_ACMPEQ || opcode == IF_ACMPNE) && Configuration.WITH_UNBOX_ACMPEQ
                && !owner.equals("java/io/ObjectOutputStream$HandleTable")) {
            // v1 v2
            ENSURE_UNBOXED.delegateVisit(mv);
            super.visitInsn(SWAP);
            // v2 v1
            ENSURE_UNBOXED.delegateVisit(mv);
            super.visitInsn(SWAP);
            // v1 v2
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        this.line = line;
        taintTagFactory.lineNumberVisited(line);
    }

    /**
     * Returns whether a class with the specified name is used by Phosphor for
     * "internal" tainting. Calls to methods in
     * internal tainting classes from instrumented classes are remapped to the
     * appropriate "$$PHOSPHORTAGGED" version
     * even if the internal tainting class is not instrumented by Phosphor. This
     * requires internal tainting classes to
     * provide instrumented versions of any method that may be invoked by a classes
     * that is instrumented by Phosphor.
     *
     * @param owner the name of class being checking
     * @return true if a class with the specified name is used by Phosphor for
     *         internal tainting
     * @see MultiTainter
     */
    private static boolean isInternalTaintingClass(String owner) {
        return owner.startsWith("edu/columbia/cs/psl/phosphor/runtime/")
                || taintTagFactory.isInternalTaintingClass(owner)
                || (controlFlowManager != null && controlFlowManager.isInternalTaintingClass(owner))
                || owner.startsWith("edu/gmu/swe/phosphor/ignored/runtime/");
    }

    private static boolean isBoxUnboxMethodToWrap(String owner, String name) {
        if ((owner.equals(INTEGER_NAME) || owner.equals(BYTE_NAME) || owner.equals(BOOLEAN_NAME)
                || owner.equals(CHARACTER_NAME)
                || owner.equals(SHORT_NAME) || owner.equals(LONG_NAME) || owner.equals(FLOAT_NAME)
                || owner.equals(DOUBLE_NAME))
                && (name.equals("toString") || name.equals("toHexString") || name.equals("toOctalString")
                || name.equals("toBinaryString")
                || name.equals("toUnsignedString") || name.equals("valueOf"))) {
            return true;
        }
        switch (owner) {
            case BOOLEAN_NAME:
                return name.equals("parseBoolean") || name.equals("booleanValue");
            case BYTE_NAME:
                return name.equals("parseByte") || name.equals("byteValue");
            case CHARACTER_NAME:
                return name.equals("digit") || name.equals("forDigit") || name.equals("charValue");
            case DOUBLE_NAME:
                return name.equals("parseDouble") || name.equals("doubleValue");
            case FLOAT_NAME:
                return name.equals("parseFloat") || name.equals("floatValue");
            case INTEGER_NAME:
                return name.equals("getChars") || name.equals("parseInt") || name.equals("parseUnsignedInt")
                        || name.equals("intValue");
            case LONG_NAME:
                return name.equals("getChars") || name.equals("parseLong") || name.equals("parseUnsignedLong")
                        || name.equals("longValue");
            case SHORT_NAME:
                return name.equals("parseShort") || name.equals("shortValue");
            default:
                return false;
        }
    }

    public static Type[] calculateParamTypes(boolean isStatic, String descriptor) {
        Type[] newArgTypes = Type.getArgumentTypes(descriptor);
        int lastArg = isStatic ? 0 : 1; // If non-static, then arg[0] = this
        for (Type t : newArgTypes) {
            lastArg += t.getSize();
        }
        Type[] paramTypes = new Type[lastArg + 1];
        int n = (isStatic ? 0 : 1);
        for (Type newArgType : newArgTypes) {
            paramTypes[n] = newArgType;
            n += newArgType.getSize();
        }
        return paramTypes;
    }

}
