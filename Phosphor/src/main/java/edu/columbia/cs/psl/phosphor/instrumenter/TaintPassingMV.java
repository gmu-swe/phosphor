package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.NativeHelper;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class TaintPassingMV extends TaintAdapter implements Opcodes {

    static final String BYTE_NAME = "java/lang/Byte";
    static final String BOOLEAN_NAME = "java/lang/Boolean";
    static final String INTEGER_NAME = "java/lang/Integer";
    static final String FLOAT_NAME = "java/lang/Float";
    static final String LONG_NAME = "java/lang/Long";
    static final String CHARACTER_NAME = "java/lang/Character";
    static final String DOUBLE_NAME = "java/lang/Double";
    static final String SHORT_NAME = "java/lang/Short";
    private final int lastArg;
    private final Type[] paramTypes;
    private final Type originalMethodReturnType;
    private final Type newReturnType;
    private final String name;
    private final boolean isStatic;
    private final String owner;
    private final String descriptor;
    private final MethodVisitor passThroughMV;
    private final boolean rewriteLVDebug;
    private final boolean isLambda;
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

    public TaintPassingMV(MethodVisitor mv, int access, String owner, String name, String descriptor, String signature,
                          String[] exceptions, String originalDesc, NeverNullArgAnalyzerAdapter analyzer,
                          MethodVisitor passThroughMV, LinkedList<MethodNode> wrapperMethodsToAdd,
                          ControlFlowPropagationPolicy controlFlowPolicy) {
        super(access, owner, name, descriptor, signature, exceptions, mv, analyzer);
        Configuration.taintTagFactory.instrumentationStarting(access, name, descriptor);
        this.isLambda = this.isIgnoreAllInstrumenting = owner.contains("$Lambda$");
        this.name = name;
        this.owner = owner;
        this.wrapperMethodsToAdd = wrapperMethodsToAdd;
        this.rewriteLVDebug = owner.equals("java/lang/invoke/MethodType");
        this.passThroughMV = passThroughMV;
        this.descriptor = descriptor;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.isObjOutputStream = (owner.equals("java/io/ObjectOutputStream") && name.startsWith("writeObject0"))
                || (owner.equals("java/io/ObjectInputStream") && name.startsWith("defaultReadFields"));
        this.paramTypes = calculateParamTypes(isStatic, descriptor);
        this.lastArg = paramTypes.length - 1;
        this.originalMethodReturnType = Type.getReturnType(originalDesc);
        this.newReturnType = Type.getReturnType(descriptor);
        this.controlFlowPolicy = controlFlowPolicy;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        Configuration.taintTagFactory.methodEntered(owner, name, descriptor, passThroughMV, lvs, this);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        exceptionHandlers.add(handler);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if(!isIgnoreAllInstrumenting && !isRawInstruction) {
            // If accessing an argument, then map it to its taint argument
            int shadowVar = var < lastArg && TaintUtils.isShadowedType(paramTypes[var]) ? var + 1 : lvs.varToShadowVar.get(var);
            controlFlowPolicy.visitingIncrement(var, shadowVar);
        }
        Configuration.taintTagFactory.iincOp(var, increment, mv, lvs, this);
        mv.visitIincInsn(var, increment);
    }

    @Override
    public void visitLabel(Label label) {
        if(!isIgnoreAllInstrumenting && Configuration.READ_AND_SAVE_BCI && label instanceof OffsetPreservingLabel) {
            Configuration.taintTagFactory.insnIndexVisited(((OffsetPreservingLabel) label).getOriginalPosition());
        }
        if(exceptionHandlers.contains(label)) {
            isAtStartOfExceptionHandler = true;
        }
        super.visitLabel(label);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
        if(isAtStartOfExceptionHandler) {
            isAtStartOfExceptionHandler = false;
            controlFlowPolicy.generateEmptyTaint(); //TODO exception reference taint is here
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if(isIgnoreAllInstrumenting) {
            super.visitVarInsn(opcode, var);
            return;
        }
        int shadowVar = getShadowVar(var, opcode);
        switch(opcode) {
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                super.visitVarInsn(opcode, var);
                super.visitVarInsn(ALOAD, shadowVar);
                if(getTopOfStackObject() == Opcodes.TOP) {
                    throw new IllegalStateException();
                }
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                controlFlowPolicy.visitingLocalVariableStore(opcode, var);
                super.visitVarInsn(ASTORE, shadowVar);
                super.visitVarInsn(opcode, var);
        }
    }

    private int getShadowVar(int local, int opcode) {
        int shadowVar;
        if(local == 0 && !isStatic) {
            // Accessing "this" so no-op, die here so we never have to worry about uninitialized this later on.
            shadowVar = 1;
        } else if(local < lastArg && paramTypes[local] != null && TaintUtils.getShadowTaintType(paramTypes[local].getDescriptor()) != null) {
            // Accessing an arg; remap it
            shadowVar = local + 1;
            if(opcode == LLOAD || opcode == LSTORE || opcode == DSTORE || opcode == DLOAD) {
                shadowVar++;
            }
        } else {
            // Not accessing an arg
            if(!lvs.varToShadowVar.containsKey(local)) {
                lvs.varToShadowVar.put(local, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_DESC), local));
            }
            shadowVar = lvs.varToShadowVar.get(local);
        }
        return shadowVar;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type descType = Type.getType(desc);
        if(isIgnoreAllInstrumenting) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        if(descType.getSort() == Type.ARRAY && descType.getDimensions() > 1) {
            desc = MultiDTaintedArray.getTypeForType(descType).getDescriptor();
        }
        boolean isIgnoredTaint = Instrumenter.isIgnoredClass(owner) || Instrumenter.isIgnoredClassWithStubsButNoTracking(owner);
        if(Instrumenter.isUninstrumentedField(owner, name) || isIgnoredTaint) {
            switch(opcode) {
                case GETFIELD:
                    super.visitInsn(POP);
                case GETSTATIC:
                    //need to turn into a wrapped type
                    super.visitFieldInsn(opcode, owner, name, desc);
                    if(descType.getSort() == Type.ARRAY) {
                        Type wrapperType = TaintUtils.getWrapperType(descType);
                        BOX_IF_NECESSARY.delegateVisit(mv);
                        super.visitTypeInsn(CHECKCAST, wrapperType.getInternalName());
                    } else if(desc.equals("Ljava/lang/Object;")) {
                        BOX_IF_NECESSARY.delegateVisit(mv);
                    }
                    NEW_EMPTY_TAINT.delegateVisit(mv);
                    return;
                case PUTFIELD:
                    //obj taint val taint
                    super.visitInsn(POP);
                    //obj taint val
                    if(descType.getSort() == Type.ARRAY || descType.getSort() == Type.OBJECT) {
                        ENSURE_UNBOXED.delegateVisit(mv);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, descType.getInternalName());
                    }
                    if(descType.getSize() == 2) {
                        super.visitInsn(DUP2_X1);
                        super.visitInsn(POP2);
                    } else {
                        super.visitInsn(SWAP);
                    }
                    super.visitInsn(POP);
                    super.visitFieldInsn(opcode, owner, name, desc);
                    return;
                case PUTSTATIC:
                    super.visitInsn(POP);
                    if(descType.getSort() == Type.ARRAY || descType.getSort() == Type.OBJECT) {
                        ENSURE_UNBOXED.delegateVisit(mv);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, descType.getInternalName());
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                    return;
            }
        }

        boolean thisIsTracked = TaintUtils.isShadowedType(descType);

        if(opcode == PUTFIELD || opcode == PUTSTATIC) {
            controlFlowPolicy.visitingFieldStore(opcode, owner, name, desc);
        }
        Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, thisIsTracked);
        switch(opcode) {
            case Opcodes.GETSTATIC:
                if(TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    super.visitLdcInsn(new ReferenceArrayTarget(desc));
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                if(TaintUtils.isShadowedType(descType)) {
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(descType.getDescriptor()));
                }
                break;
            case Opcodes.GETFIELD:
                // [objectref taint1]
                super.visitInsn(SWAP);
                super.visitInsn(DUP);
                if(TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    super.visitLdcInsn(new ReferenceArrayTarget(desc));
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                // [taint1 objectref value]
                if(descType.getSize() == 1) {
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
                break;
            case Opcodes.PUTSTATIC:
                if(TaintUtils.isShadowedType(descType)) {
                    String shadowType = TaintUtils.getShadowTaintType(desc);
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                }
                if(TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    if(descType.getDimensions() == 1) {
                        super.visitInsn(DUP);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "unwrap", "(" + wrapper.getDescriptor() + ")" + TaintUtils.getUnwrappedType(wrapper).getDescriptor(), false);
                        if(descType.getSort() != Type.OBJECT) {
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
                // RT R V T
                if(TaintUtils.isShadowedType(descType)) {
                    String shadowType = TaintUtils.getShadowTaintType(desc);
                    if(Type.getType(desc).getSize() == 2) {
                        // R T VV T
                        int tmp = lvs.getTmpLV(Type.getType(Configuration.TAINT_TAG_DESC));
                        super.visitVarInsn(ASTORE, tmp);
                        //R T VV
                        super.visitInsn(DUP2_X2);
                        // VV R T VV
                        super.visitInsn(POP2);
                        // VV R T
                        super.visitInsn(POP);
                        //VV R
                        super.visitInsn(DUP_X2);
                        super.visitInsn(DUP_X2);
                        super.visitInsn(POP);
                        //R R VV
                        super.visitFieldInsn(opcode, owner, name, desc);
                        //R T
                        super.visitVarInsn(ALOAD, tmp);
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                        lvs.freeTmpLV(tmp);
                        // System.exit(-1);
                        return;
                    } else {
                        //R T V T
                        super.visitInsn(DUP2_X2);
                        //V T R T V T
                        super.visitInsn(POP2);
                        //V T R T
                        super.visitInsn(POP); //TODO reference taint on field owner?
                        //V T R
                        super.visitInsn(DUP_X2);
                        //R V T R
                        super.visitInsn(SWAP);
                        //R V R T
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                        //T R V R
                    }
                }
                if(TaintUtils.isWrappedTypeWithSeparateField(descType)) {
                    Type wrapper = TaintUtils.getWrapperType(descType);
                    if(descType.getDimensions() == 1) {
                        //Owner ArrayWrapper
                        super.visitInsn(DUP2);
                        //Owner ArrayWrapper Owner ArrayWrapper
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "unwrap", "(" + wrapper.getDescriptor() + ")" + TaintUtils.getUnwrappedType(wrapper).getDescriptor(), false);
                        if(descType.getElementType().getSort() == Type.OBJECT) {
                            super.visitTypeInsn(CHECKCAST, descType.getInternalName());
                        }
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                    super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, wrapper.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if(isIgnoreAllInstrumenting) {
            super.visitIntInsn(opcode, operand);
            return;
        }
        switch(opcode) {
            case TaintUtils.IGNORE_EVERYTHING:
                isIgnoreAllInstrumenting = true;
                break;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                super.visitIntInsn(opcode, operand);
                controlFlowPolicy.generateEmptyTaint();
                break;
            case Opcodes.NEWARRAY:
                super.visitInsn(SWAP);
                super.visitIntInsn(opcode, operand);
                String arType = MultiDTaintedArray.getTaintArrayInternalName(operand);
                String arrayDescriptor = MultiDTaintedArray.getArrayDescriptor(operand);
                super.visitMethodInsn(INVOKESTATIC, arType, "factory", "(" + Configuration.TAINT_TAG_DESC + arrayDescriptor + ")L" + arType + ";", false);
                controlFlowPolicy.generateEmptyTaint();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if(isIgnoreAllInstrumenting) {
            super.visitMultiANewArrayInsn(desc, dims);
            return;
        }
        Type arrayType = Type.getType(desc);
        StringBuilder methodToCall = new StringBuilder("MULTIANEWARRAY_");
        StringBuilder descToCall = new StringBuilder("(");
        for(int i = 0; i < dims; i++) {
            descToCall.append('I');
            descToCall.append(Configuration.TAINT_TAG_DESC);
        }
        if(arrayType.getElementType().getSort() == Type.OBJECT) {
            methodToCall.append("REFERENCE");
            if(dims == arrayType.getDimensions()) {
                descToCall.append("Ljava/lang/Class;");
                super.visitLdcInsn(arrayType.getElementType());
            }
        } else {
            methodToCall.append(arrayType.getElementType().getDescriptor());
        }
        methodToCall.append('_');
        methodToCall.append(arrayType.getDimensions());
        methodToCall.append("DIMS");

        descToCall.append(")");
        descToCall.append(Type.getDescriptor(LazyReferenceArrayObjTags.class));
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), methodToCall.toString(), descToCall.toString(), false);

        controlFlowPolicy.generateEmptyTaint(); //TODO array reference taint?
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof ReferenceArrayTarget) {
            this.referenceArrayTarget = (ReferenceArrayTarget) cst;
        } else if(cst instanceof PhosphorInstructionInfo) {
            controlFlowPolicy.visitingPhosphorInstructionInfo((PhosphorInstructionInfo) cst);
            super.visitLdcInsn(cst);
        } else {
            super.visitLdcInsn(cst);
            if(!isIgnoreAllInstrumenting) {
                controlFlowPolicy.generateEmptyTaint();
            }
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(isIgnoreAllInstrumenting) {
            super.visitTypeInsn(opcode, type);
            return;
        }
        switch(opcode) {
            case Opcodes.ANEWARRAY:
                if(!Configuration.WITHOUT_PROPAGATION) {
                    Type t = Type.getObjectType(type);
                    //L TL
                    super.visitInsn(SWAP);
                    if(t.getSort() == Type.ARRAY) {
                        type = TaintUtils.getWrapperType(t).getInternalName();
                    }
                    super.visitTypeInsn(opcode, type);
                    //2D arrays are just 1D arrays, not wrapped 1Darrays
                    Type arType = Type.getType(LazyReferenceArrayObjTags.class);
                    super.visitMethodInsn(INVOKESTATIC, arType.getInternalName(), "factory", "(" + Configuration.TAINT_TAG_DESC + "[Ljava/lang/Object;)" + arType.getDescriptor(), false);
                    //TODO what should we make the reference taint be here? how shoudl we use the array length?
                    controlFlowPolicy.generateEmptyTaint();
                }
                break;
            case Opcodes.NEW:
                super.visitTypeInsn(opcode, type);
                controlFlowPolicy.generateEmptyTaint();
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
    }

    private void instanceOf(String type) {
        Type t = Type.getObjectType(type);
        if(TaintUtils.isWrappedType(t)) {
            type = TaintUtils.getWrapperType(t).getInternalName();
        }
        super.visitInsn(SWAP);
        super.visitTypeInsn(INSTANCEOF, type);
        super.visitInsn(SWAP);
    }

    private void checkCast(String type) {
        Type t = Type.getObjectType(type);
        super.visitInsn(SWAP);
        if(TaintUtils.isWrappedType(t)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, TaintUtils.getWrapperType(t).getInternalName());
        } else {
            super.visitTypeInsn(CHECKCAST, type);
        }
        super.visitInsn(SWAP);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        boolean hasNewName = !TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getTag() != Opcodes.H_INVOKESTATIC, desc).equals(desc);
        String newDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getTag() != Opcodes.H_INVOKESTATIC, desc, false);
        boolean isPreAllocatedReturnType = TaintUtils.isPreAllocReturnType(desc);
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            hasNewName = true;
            newDesc = TaintUtils.remapMethodDescAndIncludeReturnHolderNoControlStack(bsm.getTag() != Opcodes.H_INVOKESTATIC, desc, false);
        }
        if(isPreAllocatedReturnType && Type.getReturnType(desc).getSort() == Type.OBJECT) {
            //Don't change return type
            isPreAllocatedReturnType = false;
            newDesc = Type.getMethodDescriptor(Type.getReturnType(desc), Type.getArgumentTypes(newDesc));
            newDesc = newDesc.replace(Type.getDescriptor(TaintedReferenceWithObjTag.class), "");
            doNotUnboxTaints = true;
        }
        int opcode = INVOKEVIRTUAL;
        if(bsm.getTag() == Opcodes.H_INVOKESTATIC) {
            opcode = INVOKESTATIC;
        }

        if(isPreAllocatedReturnType) {
            Type t = Type.getReturnType(newDesc);
            super.visitVarInsn(ALOAD, lvs.getPreAllocatedReturnTypeVar(t));
        }
        Type origReturnType = Type.getReturnType(desc);
        Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
        if(!name.contains("<") && hasNewName) {
            name += TaintUtils.METHOD_SUFFIX;
        }
        //if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
        //for the taint for that array
        Type[] args = Type.getArgumentTypes(newDesc);
        int argsSize = 0;
        for(Type type : args) {
            argsSize += type.getSize();
        }

        boolean isCalledOnAPrimitiveArrayType = false;
        if(opcode == INVOKEVIRTUAL) {
            if(analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null) {
                System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
            }
            Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
            if(callee.getSort() == Type.ARRAY) {
                isCalledOnAPrimitiveArrayType = true;
            }
        }
        if(bsmArgs != null) {

            //Are we remapping something with a primitive return that gets ignored?
            if(bsm.getName().equals("metafactory") || bsm.getName().equals("altMetafactory")) {
                Handle implMethod = (Handle) bsmArgs[1];

                boolean isNEW = implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL;
                boolean isVirtual = (implMethod.getTag() == Opcodes.H_INVOKEVIRTUAL) || implMethod.getTag() == Opcodes.H_INVOKESPECIAL || implMethod.getTag() == Opcodes.H_INVOKEINTERFACE;
                String remappedImplDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(isVirtual, implMethod.getDesc(), !implMethod.getName().startsWith("lambda$"));

                if(!Instrumenter.isIgnoredClass(implMethod.getOwner()) && !Instrumenter.isIgnoredMethod(implMethod.getOwner(), implMethod.getName(), implMethod.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(isVirtual || isNEW, implMethod.getDesc()).equals(implMethod.getDesc())) {
                    Type uninstSamMethodType = (Type) bsmArgs[0];
                    bsmArgs[0] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(false, ((Type) bsmArgs[0]).getDescriptor()));
                    String implMethodDesc = implMethod.getDesc();
                    Type samMethodType = (Type) bsmArgs[0];
                    Type instantiatedType = (Type) bsmArgs[2];
                    Type[] instantiatedMethodArgs = instantiatedType.getArgumentTypes();
                    Type[] uninstImplMethodArgs = Type.getArgumentTypes(implMethod.getDesc());
                    boolean needToAddUnWrapper = false;
                    boolean needToBoxReturnType = false;
                    //If there is a wrapped return type on the impl but not in the sam
                    if((uninstSamMethodType.getReturnType().getSort() == Type.VOID && Type.getReturnType(implMethodDesc).getSort() != Type.VOID) || isNEW) {
                        needToAddUnWrapper = true;
                    }
                    else if(!TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType()) && TaintUtils.isPrimitiveType(Type.getReturnType(implMethodDesc))){
                        //If the target method returns a primitive that needs to be boxed
                        needToAddUnWrapper = true;
                        needToBoxReturnType = true;
                    }
                    if(needToAddUnWrapper) {
                        ArrayList<Type> newWrapperArgs = new ArrayList<>();
                        Type[] implMethodArgs = Type.getArgumentTypes(remappedImplDesc);
                        ArrayList<Type> uninstNewWrapperArgs = new ArrayList<>();
                        if(isVirtual) {
                            newWrapperArgs.add(Type.getObjectType(implMethod.getOwner()));
                            if(instantiatedMethodArgs.length == 0 || !instantiatedMethodArgs[0].getInternalName().equals(implMethod.getOwner())) {
                                uninstNewWrapperArgs.add(Type.getObjectType(implMethod.getOwner()));
                            }
                        }
                        for(Type t : implMethodArgs) {
                            if(!t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                                newWrapperArgs.add(t);
                            }
                        }
                        if(isNEW || needToBoxReturnType) {
                            newWrapperArgs.add(Type.getType(TaintedReferenceWithObjTag.class));
                        }
                        for (int i = 0; i < instantiatedMethodArgs.length; i++) {
                            Type t = instantiatedMethodArgs[i];
                            if (i < uninstImplMethodArgs.length && TaintUtils.isPrimitiveType(uninstImplMethodArgs[i]) && !TaintUtils.isPrimitiveType(t)) {
                                uninstNewWrapperArgs.add(uninstImplMethodArgs[i]);
                            } else {
                                uninstNewWrapperArgs.add(t);
                            }
                        }
                        Type wrapperReturnType = (isNEW || needToBoxReturnType ? Type.getType(TaintedReferenceWithObjTag.class) : Type.VOID_TYPE);
                        Type uninstWrapperDesc = Type.getMethodType(wrapperReturnType, uninstNewWrapperArgs.toArray(new Type[0]));
                        Type wrapperDesc = Type.getMethodType(wrapperReturnType, newWrapperArgs.toArray(new Type[0]));

                        Type newContainerReturnType = TaintUtils.getContainerReturnType(wrapperDesc.getReturnType());
                        Type originalReturnType = wrapperDesc.getReturnType();

                        String wrapperName = "phosphorWrapInvokeDynamic" + wrapperMethodsToAdd.size();

                        MethodNode mn = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, wrapperName, uninstWrapperDesc.getDescriptor(), null, null);

                        GeneratorAdapter ga = new GeneratorAdapter(mn, Opcodes.ACC_STATIC, wrapperName, wrapperDesc.getDescriptor());
                        ga.visitCode();

                        if(isNEW) {
                            ga.visitTypeInsn(Opcodes.NEW, implMethod.getOwner());
                            ga.visitInsn(DUP);
                        }
                        int offset = 0;
                        int i = 0;
                        for(Type t : uninstNewWrapperArgs) {
                            ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), offset);
                            if(t.getSort() == Type.INT) {
                                switch(implMethodArgs[i].getSort()) {
                                    case Type.LONG:
                                        ga.visitInsn(Opcodes.I2L);
                                        break;
                                    case Type.FLOAT:
                                        ga.visitInsn(Opcodes.I2F);
                                        break;
                                    case Type.DOUBLE:
                                        ga.visitInsn(Opcodes.I2D);
                                        break;
                                }
                            }else if(TaintUtils.isPrimitiveType(implMethodArgs[i]) && !TaintUtils.isPrimitiveType(t)){
                                //unbox
                                ga.unbox(implMethodArgs[i]);
                            }
                            i++;//for this var
                            i++;//for taint
                            offset += t.getSize();
                        }
                        int opToCall;
                        boolean isInterface = false;
                        switch(implMethod.getTag()) {
                            case H_INVOKESTATIC:
                                opToCall = INVOKESTATIC;
                                break;
                            case H_INVOKEVIRTUAL:
                                opToCall = INVOKEVIRTUAL;
                                break;
                            case H_INVOKESPECIAL:
                            case H_NEWINVOKESPECIAL:
                                opToCall = INVOKESPECIAL;
                                break;
                            case H_INVOKEINTERFACE:
                                isInterface = true;
                                opToCall = INVOKEINTERFACE;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        ga.visitMethodInsn(opToCall, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), isInterface);
                        if(needToBoxReturnType){
                            ga.box(Type.getReturnType(implMethodDesc));
                        }
                        ga.returnValue();
                        ga.visitMaxs(0, 0);
                        ga.visitEnd();
                        mn.maxLocals = 100;

                        wrapperMethodsToAdd.add(mn);

                        //Change the bsmArgs to point to the new wrapper (which may or may not get the suffix)
                        // String taintedWrapperDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(false, wrapperDesc.getDescriptor());
                        String targetName = wrapperName + TaintUtils.METHOD_SUFFIX;
                        bsmArgs[1] = new Handle(Opcodes.H_INVOKESTATIC, owner, targetName, wrapperDesc.getDescriptor(), false);

                        //build the new instantiated desntutilsc
                        for(int j = 0; j < instantiatedMethodArgs.length; j++) {
                            if(TaintUtils.isWrappedType(instantiatedMethodArgs[j])) {
                                instantiatedMethodArgs[j] = TaintUtils.getWrapperType(instantiatedMethodArgs[j]);
                            }
                        }

                        Type instantiatedReturnType;
                        if(isNEW) { //|| unboxPrimitiveReturn || boxPrimitiveReturn) {
                            instantiatedReturnType = newContainerReturnType;
                        } else {
                            instantiatedReturnType = originalReturnType;
                        }
                        if(samMethodType.getReturnType().getSort() == Type.VOID) {
                            instantiatedReturnType = Type.VOID_TYPE;
                        }
                        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(false, Type.getMethodType(instantiatedReturnType, instantiatedMethodArgs).getDescriptor()));

                    } else {
                        bsmArgs[1] = new Handle(implMethod.getTag(), implMethod.getOwner(), implMethod.getName() + (implMethod.getName().equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), remappedImplDesc, implMethod.isInterface());
                        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(false, instantiatedType.getDescriptor()));
                    }
                }
            } else {
                if(bsmArgs.length > 1 && bsmArgs[1] instanceof Handle) {
                    Type t = Type.getMethodType(((Handle) bsmArgs[1]).getDesc());
                    if(TaintUtils.isPrimitiveType(t.getReturnType())) {
                        Type _t = (Type) bsmArgs[0];
                        if(_t.getReturnType().getSort() == Type.VOID) {
                            //Manually add the return type here;
                            StringBuilder nd = new StringBuilder();
                            nd.append('(');
                            for(Type a : _t.getArgumentTypes()) {
                                nd.append(a.getDescriptor());
                            }
                            nd.append(TaintUtils.getContainerReturnType(t.getReturnType()));
                            nd.append(")V");
                            bsmArgs[0] = Type.getMethodType(nd.toString());
                        }
                        _t = (Type) bsmArgs[2];
                        if(_t.getReturnType().getSort() == Type.VOID) {
                            //Manually add the return type here;
                            StringBuilder nd = new StringBuilder();
                            nd.append('(');
                            for(Type a : _t.getArgumentTypes()) {
                                nd.append(a.getDescriptor());
                            }
                            nd.append(TaintUtils.getContainerReturnType(t.getReturnType()));
                            nd.append(")V");
                            bsmArgs[2] = Type.getMethodType(nd.toString());
                        }
                    }
                }
                for(int k = 0; k < bsmArgs.length; k++) {
                    Object o = bsmArgs[k];
                    if(o instanceof Handle) {
                        String nameH = ((Handle) o).getName();
                        boolean isVirtual = (((Handle) o).getTag() == Opcodes.H_INVOKEVIRTUAL) || ((Handle) o).getTag() == Opcodes.H_INVOKESPECIAL || ((Handle) o).getTag() == Opcodes.H_INVOKEINTERFACE;

                        if (!Instrumenter.isIgnoredClass(((Handle) o).getOwner()) && !Instrumenter.isIgnoredClassWithStubsButNoTracking(((Handle) o).getOwner()) && !Instrumenter.isIgnoredMethod(((Handle) o).getOwner(), nameH, ((Handle) o).getDesc()) &&
                                !TaintUtils.remapMethodDescAndIncludeReturnHolder(isVirtual, ((Handle) o).getDesc()).equals(((Handle) o).getDesc())) {
                            bsmArgs[k] = new Handle(((Handle) o).getTag(), ((Handle) o).getOwner(), nameH + (nameH.equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), TaintUtils.remapMethodDescAndIncludeReturnHolder(isVirtual, ((Handle) o).getDesc()), ((Handle) o).isInterface());
                        }
                    } else if (o instanceof Type) {
                        Type t = (Type) o;
                        bsmArgs[k] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(true, t.getDescriptor()));
                    }
                }
            }
        }
        if(hasNewName && !Instrumenter.isIgnoredClass(bsm.getOwner()) && !Instrumenter.isIgnoredClassWithStubsButNoTracking(bsm.getOwner())) {
            if(!Instrumenter.isIgnoredMethod(bsm.getOwner(), bsm.getName(), bsm.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(true, bsm.getDesc()).equals(bsm.getDesc())) {
                bsm = new Handle(bsm.getTag(), bsm.getOwner(), bsm.getName() + TaintUtils.METHOD_SUFFIX, TaintUtils.remapMethodDescAndIncludeReturnHolder(true, bsm.getDesc()), bsm.isInterface());
            }
        }
        for(Type arg : Type.getArgumentTypes(newDesc)) {
            if(TaintUtils.isWrappedTypeWithErasedType(arg)) {
                super.visitInsn(ACONST_NULL);
            }
        }
        super.visitInvokeDynamicInsn(name, newDesc, bsm, bsmArgs);
        if(Type.getReturnType(newDesc).getSort() != Type.VOID) {
            NEW_EMPTY_TAINT.delegateVisit(mv);
        }
        if(isCalledOnAPrimitiveArrayType) {
            if(Type.getReturnType(desc).getSort() == Type.VOID) {
                super.visitInsn(POP);
            } else if(analyzer.stack.size() >= 2) {
                //this is so dumb that it's an array type.
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }
        }
        if(doNotUnboxTaints) {
            doNotUnboxTaints = false;
            return;
        }
        String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
        if(taintType != null) {
            super.visitInsn(DUP);
            if(origReturnType.getSort() == Type.OBJECT) {
                super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
            } else {
                super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
            }
            if(origReturnType.getSize() == 2) {
                super.visitInsn(DUP2_X1);
                super.visitInsn(POP2);
            } else {
                super.visitInsn(SWAP);
            }
            super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintType);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        controlFlowPolicy.visitingMaxs();
        if(rewriteLVDebug) {
            Label end = new Label();
            super.visitLabel(end);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        if(isIgnoreAllInstrumenting || isRawInstruction) {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            return;
        }
        if(name.equals("getProperty") && this.owner.equals("org/eclipse/jdt/core/tests/util/Util")) {
            // Workaround for eclipse benchmark
            super.visitInsn(POP); //remove the taint
            owner = Type.getInternalName(ReflectionMasker.class);
            name = "getPropertyHideBootClasspath";
        }
        if(isBoxUnboxMethodToWrap(owner, name)) {
            if(name.equals("valueOf")) {
                switch(owner) {
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
            if(opcode != INVOKESTATIC) {
                opcode = INVOKESTATIC;
                desc = "(L" + owner + ";" + desc.substring(1);
            }
            owner = "edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator";
        }
        boolean isPreAllocatedReturnType = TaintUtils.isPreAllocReturnType(desc);
        if(Instrumenter.isClassWithHashMapTag(owner) && name.equals("valueOf")) {
            Type[] args = Type.getArgumentTypes(desc);
            if(args[0].getSort() != Type.OBJECT) {
                super.visitInsn(SWAP);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
                super.visitInsn(SWAP);
            }
            return;
        }

        Type ownerType = Type.getObjectType(owner);
        if(owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints") && !name.equals("hasNoDependencies") && !desc.equals("(I)V") && !owner.endsWith("Tainter") && !owner.endsWith("CharacterUtils")
                && !name.equals("getPHOSPHOR_TAG") && !name.equals("setPHOSPHOR_TAG") && !owner.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropagator")
                && !owner.equals(Type.getInternalName(PowerSetTree.class))
                && !owner.equals("edu/columbia/cs/psl/phosphor/util/IgnoredTestUtil")
                && !owner.equals(Configuration.TAINT_TAG_INTERNAL_NAME)
                && !owner.equals(TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME)) {
            Configuration.taintTagFactory.methodOp(opcode, owner, name, desc, isInterface, mv, lvs, this);
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if(Type.getReturnType(desc).getSort() != Type.VOID) {
                NEW_EMPTY_TAINT.delegateVisit(mv);
            }
            return;
        }
        if(opcode == INVOKEVIRTUAL && TaintUtils.isWrappedType(ownerType)) {
            owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
        }
        if(opcode == INVOKEVIRTUAL && name.equals("clone") && desc.startsWith("()")) {
            if(owner.equals("java/lang/Object") && TaintUtils.isWrapperType(getTopOfStackType())) {
                owner = getTopOfStackType().getInternalName();
            } else {
                //TODO reference tainting - should we still pass the tag to custom clone implementations?
                //we need to do it like this for arrays either way.
                super.visitInsn(SWAP);//T A
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                //T A
                super.visitInsn(SWAP);
                return;
            }
        }
        if(opcode == INVOKEVIRTUAL && name.equals("getClass") && desc.equals("()Ljava/lang/Class;")) {
            super.visitInsn(SWAP);
            if(isObjOutputStream) {
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            } else {
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(NativeHelper.class), "getClassOrWrapped", "(Ljava/lang/Object;)Ljava/lang/Class;", false);
            }
            super.visitInsn(SWAP);
            return;
        }
        if((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
                && !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
            owner = Type.getInternalName(TaintUtils.class);
        }
        if((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) && opcode == INVOKEVIRTUAL && owner.equals("java/lang/Object") && (name.equals("equals") || name.equals("hashCode"))) {
            Type callee = getTopOfStackType();
            if(name.equals("equals")) {
                callee = getStackTypeAtOffset(1);
            }
            if(callee.getSort() == Type.OBJECT) {
                String calledOn = callee.getInternalName();
                try {
                    Class<?> in = Class.forName(calledOn.replace('/', '.'), false, TaintPassingMV.class.getClassLoader());
                    if(!in.isInterface() && !Instrumenter.isIgnoredClass(calledOn) && !Instrumenter.isIgnoredClassWithStubsButNoTracking(calledOn)) {
                        owner = calledOn;
                    }
                } catch(Throwable t) {
                    //if not ignored, can still make an invokeinterface
                    if(!Instrumenter.isIgnoredClass(calledOn) && !Instrumenter.isIgnoredClassWithStubsButNoTracking(calledOn)) {
                        owner = Type.getInternalName(TaintedWithObjTag.class);
                        opcode = INVOKEINTERFACE;
                        isInterface = true;
                    }

                }
            }
        }
        if(opcode == INVOKEVIRTUAL && Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE && (name.equals("equals")
                || name.equals("hashCode")) && owner.equals("java/lang/Object")) {
            opcode = INVOKESTATIC;
            owner = Type.getInternalName(NativeHelper.class);
            if(name.equals("equals")) {
                desc = "(Ljava/lang/Object;Ljava/lang/Object;)Z";
            } else {
                desc = "(Ljava/lang/Object;)I";
            }
        }
        //to reduce how much we need to wrap, we will only rename methods that actually have a different descriptor
        boolean hasNewName = !TaintUtils.remapMethodDescAndIncludeReturnHolder(opcode != INVOKESTATIC, desc).equals(desc);
        if((Instrumenter.isIgnoredClass(owner) || Instrumenter.isIgnoredClassWithStubsButNoTracking(owner) || Instrumenter.isIgnoredMethod(owner, name, desc)) && !isInternalTaintingClass(owner) && !name.equals("arraycopy")) {
            Type[] args = Type.getArgumentTypes(desc);
            int argsSize = 0;
            //Remove all taints
            int[] tmp = new int[args.length];
            Type[] ts = new Type[args.length];
            for(int i = 0; i < args.length; i++) {
                Type expected = args[args.length - i - 1];
                argsSize += expected.getSize();
                Type t = getTopOfStackType();
                ts[i] = t;
                tmp[i] = lvs.getTmpLV(t);
                super.visitInsn(Opcodes.POP);
                if(TaintUtils.isWrapperType(t)) {
                    Type unwrapped = TaintUtils.getUnwrappedType(t);
                    super.visitMethodInsn(INVOKESTATIC, t.getInternalName(), "unwrap", "(" + t.getDescriptor() + ")" + unwrapped.getDescriptor(), false);
                    if(unwrapped.getDescriptor().equals("[Ljava/lang/Object;")) {
                        super.visitTypeInsn(CHECKCAST, expected.getInternalName());
                    }
                }
                super.visitVarInsn(t.getOpcode(ISTORE), tmp[i]);
            }
            if(opcode != INVOKESTATIC) {
                super.visitInsn(POP); //remove reference taint
            }
            for(int i = args.length - 1; i >= 0; i--) {
                super.visitVarInsn(ts[i].getOpcode(ILOAD), tmp[i]);
                lvs.freeTmpLV(tmp[i]);
            }
            boolean isCalledOnAPrimitiveArrayType = false;
            if(opcode == INVOKEVIRTUAL) {
                Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
                if(callee.getSort() == Type.ARRAY) {
                    isCalledOnAPrimitiveArrayType = true;
                }
            }
            Configuration.taintTagFactory.methodOp(opcode, owner, name, desc, isInterface, mv, lvs, this);
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);

            Type returnType = Type.getReturnType(desc);
            if(returnType.getDescriptor().endsWith("Ljava/lang/Object;") || returnType.getSort() == Type.ARRAY) {
                BOX_IF_NECESSARY.delegateVisit(mv);
                if(TaintUtils.isWrappedType(returnType)) {
                    super.visitTypeInsn(Opcodes.CHECKCAST, TaintUtils.getWrapperType(returnType).getInternalName());
                }
            }
            if(returnType.getSort() != Type.VOID) {
                controlFlowPolicy.generateEmptyTaint();
            }
            return;
        }
        String newDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(opcode != INVOKESTATIC, desc);
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            if((isInternalTaintingClass(owner) || owner.startsWith("[")) && !name.equals("getControlFlow") && !name.startsWith("hashCode") && !name.startsWith("equals")) {
                newDesc = newDesc.replace(TaintTrackingClassVisitor.CONTROL_STACK_DESC, "");
            } else {
                super.visitVarInsn(ALOAD, lvs.getIndexOfMasterControlLV());
            }
            if(owner.startsWith("[")) {
                hasNewName = false;
            }
        }
        if(isPreAllocatedReturnType) {
            Type t = Type.getReturnType(newDesc);
            super.visitVarInsn(ALOAD, lvs.getPreAllocatedReturnTypeVar(t));
        }
        Type origReturnType = Type.getReturnType(desc);
        Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));

        if(!name.contains("<") && hasNewName) {
            name += TaintUtils.METHOD_SUFFIX;
        }

        Type[] args = Type.getArgumentTypes(newDesc);
        Type[] argsInReverse = new Type[args.length];
        int argsSize = 0;

        //Before we go, push enough NULL's on the stack to account for the extra args we add to disambiguate wrappers.
        for(int i = 0; i < args.length; i++) {
            argsInReverse[args.length - i - 1] = args[i];
            argsSize += args[i].getSize();
            if(TaintUtils.isWrappedTypeWithErasedType(args[i])) {
                super.visitInsn(ACONST_NULL);
            }
        }
        boolean isCalledOnAPrimitiveArrayType = false;
        if(opcode == INVOKEVIRTUAL) {
            if(analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null) {
                System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
            }
            Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
            if(callee.getSort() == Type.ARRAY) {
                isCalledOnAPrimitiveArrayType = true;
            }
            if(callee.getDescriptor().equals("Ljava/lang/Object;") && !owner.equals("java/lang/Object")) {
                //AALOAD can result in a type of "java/lang/Object" on the stack (if there are no stack map frames)
                //If this happened, we need to force a checkcast.
                LocalVariableNode[] tmpLVs = storeToLocals(args.length);
                super.visitTypeInsn(CHECKCAST, owner);
                for(int i = tmpLVs.length - 1; i >= 0; i--) {
                    super.visitVarInsn(Type.getType(tmpLVs[i].desc).getOpcode(ILOAD), tmpLVs[i].index);
                }
                freeLVs(tmpLVs);
            }
        }
        Configuration.taintTagFactory.methodOp(opcode, owner, name, newDesc, isInterface, mv, lvs, this);

        super.visitMethodInsn(opcode, owner, name, newDesc, isInterface);
        if(isCalledOnAPrimitiveArrayType) {
            if(Type.getReturnType(desc).getSort() == Type.VOID) {
                super.visitInsn(POP);
            } else if(analyzer.stack.size() >= 2) {
                //this is so dumb that it's an array type.
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }
        }
        if(isTaintlessArrayStore) {
            isTaintlessArrayStore = false;
            return;
        }
        if(doNotUnboxTaints) {
            doNotUnboxTaints = false;
            return;
        }
        String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
        if(taintType != null) {
            FrameNode fn = getCurrentFrameNode();
            fn.type = Opcodes.F_NEW;
            super.visitInsn(DUP);
            String taintTypeRaw = Configuration.TAINT_TAG_DESC;
            if(origReturnType.getSort() == Type.OBJECT || origReturnType.getSort() == Type.ARRAY) {
                super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", "Ljava/lang/Object;");
                if(TaintUtils.isWrappedType(origReturnType)) {
                    super.visitTypeInsn(CHECKCAST, TaintUtils.getWrapperType(origReturnType).getInternalName());
                } else {
                    super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
                }
            } else {
                super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
            }
            if(origReturnType.getSize() == 2) {
                super.visitInsn(DUP2_X1);
                super.visitInsn(POP2);
            } else {
                super.visitInsn(SWAP);
            }
            super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintTypeRaw);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(TaintUtils.isReturnOpcode(opcode) || opcode == ATHROW) {
            controlFlowPolicy.onMethodExit(opcode);
        }
        if(isLambda && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            //Do we need to box?
            Type returnType = Type.getReturnType(this.descriptor);
            if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct")) {
                String t = null;
                if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
                    t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Tainted", "");
                    t = t.replace("WithObjTag", "");
                } else if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Lazy", "");
                    t = t.replace("ObjTags", "");
                    t = "[" + t;
                }
                if(t != null) {
                    t = t.replace(";", "").replace("Int", "I")
                            .replace("Byte", "B")
                            .replace("Short", "S")
                            .replace("Long", "J")
                            .replace("Boolean", "Z")
                            .replace("Float", "F")
                            .replace("Double", "D");
                }
                //Probably need to box...
                int returnHolder = lastArg - 1;
                if(getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1)).getSize() == 2) {
                    super.visitVarInsn(ALOAD, returnHolder);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitVarInsn(ALOAD, returnHolder);
                    super.visitInsn(SWAP);
                }
                if(t.equals("Reference")) {
                    t = "Ljava/lang/Object;";
                }
                super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "val", t);

                super.visitVarInsn(ALOAD, returnHolder);
                super.visitInsn(DUP);
                NEW_EMPTY_TAINT.delegateVisit(mv);
                super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);

                super.visitInsn(ARETURN);
            } else {
                super.visitInsn(opcode);
            }
            return;
        } else if(opcode == TaintUtils.RAW_INSN) {
            isRawInstruction = !isRawInstruction;
            return;
        } else if(opcode == TaintUtils.IGNORE_EVERYTHING) {
            isIgnoreAllInstrumenting = !isIgnoreAllInstrumenting;
            Configuration.taintTagFactory.signalOp(opcode, null);
            super.visitInsn(opcode);
            return;
        } else if(opcode == TaintUtils.NO_TAINT_STORE_INSN) {
            isTaintlessArrayStore = true;
            return;
        } else if(isIgnoreAllInstrumenting || isRawInstruction) {
            super.visitInsn(opcode);
            return;
        }

        switch(opcode) {
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                super.visitInsn(POP);
            case Opcodes.NOP:
            case TaintUtils.FOLLOWED_BY_FRAME:
                super.visitInsn(opcode);
                break;
            case Opcodes.ACONST_NULL:
                super.visitInsn(opcode);
                controlFlowPolicy.generateEmptyTaint();
                break;
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
                super.visitInsn(opcode);
                controlFlowPolicy.generateEmptyTaint();
                return;
            case Opcodes.LALOAD:
            case Opcodes.DALOAD:
            case Opcodes.IALOAD:
            case Opcodes.FALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.AALOAD:
                visitArrayLoad(opcode);
                break;
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                visitArrayStore(opcode);
                break;
            case Opcodes.POP:
                super.visitInsn(POP2);
                return;
            case Opcodes.POP2:
                super.visitInsn(POP);
                if(getTopOfStackType().getSize() != 2) {
                    super.visitInsn(POP);
                }
                super.visitInsn(POP2);
                return;
            case Opcodes.DUP:
                super.visitInsn(Opcodes.DUP2);
                break;
            case Opcodes.DUP2:
                Object topOfStack = analyzer.stack.get(analyzer.stack.size() - 2);

                //0 1 -> 0 1 2 3
                if(getStackElementSize(topOfStack) == 1) {
                    DUPN_XU(4, 0);
                } else {
                    // DUP2, top of stack is double
                    //VVT -> VVT VVT
                    int top = lvs.getTmpLV();
                    super.visitInsn(DUP_X2);
                    //TVVT
                    super.visitInsn(TaintUtils.IS_TMP_STORE);
                    super.visitVarInsn(ASTORE, top);
                    //TVV
                    super.visitInsn(DUP2_X1);
                    //VVTVV
                    super.visitVarInsn(ALOAD, top);
                    // VVT VVT
                    lvs.freeTmpLV(top);
                }
                break;
            case Opcodes.DUP_X1:
                super.visitInsn(DUP2_X2);
                break;
            case Opcodes.DUP_X2:
                //X?X? VT -> VTXX?VT
                if(getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 4)) == 2) {
                    // With long/double under
                    //XXT VT -> VT XXTVT
                    DUPN_XU(2, 2); //fixed
                } else {
                    // With 1word under
                    DUPN_XU(2, 4);
                }
                break;
            case Opcodes.DUP2_X1:
                //ATBTCT -> BTCTATBTCT (0 1 2 3 4)
                topOfStack = analyzer.stack.get(analyzer.stack.size() - 2);
                if(getStackElementSize(topOfStack) == 1) {
                    //ATBTCT -> BTCTATBTCT
                    DUPN_XU(4, 2);
                } else {
                    //ATBBT -> BBTATBBT
                    DUPN_XU(2, 2);
                }
                break;
            case Opcodes.DUP2_X2:
                topOfStack = analyzer.stack.get(analyzer.stack.size() - 2);
                if(getStackElementSize(topOfStack) == 1) {
                    //Second must be a single word
                    //??TBTCT ->
                    Object third = analyzer.stack.get(analyzer.stack.size() - 6);
                    if(getStackElementSize(third) == 1) {
                        //ATBTCTDT -> CTDTATBTCTDT
                        DUPN_XU(4, 4);
                    } else {
                        //Two single words above a tainted double word
                        //AATBTCT -> BTCTAATBTCT
                        DUPN_XU(4, 3);
                    }
                } else {
                    //Top is 2 words
                    //??TBBT ->
                    Object third = analyzer.stack.get(analyzer.stack.size() - 5);
                    if(getStackElementSize(third) == 1) {
                        //ATBTCCT -> CCTATBTCCT
                        DUPN_XU(2, 4);
                    } else {
                        DUPN_XU(2, 2);
                    }
                }
                break;
            case Opcodes.SWAP:
                super.visitInsn(DUP2_X2);
                super.visitInsn(POP2);
                break;
            case Opcodes.FADD:
            case Opcodes.FREM:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IOR:
            case Opcodes.IAND:
            case Opcodes.IXOR:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.LSHL:
            case Opcodes.LUSHR:
            case Opcodes.LSHR:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LADD:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                break;
            case Opcodes.DRETURN:
            case Opcodes.LRETURN:
                int retIdx = lvs.getPreAllocatedReturnTypeVar(newReturnType);
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
                super.visitVarInsn(ALOAD, retIdx);
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(ARETURN);
                break;
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
                retIdx = lvs.getPreAllocatedReturnTypeVar(newReturnType);
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                if(opcode == ARETURN) {
                    super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", "Ljava/lang/Object;");
                } else {
                    super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
                }
                super.visitVarInsn(ALOAD, retIdx);
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(ARETURN);
                break;
            case Opcodes.RETURN:
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(opcode);
                break;
            case Opcodes.ATHROW:
                super.visitInsn(POP); //TODO reference tainting from thrown exceptions
                super.visitInsn(opcode);
                break;
            default:
                super.visitInsn(opcode);
                throw new IllegalArgumentException();
        }
    }

    /**
     * stack_pre = [arrayref, reference-taint, index, index-taint, value, value-taint]
     * stack_post = []
     *
     * @param opcode the opcode of the instruction originally to be visited. This opcode is either IASTORE, LASTORE,
     *               FASTORE,DASTORE, BASTORE, CASTORE, SASTORE, or AASTORE.
     */
    private void visitArrayStore(int opcode) {
        // A T I T V T
        controlFlowPolicy.visitingArrayStore(opcode);
        int valuePosition = analyzer.stack.size() - (opcode == LASTORE || opcode == DASTORE ? 3 : 2);
        int indexPosition = valuePosition - 2;
        int arrayRefPosition = indexPosition - 2;
        MethodRecord setMethod;
        if(analyzer.stack.get(arrayRefPosition) == Opcodes.NULL) {
            setMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, "");
        } else {
            String arrayReferenceType = (String) analyzer.stack.get(arrayRefPosition);
            if(arrayReferenceType.startsWith("[") || !arrayReferenceType.contains("Lazy")) {
                throw new IllegalStateException("Calling XASTORE on " + arrayReferenceType);
            }
            setMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, arrayReferenceType);
        }
        setMethod.delegateVisit(mv);
        isTaintlessArrayStore = false;
    }

    /**
     * stack_pre = [arrayref, reference-taint, index, index-taint]
     * stack_post = [value, value-taint]
     *
     * @param opcode the opcode of the instruction originally to be visited. This opcode is either LALOAD, DALOAD,
     *               IALOAD, FALOAD, BALOAD, CALOAD, SALOAD, or AALOAD.
     */
    private void visitArrayLoad(int opcode) {
        LocalVariableNode[] d = storeToLocals(3);
        loadLV(2, d);
        loadLV(1, d);
        loadLV(0, d);
        int arrayRefPosition = analyzer.stack.size() - 4;
        MethodRecord getMethod;
        if(analyzer.stack.get(arrayRefPosition) == Opcodes.NULL) {
            getMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, "");
        } else {
            String arrayReferenceType = (String) analyzer.stack.get(arrayRefPosition);
            if(arrayReferenceType.startsWith("[") || !arrayReferenceType.contains("Lazy")) {
                throw new IllegalStateException("Calling XALOAD on " + arrayReferenceType);
            }
            getMethod = TaintMethodRecord.getTaintedArrayRecord(opcode, arrayReferenceType);
        }
        int preAllocated = lvs.getPreAllocatedReturnTypeVar(Type.getType(getMethod.getReturnType()));
        super.visitVarInsn(ALOAD, preAllocated);
        getMethod.delegateVisit(mv);
        unwrap(getMethod.getReturnType());
        // [value, value-taint]
        loadLV(2, d);
        freeLVs(d);
        super.visitInsn(SWAP);
        // [value, reference-taint, value-taint]
        controlFlowPolicy.visitingArrayLoad(opcode);
        COMBINE_TAGS.delegateVisit(mv);
    }

    /**
     * stack_pre = [TaintedPrimitiveWithObjTag]
     * stack_post = [value value-taint]
     *
     * @param wrapperType the type of the TaintedPrimitiveWithObjTag instance to be unwrapped
     */
    private void unwrap(Class<?> wrapperType) {
        super.visitInsn(DUP);
        String wrapperName = Type.getInternalName(wrapperType);
        String valueType = Type.getDescriptor(TaintUtils.getUnwrappedClass(wrapperType));
        super.visitFieldInsn(GETFIELD, wrapperName, "val", valueType);
        if(TaintedReferenceWithObjTag.class.equals(wrapperType) && referenceArrayTarget != null) {
            Type originalArrayType = Type.getType(referenceArrayTarget.getOriginalArrayType());
            String castTo = Type.getType(originalArrayType.getDescriptor().substring(1)).getInternalName();
            if(originalArrayType.getDimensions() == 2) {
                castTo = TaintUtils.getWrapperType(Type.getType(castTo)).getInternalName();
            } else if(originalArrayType.getDimensions() > 2) {
                castTo = Type.getInternalName(LazyReferenceArrayObjTags.class);
            }
            super.visitTypeInsn(CHECKCAST, castTo);
        }
        if(TaintedLongWithObjTag.class.equals(wrapperType) || TaintedDoubleWithObjTag.class.equals(wrapperType)) {
            super.visitInsn(DUP2_X1);
            super.visitInsn(POP2);
        } else {
            super.visitInsn(SWAP);
        }
        super.visitFieldInsn(GETFIELD, wrapperName, "taint", Configuration.TAINT_TAG_DESC);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label[] labels) {
        if(!isIgnoreAllInstrumenting) {
            Configuration.taintTagFactory.tableSwitch(min, max, defaultLabel, labels, mv, lvs, this);
            controlFlowPolicy.visitTableSwitch(min, max, defaultLabel, labels);
        }
        super.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultLabel, int[] keys, Label[] labels) {
        if(!isIgnoreAllInstrumenting) {
            Configuration.taintTagFactory.lookupSwitch(defaultLabel, keys, labels, mv, lvs, this);
            controlFlowPolicy.visitLookupSwitch(defaultLabel, keys, labels);
        }
        super.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if(!isIgnoreAllInstrumenting) {
            unboxForReferenceCompare(opcode);
            Configuration.taintTagFactory.jumpOp(opcode, label, mv, lvs, this);
            controlFlowPolicy.visitingJump(opcode, label);
        }
        super.visitJumpInsn(opcode, label);
    }

    private void unboxForReferenceCompare(int opcode) {
        if((opcode == IF_ACMPEQ || opcode == IF_ACMPNE) && Configuration.WITH_UNBOX_ACMPEQ
                && !owner.equals("java/io/ObjectOutputStream$HandleTable")) {
            // v1 t1 v2 t2
            super.visitInsn(SWAP);
            ENSURE_UNBOXED.delegateVisit(mv);
            super.visitInsn(SWAP);
            // v1 t1 v2* t2
            super.visitInsn(DUP2_X2);
            // v2* t2 v1 t1 v2* t2
            super.visitInsn(POP2);
            // v2* t2 v1 t1
            super.visitInsn(SWAP);
            ENSURE_UNBOXED.delegateVisit(mv);
            super.visitInsn(SWAP);
            // v2* t2 v1* t1
            super.visitInsn(DUP2_X2);
            // v1* t1 v2* t2 v1* t1
            super.visitInsn(POP2);
            // v1* t1 v2* t2
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        this.line = line;
        Configuration.taintTagFactory.lineNumberVisited(line);
    }

    /**
     * Returns whether a class with the specified name is used by Phosphor for "internal" tainting. Calls to methods in
     * internal tainting classes from instrumented classes are remapped to the appropriate "$$PHOSPHORTAGGED" version
     * even if the internal tainting class is not instrumented by Phosphor. This requires internal tainting classes to
     * provide instrumented versions of any method that may be invoked by a classes that is instrumented by Phosphor.
     *
     * @param owner the name of class being checking
     * @return true if a class with the specified name is used by Phosphor for internal tainting
     * @see MultiTainter
     */
    private static boolean isInternalTaintingClass(String owner) {
        return owner.startsWith("edu/columbia/cs/psl/phosphor/runtime/")
                || Configuration.taintTagFactory.isInternalTaintingClass(owner)
                || owner.startsWith("edu/gmu/swe/phosphor/ignored/runtime/");
    }

    private static boolean isBoxUnboxMethodToWrap(String owner, String name) {
        if((owner.equals(INTEGER_NAME) || owner.equals(BYTE_NAME) || owner.equals(BOOLEAN_NAME) || owner.equals(CHARACTER_NAME)
                || owner.equals(SHORT_NAME) || owner.equals(LONG_NAME) || owner.equals(FLOAT_NAME) || owner.equals(DOUBLE_NAME))
                && (name.equals("toString") || name.equals("toHexString") || name.equals("toOctalString") || name.equals("toBinaryString")
                || name.equals("toUnsignedString") || name.equals("valueOf"))) {
            return true;
        }
        switch(owner) {
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
                return name.equals("getChars") || name.equals("parseInt") || name.equals("parseUnsignedInt") || name.equals("intValue");
            case LONG_NAME:
                return name.equals("getChars") || name.equals("parseLong") || name.equals("parseUnsignedLong") || name.equals("longValue");
            case SHORT_NAME:
                return name.equals("parseShort") || name.equals("shortValue");
            default:
                return false;
        }
    }

    public static Type[] calculateParamTypes(boolean isStatic, String descriptor) {
        Type[] newArgTypes = Type.getArgumentTypes(descriptor);
        int lastArg = isStatic ? 0 : 1; // If non-static, then arg[0] = this
        for(Type t : newArgTypes) {
            lastArg += t.getSize();
        }
        Type[] paramTypes = new Type[lastArg + 1];
        int n = (isStatic ? 0 : 1);
        for(Type newArgType : newArgTypes) {
            paramTypes[n] = newArgType;
            n += newArgType.getSize();
        }
        return paramTypes;
    }
}