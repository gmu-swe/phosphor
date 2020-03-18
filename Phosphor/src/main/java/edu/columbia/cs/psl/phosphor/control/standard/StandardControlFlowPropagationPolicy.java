package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.AbstractControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.LocalVariable;
import edu.columbia.cs.psl.phosphor.control.standard.ForceControlStore.ForceControlStoreField;
import edu.columbia.cs.psl.phosphor.control.standard.ForceControlStore.ForceControlStoreLocal;
import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecord;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import edu.columbia.cs.psl.phosphor.struct.Field;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy.push;
import static edu.columbia.cs.psl.phosphor.control.standard.ControlMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.COMBINE_TAGS;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.COMBINE_TAGS_ON_OBJECT_CONTROL;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintPassingMV.calculateParamTypes;

/**
 * Propagates control flows through a method by delegating instruction visits to a MethodVisitor.
 */
public class StandardControlFlowPropagationPolicy extends AbstractControlFlowPropagationPolicy<StandardControlFlowAnalyzer> {

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
     * The number of unique IDs assigned to branches in the method
     */
    private int numberOfUniqueBranchIDs = 0;
    /**
     * The ID of the next branch instruction encountered or -1
     */
    private int nextBranchID = -1;

    /**
     * The local variable index of the EnqueuedTaint instance
     */
    private int enqueuedTaintIndex = -1;

    /**
     * The local variable index of the ExceptionalTaintData instance
     */
    private int exceptionTaintDataIndex = -1;

    /**
     * The local variable index of the int[] instance for storing pushed branches
     */
    private int pushedBranchesIndex = -1;

    private LocalVariable[] createdLocalVariables = new LocalVariable[0];

    public StandardControlFlowPropagationPolicy(StandardControlFlowAnalyzer flowAnalyzer, boolean isStatic, String descriptor) {
        super(flowAnalyzer);
        this.paramTypes = calculateParamTypes(isStatic, descriptor);
        this.lastParameterIndex = paramTypes.length - 1;
    }

    @Override
    public void poppingFrame(MethodVisitor mv) {
        callPopAll(mv);
    }

    @Override
    public LocalVariable[] createdLocalVariables() {
        return createdLocalVariables;
    }

