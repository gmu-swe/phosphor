package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class SinkTaintingMV extends AdviceAdapter {

    // The arguments of the sink method being visited
    private final Type[] args;
    // The untainted signature of the sink method from which the sink method being visited inherited its status as a sink
    private final String baseSink;
    // The untainted signature of the sink method being visited
    private final String actualSink;
    // Whether the method being visited is static
    private final boolean isStatic;
    // Starts the scope of the try block
    private final Label startLabel;
    // Ends the scope of the try block starts the finally block
    private final Label endLabel;
    // The remaining number of try catch blocks that need to be visited before the sink try-catch can be visited
    private int numberOfRemainingTryCatchBlocks = 0;
    private final String owner;

    public SinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv, access, name, desc);
        this.args = Type.getArgumentTypes(desc);
        this.baseSink = BasicSourceSinkManager.getInstance().getBaseSink(owner, name, desc);
        this.actualSink = SourceSinkManager.getOriginalMethodSignature(owner, name, desc);
        this.isStatic = (access & ACC_STATIC) != 0;
        this.startLabel = new Label();
        this.endLabel = new Label();
        this.owner = owner;
    }

    /* Adds code to make a call to enteringSink. */
    private void callEnteringSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "enteringSink", "(Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    /* Adds code to add a non-taint tag object to the object array. */
    private void addObject(int arrayIdx, int idx) {
        // Duplicate the object array
        super.visitInsn(DUP);
        // Push the array index onto the stack
        push(arrayIdx);
        // Push the argument onto the stack
        super.visitVarInsn(ALOAD, idx);
        // Store the argument into the array
        super.visitInsn(AASTORE);
    }

    /* Adds code to wrap an taint tag argument with its primitive and add the wrapped object to the object array at the specified
     * index. */
    private void addWrappedPrimitive(int arrayIdx, int tagIdx, int primitiveIdx, Type primitiveType) {
        // Duplicate the object array
        super.visitInsn(DUP);
        // Push the array index onto the stack
        push(arrayIdx);
        // Wrap the primitive and tag together
        Type containerType = TaintUtils.getContainerReturnType(primitiveType);
        super.visitTypeInsn(NEW, containerType.getInternalName());
        super.visitInsn(DUP);
        super.visitVarInsn(ALOAD, tagIdx);
        super.visitVarInsn(primitiveType.getOpcode(ILOAD), primitiveIdx);
        String constructorType = primitiveType.getDescriptor();
        if(primitiveType.getSort() == Type.OBJECT || primitiveType.getSort() == Type.ARRAY) {
            constructorType = "Ljava/lang/Object;";
        }
        super.visitMethodInsn(INVOKESPECIAL, containerType.getInternalName(), "<init>", "(" + Configuration.TAINT_TAG_DESC +
                constructorType + ")V", false);
        // Store the wrapped value into the array
        super.visitInsn(AASTORE);
    }

    /* Adds the code to create an appropriately sized array for all of the objects that need to be checked for taint tags. */
    private void initializeArgumentArray() {
        int count = 0; // Subtract one if skipping a param
        for(int i = 0; i < args.length; i++) {
            if(args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) {
                // Argument is a taint tag
                count++;
            }
        }
        super.visitIntInsn(SIPUSH, count);
        super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        // Call enteringSink before the original body code of the sink
        callEnteringSink();
        // Add the auto-tainter to the stack
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        // Load this onto the stack for non-static methods or null for static methods
        if(isStatic) {
            super.visitInsn(ACONST_NULL);
        } else {
            loadThis();
        }
        // Initialize the array of objects to check
        initializeArgumentArray();
        int arrayIdx = 0;
        // Added objects that need to be checked to the array
        int idx = isStatic ? 0 : 1; // Start the arguments array after "this" argument for non-static methods
        for(int i = 0; i < args.length; i++) {
            if(args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) {
                // The argument is a taint tag
                int priorArgSize = (i == 0 ? 1 : args[i - 1].getSize());
                Type priorArgType = (i == 0 ? Type.getObjectType(owner) : args[i - 1]);
                addWrappedPrimitive(arrayIdx++, idx, idx - priorArgSize, priorArgType);
            }
            idx += args[i].getSize();
        }
        // Load the sink info
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        // Call checkTaint
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false);
        // If there are no other exception handlers for this method begin the try-finally block around the sink
        if(numberOfRemainingTryCatchBlocks == 0) {
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
        super.visitFrame(F_NEW, 0, new Object[0], 1, new Object[]{"java/lang/Throwable"});
        super.visitVarInsn(ASTORE, 1); // Push the throwable that was thrown onto the stack
        callExitingSink();
        super.visitVarInsn(ALOAD, 1); // Pop the throwable that was thrown off the stack
        super.visitInsn(ATHROW); // Throw the popped throwable
        super.visitMaxs(maxStack, maxLocals);
    }

    /* Adds code that makes a call to exitingSink at the end of a sink method. */
    private void callExitingSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(actualSink);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "exitingSink", "(Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        this.numberOfRemainingTryCatchBlocks--;
        if(this.numberOfRemainingTryCatchBlocks == 0) {
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
}
