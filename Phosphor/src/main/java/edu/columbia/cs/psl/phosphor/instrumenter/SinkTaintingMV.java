package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import jdk.internal.org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

public class SinkTaintingMV extends AdviceAdapter {

    // The arguments of the sink method being visited
    private final Type[] args;
    // The untainted signature of the sink method from which the sink method being
    // visited inherited its status as a sink
    private final String baseSink;
    // The untainted signature of the sink method being visited
    private final String actualSink;
    // Whether the method being visited is static
    private final boolean isStatic;
    // Starts the scope of the try block
    private final Label startLabel;
    // Ends the scope of the try block starts the finally block
    private final Label endLabel;
    // The remaining number of try catch blocks that need to be visited before the
    // sink try-catch can be visited
    private int numberOfRemainingTryCatchBlocks = 0;
    private final String owner;
    private boolean isConstructor;

    private int idxOfPhosphorStackFrame;
    private GeneratorAdapter generatorAdapter;

    public SinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv, access, name, desc);
        this.args = Type.getArgumentTypes(desc);
        this.baseSink = BasicSourceSinkManager.getInstance().getBaseSink(owner, name, desc);
        this.actualSink = owner + "." + name + desc;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.startLabel = new Label();
        this.endLabel = new Label();
        this.owner = owner;
        this.isConstructor = name.equals("<init>");
        Type[] args = Type.getArgumentTypes(desc);
        if((access & Opcodes.ACC_STATIC) == 0){
            idxOfPhosphorStackFrame = 1;
        }
        for(Type t : args){
            idxOfPhosphorStackFrame += t.getSize();
        }
        this.generatorAdapter = new GeneratorAdapter(this, access, name, desc);

    }

    /* Adds code to make a call to enteringSink. */
    private void callEnteringSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "enteringSink",
                "(Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    private int[] shadowVarsForArgs;
    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        if(shadowVarsForArgs == null && value instanceof String){
            String str = (String) value;
            if(str.startsWith("PhosphorArgTaintIndices=")){
                String[] parts = str.substring("PhosphorArgTaintIndices=".length()).split(",");
                shadowVarsForArgs = new int[parts.length];
                for(int i = 0; i< parts.length; i++){
                    shadowVarsForArgs[i] = Integer.parseInt(parts[i]);
                }
                if(!isConstructor){
                    this.checkTaintsOnArgs();
                }
            }
        }
    }

    private void checkTaintsOnArgs(){
        // Call enteringSink before the original body code of the sink
        callEnteringSink(); // start 2
        // Add the auto-tainter to the stack
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                Type.getDescriptor(TaintSourceWrapper.class));
        // Initialize the array of objects to check
        super.visitIntInsn(SIPUSH, args.length + (isStatic ? 0 : 1));
        super.visitTypeInsn(ANEWARRAY, "java/lang/Object");

        if(!isStatic){
            super.visitInsn(DUP);
            super.visitInsn(ICONST_0);
            super.visitVarInsn(ALOAD, 0);
            super.visitInsn(AASTORE);
        }

        int idx = isStatic ? 0 : 1; // Start the arguments array after "this" argument for non-static methods
        for (int i = 0; i < args.length; i++) {
            // Duplicate the object array
            super.visitInsn(DUP);
            generatorAdapter.push(i + (isStatic? 0 : 1));
            super.visitVarInsn(args[i].getOpcode(ILOAD), idx);
            generatorAdapter.box(args[i]);
            // Push the array index onto the stack
            super.visitInsn(AASTORE);
            idx += args[i].getSize();
        }
        // Load the taint tags
        super.visitIntInsn(SIPUSH, shadowVarsForArgs.length);
        super.visitTypeInsn(ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
        for(int i = 0; i < shadowVarsForArgs.length; i++){
            super.visitInsn(DUP);
            generatorAdapter.push(i);
            super.visitVarInsn(ALOAD, shadowVarsForArgs[i]);
            super.visitInsn(AASTORE);
        }

        // Load the sink info
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        // Call checkTaint
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint",
                "([Ljava/lang/Object;["+Configuration.TAINT_TAG_DESC+"Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        if(this.isConstructor) {
            checkTaintsOnArgs();
        }
        // If there are no other exception handlers for this method begin the
        // try-finally block around the sink
        if (numberOfRemainingTryCatchBlocks == 0) {
            addTryCatchBlockHeader();
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        // Add the call to exiting sink before the method exits
        callExitingSink();
        super.onMethodExit(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.mark(endLabel); // Ends try block and starts finally block
        super.visitFrame(F_NEW, 0, new Object[0], 1, new Object[] { "java/lang/Throwable" });
        super.visitVarInsn(ASTORE, 1); // Push the throwable that was thrown onto the stack
        callExitingSink();
        super.visitVarInsn(ALOAD, 1); // Pop the throwable that was thrown off the stack
        super.visitInsn(ATHROW); // Throw the popped throwable
        super.visitMaxs(maxStack, maxLocals);
    }

    /* Adds code that makes a call to exitingSink at the end of a sink method. */
    private void callExitingSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "exitingSink",
                "(Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        this.numberOfRemainingTryCatchBlocks--;
        if (this.numberOfRemainingTryCatchBlocks == 0) {
            addTryCatchBlockHeader();
        }
    }

    private void addTryCatchBlockHeader() {
        super.visitTryCatchBlock(startLabel, endLabel, endLabel, null);
        super.mark(startLabel);
    }

    public void setNumberOfTryCatchBlocks(int num) {
        this.numberOfRemainingTryCatchBlocks = num;
    }

    private AnalyzerAdapter analyzerAdapter;
    public void setAnalyzerAdapter(AnalyzerAdapter analyzerAdapter) {
        this.analyzerAdapter = analyzerAdapter;
    }
}