    @Override
    public void initializeLocalVariables(MethodVisitor mv) {
        numberOfUniqueBranchIDs = flowAnalyzer.getNumberOfUniqueBranchIDs();
        SinglyLinkedList<LocalVariable> newLocalVariables = new SinglyLinkedList<>();
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && flowAnalyzer.getNumberOfTryCatch() > 0) {
            enqueuedTaintIndex = localVariableManager.createPermanentLocalVariable(EnqueuedTaint.class, "enqueuedTaint");
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitVarInsn(ASTORE, enqueuedTaintIndex);
            newLocalVariables.push(new LocalVariable(enqueuedTaintIndex, Type.getInternalName(EnqueuedTaint.class)));
        }
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && flowAnalyzer.getNumberOfThrows() > 0) {
            // Create a local variable for the exception data
            exceptionTaintDataIndex = localVariableManager.createPermanentLocalVariable(ExceptionalTaintData.class, "phosphorExceptionTaintData");
            mv.visitTypeInsn(NEW, Type.getInternalName(ExceptionalTaintData.class));
            mv.visitInsn(DUP);
            delegate.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ExceptionalTaintData.class), "<init>", "()V", false);
            delegate.visitVarInsn(ASTORE, exceptionTaintDataIndex);
            newLocalVariables.push(new LocalVariable(exceptionTaintDataIndex, Type.getInternalName(ExceptionalTaintData.class)));
        }
        if(!Configuration.IMPLICIT_HEADERS_NO_TRACKING && !Configuration.WITHOUT_PROPAGATION && numberOfUniqueBranchIDs > 0) {
            // Create a local variable for the array used to track tags pushed for each "branch" location
            mv.visitInsn(Opcodes.ACONST_NULL);
            pushedBranchesIndex = localVariableManager.createPermanentLocalVariable(int[].class, "pushedBranches");
            mv.visitVarInsn(Opcodes.ASTORE, pushedBranchesIndex);
            newLocalVariables.push(new LocalVariable(pushedBranchesIndex, Type.getInternalName(int[].class)));
        }
        createdLocalVariables = newLocalVariables.toArray(new LocalVariable[0]);
    }

    @Override
    public void visitingIncrement(int var, int shadowVar) {
        delegate.visitVarInsn(ALOAD, shadowVar); // Current tag
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
        delegate.visitVarInsn(ASTORE, shadowVar);
    }

    @Override
    public void visitingLocalVariableStore(int opcode, int var) {
        switch(opcode) {
            case ISTORE:
            case FSTORE:
            case DSTORE:
            case LSTORE:
            case ASTORE:
                // value taint
                copyTag();
                COMBINE_TAGS.delegateVisit(delegate);
                break;
        }
    }

    @Override
    public void visitingArrayStore(int opcode) {
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
    }

    @Override
    public void visitingFieldStore(int opcode, String owner, String name, String descriptor) {
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
    }

    @Override
    public void generateEmptyTaint() {
        copyTag();
    }

    @Override
    public void onMethodExit(int opcode) {
        if(opcode == ATHROW) {
            // Exception Taint
            copyTag();
            COMBINE_TAGS.delegateVisit(delegate);
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
    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
        pushBranchStart();
        delegate.visitInsn(POP); // Remove the taint tag
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
        pushBranchStart();
        delegate.visitInsn(POP); // Remove the taint tag
    }

    @Override
    public void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info) {
        if(info instanceof ForceControlStoreField) {
            forceControlStoreFields.add(((ForceControlStoreField) info).getField());
        } else if(info instanceof ForceControlStoreLocal) {
            forceControlStoreVariables.add(((ForceControlStoreLocal) info).getLocalVariableIndex());
        } else if(info instanceof ExecuteForceControlStore) {
            if(!analyzer.stack.isEmpty() && !Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
                copyTag();
                COMBINE_TAGS.delegateVisit(delegate);
            }
        } else if(info instanceof BranchStart) {
            nextBranchID = ((BranchStart) info).getBranchID();
        } else if(info instanceof BranchEnd) {
            int branchID = ((BranchEnd) info).getBranchID();
            if(pushedBranchesIndex != -1) {
                callPopControlTaint(branchID);
            }
        } else if(info instanceof ExceptionHandlerStart) {
            exceptionHandlerStart(((ExceptionHandlerStart) info).getExceptionType());
        } else if(info instanceof ExceptionHandlerEnd) {
            exceptionHandlerEnd(((ExceptionHandlerEnd) info).getExceptionType());
        } else if(info instanceof UnthrownException) {
            unthrownException(((UnthrownException) info).getExceptionType());
        } else if(info instanceof UnthrownExceptionCheck) {
            unthrownExceptionCheck(((UnthrownExceptionCheck) info).getExceptionType());
        }
    }

    /**
     * stack_pre = [taint]
     * stack_post = [taint]
     **/
    private void pushBranchStart() {
        if(nextBranchID != -1) {
            MethodRecord pushMethod;
            if(exceptionTaintDataIndex >= 0) {
                pushMethod = STANDARD_CONTROL_STACK_PUSH_TAG_EXCEPTION;
            } else {
                pushMethod = STANDARD_CONTROL_STACK_PUSH_TAG;
            }
            delegate.visitInsn(DUP);
            // T T
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(SWAP);
            // T ControlFlowStack T
            delegate.visitVarInsn(ALOAD, pushedBranchesIndex);
            push(delegate, nextBranchID);
            push(delegate, numberOfUniqueBranchIDs);
            if(exceptionTaintDataIndex >= 0) {
                delegate.visitVarInsn(ALOAD, exceptionTaintDataIndex);
            }
            pushMethod.delegateVisit(delegate);
            delegate.visitVarInsn(ASTORE, pushedBranchesIndex);
        }
        nextBranchID = -1;
    }

    private void callPopControlTaint(int branchID) {
        if(branchID < 0) {
            callPopAll(delegate);
        } else {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitVarInsn(ALOAD, pushedBranchesIndex);
            push(delegate, branchID);
            if(exceptionTaintDataIndex >= 0) {
                delegate.visitVarInsn(ALOAD, exceptionTaintDataIndex);
                STANDARD_CONTROL_STACK_POP_EXCEPTION.delegateVisit(delegate);
            } else {
                STANDARD_CONTROL_STACK_POP.delegateVisit(delegate);
            }
        }
    }

    /**
     * Pops off of the stack all of the tags pushed onto the ControlFlowStack during the execution of the method
     * being visited.
     */
    private void callPopAll(MethodVisitor mv) {
        if(pushedBranchesIndex >= 0) {
            mv.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            mv.visitVarInsn(ALOAD, pushedBranchesIndex);
            if(exceptionTaintDataIndex >= 0) {
                mv.visitVarInsn(ALOAD, exceptionTaintDataIndex);
                STANDARD_CONTROL_STACK_POP_ALL_EXCEPTION.delegateVisit(mv);
            } else {
                STANDARD_CONTROL_STACK_POP_ALL.delegateVisit(mv);
            }
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
                    Map<Integer, Integer> varToShadowVar = localVariableManager.getVarToShadowVar();
                    if(varToShadowVar.containsKey(var)) {
                        shadowVar = varToShadowVar.get(var);
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
                    copyTag();
                    COMBINE_TAGS.delegateVisit(delegate);
                    delegate.visitVarInsn(ASTORE, shadowVar);
                } else {
                    if(!(analyzer.locals.get(var) instanceof Integer)) {
                        // Probably wrong since reference tainting update
                        // TODO fix or remove
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
     * Add tags from the ControlFlowStack to the specified field. The specified field
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
            copyTag();
            COMBINE_TAGS.delegateVisit(delegate);
            delegate.visitFieldInsn(putFieldOpcode, field.owner, field.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
        } else if(Type.getType(field.description).getSort() == Type.OBJECT) {
            // Probably wrong since reference tainting update
            // TODO fix or remove
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name, field.description);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT_CONTROL.delegateVisit(delegate);
        }
    }

    private void exceptionHandlerStart(String type) {
        if(type == null) {
            delegate.visitInsn(DUP2);
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitInsn(DUP_X2);
            delegate.visitInsn(POP);
            delegate.visitVarInsn(ALOAD, enqueuedTaintIndex);
            STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_START.delegateVisit(delegate);
            delegate.visitVarInsn(ASTORE, enqueuedTaintIndex);
            executeForcedControlStores();
        } else {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitLdcInsn(Type.getObjectType(type));
            STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_START_TYPES.delegateVisit(delegate);
        }
    }

    private void exceptionHandlerEnd(String type) {
        if(type == null) {
            // End of a handler
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitVarInsn(ALOAD, enqueuedTaintIndex);
            STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_END.delegateVisit(delegate);
        } else {
            // End of a try block
            executeForcedControlStores();
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            delegate.visitLdcInsn(Type.getObjectType(type));
            STANDARD_CONTROL_STACK_TRY_BLOCK_END.delegateVisit(delegate);
        }
    }

    private void unthrownException(String type) {
        // Records that we are returning and might instead have thrown an exception
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitVarInsn(ALOAD, exceptionTaintDataIndex);
        delegate.visitLdcInsn(Type.getObjectType(type));
        STANDARD_CONTROL_STACK_ADD_UNTHROWN_EXCEPTION.delegateVisit(delegate);
    }

    private void unthrownExceptionCheck(String type) {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitLdcInsn(Type.getObjectType(type));
        STANDARD_CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION.delegateVisit(delegate);
    }

    private void copyTag() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        STANDARD_CONTROL_STACK_COPY_TAG.delegateVisit(delegate);
    }
}