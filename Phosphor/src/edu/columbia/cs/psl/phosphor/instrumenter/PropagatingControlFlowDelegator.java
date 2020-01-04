package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.Field;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

/**
 * Propagates control flows through a method by delegating instruction visits to a MethodVisitor.
 */
public class PropagatingControlFlowDelegator extends AbstractControlFlowDelegator {

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    /**
     * Tracks the current stack and local variable bindings.
     */
    private final NeverNullArgAnalyzerAdapter analyzer;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private final LocalVariableManager localVariableManager;

    /**
     * The number of unique identifiers for "branch" location in the method being visited.
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
     * The ID of the next branch instruction encountered or -1
     */
    private int nextBranchID = -1;

    public PropagatingControlFlowDelegator(MethodVisitor delegate, NeverNullArgAnalyzerAdapter analyzer,
                                           LocalVariableManager localVariableManager, int lastParameterIndex,
                                           Type[] paramTypes, int numberOfBranchIDs) {
        this.delegate = delegate;
        this.analyzer = analyzer;
        this.localVariableManager = localVariableManager;
        this.lastParameterIndex = lastParameterIndex;
        this.paramTypes = paramTypes;
        this.numberOfBranchIDs = numberOfBranchIDs;
    }

    @Override
    public void visitingIncrement(int var, int shadowVar) {
        delegate.visitVarInsn(ALOAD, shadowVar); // Current tag
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        COMBINE_TAGS_CONTROL.delegateVisit(delegate);
        delegate.visitVarInsn(ASTORE, shadowVar);
    }

    @Override
    public void visitingBranchStart(int branchID) {
        nextBranchID = branchID;
    }

    @Override
    public void visitingBranchEnd(int branchID) {
        if(localVariableManager.getIndexOfBranchesLV() != -1) {
            callPopControlTaint(branchID);
        }
    }

    @Override
    public void storingTaintedValue(int opcode, int var) {
        switch(opcode) {
            case ISTORE:
            case FSTORE:
            case DSTORE:
            case LSTORE:
            case ASTORE:
                // value taint
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
                COMBINE_TAGS_CONTROL.delegateVisit(delegate);
                break;
            case TaintUtils.FORCE_CTRL_STORE:
                forceControlStoreVariables.add(var);
        }
    }

    @Override
    public void visitingForceControlStoreField(Field field) {
        forceControlStoreFields.add(field);
    }

    @Override
    public void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint) {
        // val taint
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        COMBINE_TAGS_CONTROL.delegateVisit(delegate);
    }

    @Override
    public void generateEmptyTaint() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
            CONTROL_STACK_COPY_TAG_EXCEPTIONS.delegateVisit(delegate);
        } else {
            CONTROL_STACK_COPY_TAG.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingExceptionHandlerStart(String type) {
        if(type == null) {
            delegate.visitInsn(DUP2);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(DUP_X2);
            delegate.visitInsn(POP);
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
    public void onMethodExit(int opcode) {
        if(opcode == ATHROW) {
            delegate.visitInsn(DUP);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingForceControlStore(Type stackTop) {
        if(!Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_CONTROL.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingJump(int opcode, Label label) {
        switch(opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                // v t
                pushBranchStart();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ACMPEQ:
                // v1 t1 v2 t2
                delegate.visitInsn(DUP2_X1);
                // v1 v2 t2 t1 v2 t2
                delegate.visitInsn(POP2);
                // v1 v2 t2 t1
                COMBINE_TAGS.delegateVisit(delegate);
                // v1 v2 t
                pushBranchStart();
                delegate.visitInsn(POP); // Remove the taint tag
                executeForcedControlStores();
                break;
        }
        delegate.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
        pushBranchStart();
        delegate.visitInsn(POP); // Remove the taint tag
        delegate.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
        pushBranchStart();
        delegate.visitInsn(POP); // Remove the taint tag
        delegate.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    // stack_pre = [taint]
    // stack_post = [taint]
    private void pushBranchStart() {
        if(nextBranchID != -1) {
            TaintMethodRecord pushMethod;
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                pushMethod = CONTROL_STACK_PUSH_TAG_EXCEPTION;
            } else {
                pushMethod = CONTROL_STACK_PUSH_TAG;
            }
            delegate.visitInsn(DUP);
            // T T
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(SWAP);
            // T ControlTaintTagStack T
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
            push(delegate, nextBranchID);
            push(delegate, numberOfBranchIDs);
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
            }
            pushMethod.delegateVisit(delegate);
            delegate.visitVarInsn(ASTORE, localVariableManager.getIndexOfBranchesLV());
        }
        nextBranchID = -1;
    }

    private void callPopControlTaint(int branchID) {
        if(branchID < 0) {
            callPopAll();
        } else {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
            push(delegate, branchID);
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
                CONTROL_STACK_POP_EXCEPTION.delegateVisit(delegate);
            } else {
                CONTROL_STACK_POP.delegateVisit(delegate);
            }
        }
    }

    /**
     * Pops off of the stack all of the tags pushed onto the ControlTaintTagStack during the execution of the method
     * being visited.
     */
    private void callPopAll() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
        if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
            CONTROL_STACK_POP_ALL_EXCEPTION.delegateVisit(delegate);
        } else {
            CONTROL_STACK_POP_ALL.delegateVisit(delegate);
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
                if(var < lastParameterIndex && TaintUtils.isShadowedType(paramTypes[var])) {
                    // Accessing an arg; remap it
                    Type localType = paramTypes[var];
                    if(localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
                        shadowVar = var + localType.getSize();
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
}