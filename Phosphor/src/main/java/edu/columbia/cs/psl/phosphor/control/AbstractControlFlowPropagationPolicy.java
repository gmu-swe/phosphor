package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;

public abstract class AbstractControlFlowPropagationPolicy<T extends ControlFlowAnalyzer> implements ControlFlowPropagationPolicy {

    /**
     * The analyzer used to annotate control flow information is the method being visited.
     */
    protected final T flowAnalyzer;

    /**
     * Visitor to which instruction visiting is delegated.
     */
    protected MethodVisitor delegate;

    /**
     * Tracks the current stack and local variable bindings.
     */
    protected NeverNullArgAnalyzerAdapter analyzer;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    protected LocalVariableManager localVariableManager;

    public AbstractControlFlowPropagationPolicy(T flowAnalyzer) {
        this.flowAnalyzer = flowAnalyzer;
    }

    @Override
    public T getFlowAnalyzer() {
        return flowAnalyzer;
    }

    @Override
    public void initialize(MethodVisitor delegate, LocalVariableManager localVariableManager, NeverNullArgAnalyzerAdapter analyzer) {
        this.delegate = delegate;
        this.localVariableManager = localVariableManager;
        this.analyzer = analyzer;
    }

    @Override
    public void initializeLocalVariables(MethodVisitor mv) {

    }

    @Override
    public void visitingMaxs() {

    }

    @Override
    public void preparingFrame() {

    }

    @Override
    public void poppingFrame(MethodVisitor mv) {

    }

    @Override
    public LocalVariable[] createdLocalVariables() {
        return new LocalVariable[0];
    }

    @Override
    public void visitingIncrement(int var, int shadowVar) {

    }

    @Override
    public void visitingLocalVariableStore(int opcode, int var) {

    }

    @Override
    public void visitingArrayStore(int opcode) {

    }

    @Override
    public void visitingArrayLoad(int opcode) {

    }

    @Override
    public void visitingFieldStore(int opcode, String owner, String name, String descriptor) {

    }

    @Override
    public void visitingInstanceFieldLoad(String owner, String name, String descriptor) {

    }

    @Override
    public void onMethodExit(int opcode) {

    }

    @Override
    public void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info) {

    }

    @Override
    public void visitingInstanceOf() {

    }
}
