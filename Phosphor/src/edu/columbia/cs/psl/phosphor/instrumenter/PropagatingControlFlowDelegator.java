package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.Field;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

/**
 * Propagates control flows through a method by delegating instruction visits to a MethodVisitor.
 */
public class PropagatingControlFlowDelegator implements ControlFlowDelegator {

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    /**
     * Visitor to which instruction visiting that needs to "pass through" the primary delegate is delegated.
     */
    private final MethodVisitor passThroughDelegate;

    /**
     * Tracks the current stack and local variable bindings.
     */
    private final NeverNullArgAnalyzerAdapter analyzer;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private final LocalVariableManager localVariableManager;

    /**
     * True if the method being visited is an instance initialization method (i.e.,  {@code <init>}).
     */
    private final boolean isInstanceInitializer;

    /**
     * The number of unique identifier for "branch" location in the method being visited.
     */
    private final int numberOfBranchIDs;

    /**
     * Set containing the indices of local variables that need to be force stored.
     */
    private final Set<Integer> forceControlStoreVariables = new HashSet<>();

    /**
     * Set containing field that need to be force stored.
     */
    private final Set<Field> forceControlStoreFields = new HashSet<>();

    /**
     * Index of the last parameter of the method being visited.
     */
    private final int lastParameterIndex;

    /**
     * The types of parameters of the method being visited.
     */
    private final Type[] paramTypes;

    /**
     * Contains info about the next "branch" location encountered.
     */
    private SinglyLinkedList<BranchInfo> nextBranch = new SinglyLinkedList<>();

    /**
     * Indicates that the next instruction visited should be excluded from the scope of all revisable branch edges.
     */
    private boolean excludeNext = false;

    public PropagatingControlFlowDelegator(MethodVisitor delegate, MethodVisitor passThroughDelegate, NeverNullArgAnalyzerAdapter analyzer,
                                           LocalVariableManager localVariableManager, PrimitiveArrayAnalyzer primitiveArrayAnalyzer,
                                           String methodName, int lastParameterIndex, Type[] paramTypes) {
        this.delegate = delegate;
        this.passThroughDelegate = passThroughDelegate;
        this.analyzer = analyzer;
        this.localVariableManager = localVariableManager;
        this.isInstanceInitializer = "<init>".equals(methodName);
        this.lastParameterIndex = lastParameterIndex;
        this.paramTypes = paramTypes;
        this.numberOfBranchIDs = (primitiveArrayAnalyzer.nJumps + primitiveArrayAnalyzer.nTryCatch == 0) ? 0 : primitiveArrayAnalyzer.nJumps + primitiveArrayAnalyzer.nTryCatch + 2;
    }

    @Override
    public void visitedCode() {

    }

    @Override
    public void visitedTryCatch() {

    }

    @Override
    public void visitingIncrement(int var) {
        int shadowVar = 0;
        if(var < lastParameterIndex && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
            // Accessing an arg; remap it
            Type localType = paramTypes[var];
            if(TaintUtils.getShadowTaintType(localType.getDescriptor()) != null) {
                shadowVar = var - 1;
            }
        } else {
            shadowVar = localVariableManager.varToShadowVar.get(var);
        }
        delegate.visitVarInsn(ALOAD, shadowVar); // Current tag
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        if(excludeNext) {
            excludeNext = false;
            CONTROL_STACK_COPY_REVISION_EXCLUDED_TAG.delegateVisit(delegate);
            COMBINE_TAGS.delegateVisit(delegate);
        } else {
            COMBINE_TAGS_CONTROL.delegateVisit(delegate);
        }
        delegate.visitVarInsn(ASTORE, shadowVar);
    }

    private void getRevisionExcludedControlTag() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        CONTROL_STACK_COPY_REVISION_EXCLUDED_TAG.delegateVisit(delegate);
    }

    @Override
    public void visitingBranchStart(int branchID, boolean revisable) {
        nextBranch.enqueue(new BranchInfo(branchID, revisable));
    }

    @Override
    public void visitingBranchEnd(int branchID) {
        if(localVariableManager.getIndexOfBranchesLV() != -1) {
            callPopControlTaint(branchID);
        }
    }

    @Override
    public void storingTaintedValue(int opcode, int var) {
        if(opcode == TaintUtils.FORCE_CTRL_STORE) {
            forceControlStoreVariables.add(var);
        } else if(excludeNext) {
            switch(opcode) {
                case ISTORE:
                case FSTORE:
                    // taint value
                    delegate.visitInsn(SWAP);
                    getRevisionExcludedControlTag();
                    COMBINE_TAGS.delegateVisit(delegate);
                    delegate.visitInsn(SWAP);
                    break;
                case DSTORE:
                case LSTORE:
                    // taint value
                    delegate.visitInsn(DUP2_X1);
                    delegate.visitInsn(POP2);
                    getRevisionExcludedControlTag();
                    COMBINE_TAGS.delegateVisit(delegate);
                    delegate.visitInsn(DUP_X2);
                    delegate.visitInsn(POP);
                    break;
                case ASTORE:
                    // objectref
                    delegate.visitInsn(DUP);
                    getRevisionExcludedControlTag();
                    COMBINE_TAGS_IN_PLACE.delegateVisit(delegate);
                    break;
            }
            excludeNext = false;
        } else {
            switch(opcode) {
                case ISTORE:
                case FSTORE:
                    // taint value
                    delegate.visitInsn(SWAP);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                    delegate.visitInsn(SWAP);
                    break;
                case DSTORE:
                case LSTORE:
                    // taint value
                    delegate.visitInsn(DUP2_X1);
                    delegate.visitInsn(POP2);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                    delegate.visitInsn(DUP_X2);
                    delegate.visitInsn(POP);
                    break;
                case ASTORE:
                    // objectref
                    delegate.visitInsn(DUP);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
                    break;
            }
        }
    }

    @Override
    public void visitingForceControlStoreField(Field field) {
        forceControlStoreFields.add(field);
    }

    @Override
    public void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint) {
        if(!isStatic) {
            // Taint the object that owns the field
            if(!isInstanceInitializer) {
                if(type.getSize() == 1) {
                    if(topCarriesTaint) {
                        // obj, taint, val
                        delegate.visitInsn(DUP2_X1);
                        // taint, val, obj, taint, val
                        delegate.visitInsn(POP2);
                        // taint, val, obj
                        delegate.visitInsn(DUP_X2);
                        // obj, taint, val, obj
                    } else {
                        // obj, val
                        delegate.visitInsn(SWAP);
                        // val, obj
                        delegate.visitInsn(DUP_X1);
                        // obj, val, obj
                    }
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
                } else {
                    // obj, taint, val-wide
                    int tmp = localVariableManager.getTmpLV(type);
                    delegate.visitVarInsn((type.getSort() == Type.DOUBLE ? DSTORE : LSTORE), tmp);
                    // obj, taint
                    delegate.visitInsn(SWAP);
                    // taint, obj
                    delegate.visitInsn(DUP_X1);
                    // obj, taint, obj
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
                    // obj, taint
                    delegate.visitVarInsn(type.getSort() == Type.DOUBLE ? DLOAD : LLOAD, tmp);
                    // obj, taint, val-wide
                    localVariableManager.freeTmpLV(tmp);
                }
            }
        }
        if(type.getSort() == Type.OBJECT) {
            // objectref
            delegate.visitInsn(DUP);
            //objectref objectref
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
            // objectref
        } else if(type.getSort() != Type.ARRAY) {
            // Primitive type
            if(type.getSize() == 1) {
                // taint, val
                delegate.visitInsn(SWAP);
                // val, taint
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                // val, taint
                delegate.visitInsn(SWAP);
                // taint, val
            } else {
                // taint, val-wide
                delegate.visitInsn(DUP2_X1);
                // val-wide, taint, val-wide
                delegate.visitInsn(POP2);
                // val-wide, taint
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                // val-wide, taint
                delegate.visitInsn(DUP_X2);
                // taint, val-wide, taint
                delegate.visitInsn(POP);
                // taint, val-wide
            }
        }
    }

    @Override
    public void generateEmptyTaint() {
        if(excludeNext) {
            getRevisionExcludedControlTag();
            excludeNext = false;
        } else {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
                CONTROL_STACK_COPY_TAG_EXCEPTIONS.delegateVisit(delegate);
            } else {
                CONTROL_STACK_COPY_TAG.delegateVisit(delegate);
            }
        }
    }

    @Override
    public void visitingExceptionHandlerStart(String type) {
        if(type == null) {
            delegate.visitInsn(DUP);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(SWAP);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfControlExceptionLV());
            CONTROL_STACK_EXCEPTION_HANDLER_START.delegateVisit(delegate);
            delegate.visitVarInsn(ASTORE, localVariableManager.getIndexOfControlExceptionLV());
            executeForcedControlStores();
        } else {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitLdcInsn(Type.getObjectType(type));
            CONTROL_STACK_EXCEPTION_HANDLER_START_VOID.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingExceptionHandlerEnd(String type) {
        if(type == null) {
            // End of a handler
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfControlExceptionLV());
            CONTROL_STACK_EXCEPTION_HANDLER_END.delegateVisit(delegate);
        } else {
            // End of a try block
            executeForcedControlStores();
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitLdcInsn(Type.getObjectType(type));
            CONTROL_STACK_TRY_BLOCK_END.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingUnthrownException(String type) {
        // Records that we are returning and might instead have thrown an exception
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
        delegate.visitLdcInsn(Type.getObjectType(type));
        CONTROL_STACK_ADD_UNTHROWN_EXCEPTION.delegateVisit(delegate);
    }

    @Override
    public void visitingUnthrownExceptionCheck(String type) {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitLdcInsn(Type.getObjectType(type));
        CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION.delegateVisit(delegate);
    }

    @Override
    public void visitingTrackedInstanceOf() {
        delegate.visitInsn(DUP);
        GET_TAINT_OBJECT.delegateVisit(delegate);
        delegate.visitInsn(SWAP);
    }

    @Override
    public void visitingMaxs(int maxStack, int maxLocals) {

    }

    @Override
    public void onMethodExit(int opcode) {
        if(opcode == ATHROW) {
            passThroughDelegate.visitInsn(DUP);
            passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(passThroughDelegate);
        }
    }

    @Override
    public void visitingForceControlStore(Type stackTop) {
        if(!Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
            if(stackTop.getSort() != Type.OBJECT && stackTop.getSort() != Type.ARRAY) {
                if(stackTop.getSize() == 1) {
                    delegate.visitInsn(SWAP);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                    delegate.visitInsn(SWAP);
                } else {
                    delegate.visitInsn(DUP2_X1);
                    delegate.visitInsn(POP2);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                    delegate.visitInsn(DUP_X2);
                    delegate.visitInsn(POP);
                }
            } else {
                delegate.visitInsn(DUP);
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
            }
        }
    }

    @Override
    public void visitingJump(int opcode) {
        switch(opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
                // T V
                delegate.visitInsn(SWAP);
                pushBranchStarts();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
                break;
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                // objectref
                delegate.visitInsn(DUP);
                GET_TAINT_COPY_SIMPLE.delegateVisit(delegate);
                pushBranchStarts();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
                // T V T V
                int tmp = localVariableManager.getTmpLV(Type.INT_TYPE);
                // T V T V
                delegate.visitInsn(SWAP);
                delegate.visitInsn(TaintUtils.IS_TMP_STORE);
                delegate.visitVarInsn(Configuration.TAINT_STORE_OPCODE, tmp);
                // T V V
                delegate.visitInsn(DUP2_X1);
                delegate.visitInsn(POP2);
                // V V T
                delegate.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, tmp);
                localVariableManager.freeTmpLV(tmp);
                // V V T T
                COMBINE_TAGS.delegateVisit(delegate);
                // V V T
                pushBranchStarts();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
                break;
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ACMPEQ:
                // objectref objectref
                delegate.visitInsn(DUP2);
                GET_TAINT_COPY_SIMPLE.delegateVisit(delegate);
                // objectref objectref objectref T
                delegate.visitInsn(SWAP);
                GET_TAINT_COPY_SIMPLE.delegateVisit(delegate);
                COMBINE_TAGS.delegateVisit(delegate);
                // objectref objectref T
                pushBranchStarts();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
        }
    }

    @Override
    public void visitingSwitch() {
        delegate.visitInsn(SWAP);
        pushBranchStarts();
        delegate.visitInsn(POP); // Remove the taint tag
    }

    @Override
    public void storingReferenceInArray() {
        delegate.visitInsn(DUP);
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
    }

    @Override
    public void visitingExcludeRevisableBranches() {
        excludeNext = true;
    }

    /* stack_pre = Taint, stack_post = Taint */
    private void pushBranchStarts() {
        TaintMethodRecord pushMethod = localVariableManager.getIndexOfMasterExceptionLV() >= 0 ? CONTROL_STACK_PUSH_TAG_EXCEPTION : CONTROL_STACK_PUSH_TAG;
        for(BranchInfo info : nextBranch) {
            delegate.visitInsn(DUP);
            // T T
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(SWAP);
            // T ControlTaintTagStack T
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
            push(delegate, info.branchID);
            push(delegate, numberOfBranchIDs);
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
            }
            delegate.visitInsn(info.revisable ? ICONST_1 : ICONST_0);
            pushMethod.delegateVisit(delegate);
            delegate.visitVarInsn(ASTORE, localVariableManager.getIndexOfBranchesLV());
        }
        nextBranch.clear();
    }

    private void callPopControlTaint(int branchID) {
        if(branchID < 0) {
            callPopAll();
        } else {
            passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
            push(passThroughDelegate, branchID);
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
                CONTROL_STACK_POP_EXCEPTION.delegateVisit(passThroughDelegate);
            } else {
                CONTROL_STACK_POP.delegateVisit(passThroughDelegate);
            }
        }
    }

    /**
     * Pops off of the stack all of the tags pushed onto the ControlTaintTagStack during the execution of the method
     * being visited.
     */
    private void callPopAll() {
        passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
        if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
            passThroughDelegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
            CONTROL_STACK_POP_ALL_EXCEPTION.delegateVisit(passThroughDelegate);
        } else {
            CONTROL_STACK_POP_ALL.delegateVisit(passThroughDelegate);
        }
    }

    private void executeForcedControlStores() {
        if(!Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
            for(Field f : forceControlStoreFields) {
                addControlTagsToOwnedField(f);
            }
            for(int var : forceControlStoreVariables) {
                int shadowVar = -1;
                if(analyzer.locals.size() <= var || analyzer.locals.get(var) == Opcodes.TOP) {
                    continue;
                }
                if(var < lastParameterIndex && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
                    // Accessing an arg; remap it
                    Type localType = paramTypes[var];
                    if(localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
                        shadowVar = var - 1;
                    } else if(localType.getSort() == Type.ARRAY) {
                        continue;
                    }
                } else {
                    if(localVariableManager.varToShadowVar.containsKey(var)) {
                        shadowVar = localVariableManager.varToShadowVar.get(var);
                        if(analyzer.locals.get(var) instanceof String && ((String) analyzer.locals.get(var)).startsWith("[")) {
                            continue;
                        }
                        if(shadowVar >= analyzer.locals.size()
                                || analyzer.locals.get(shadowVar) instanceof Integer
                                || ((String) analyzer.locals.get(shadowVar)).startsWith("[")) {
                            continue;
                        }
                    }
                }
                if(shadowVar >= 0) {
                    delegate.visitVarInsn(ALOAD, shadowVar);
                    delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                    COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                    delegate.visitVarInsn(ASTORE, shadowVar);
                } else {
                    if(!(analyzer.locals.get(var) instanceof Integer)) {
                        delegate.visitVarInsn(ALOAD, var);
                        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                        COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
                    }
                }
            }
        }
        forceControlStoreFields.clear();
        forceControlStoreVariables.clear();
    }

    /**
     * Add tags from the ControlTaintTagStack to the specified field. The specified field
     * should be one that is owned by the class being visited.
     *
     * @param field the field whose associated taint tag should be updated
     */
    private void addControlTagsToOwnedField(Field field) {
        int getFieldOpcode = field.isStatic ? GETSTATIC : GETFIELD;
        int putFieldOpcode = field.isStatic ? PUTSTATIC : PUTFIELD;
        if(TaintUtils.isPrimitiveType(Type.getType(field.description))) {
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
                delegate.visitInsn(DUP);
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_CONTROL.delegateVisit(delegate);
            delegate.visitFieldInsn(putFieldOpcode, field.owner, field.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
        } else if(Type.getType(field.description).getSort() == Type.OBJECT) {
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name, field.description);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
        }
    }

    /**
     * Loads the specified value onto the stack.
     *
     * @param delegate the method visitor that should be used to load the specified value onto the stack
     * @param value    the value to be pushed onto the stack
     */
    public static void push(MethodVisitor delegate, int value) {
        if(value >= -1 && value <= 5) {
            delegate.visitInsn(Opcodes.ICONST_0 + value);
        } else if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            delegate.visitIntInsn(Opcodes.BIPUSH, value);
        } else if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            delegate.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            delegate.visitLdcInsn(value);
        }
    }

    /**
     * Record type that stores the identifier for a branch with whether or not the branch is considered to be revisable.
     */
    private static final class BranchInfo {
        final int branchID;
        final boolean revisable;

        BranchInfo(int branchID, boolean revisable) {
            this.branchID = branchID;
            this.revisable = revisable;
        }
    }
}