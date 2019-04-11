package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class SinkTaintingMV extends AdviceAdapter {

    private final String owner;
    private final String name;
    private final String desc;
    // The sink from which this sink inherited its status as a sink
    private final String baseSink;
    private final boolean isStatic;
    private int numberOfRemainingTryCatchBlocks = 0;

    // Starts the scope of the try block
    private final Label startLabel;
    // Ends the scope of the try block starts the finally block
    private final Label endLabel;

    public SinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(ASM5, mv, access, name, desc);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.baseSink = BasicSourceSinkManager.getInstance().getBaseSink(owner, name, desc);
        this.isStatic = (access & ACC_STATIC) != 0;
        this.startLabel = new Label();
        this.endLabel = new Label();
    }

    /* Adds code to make a call to enteringSink. */
    private void callEnteringSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(owner+"."+name+desc);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "enteringSink", "(Ljava/lang/String;)V", false);
    }

    /* Adds code to make a call to checkTaint for a non taint tag object.*/
    private void callCheckTaintObject(int idx) {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitVarInsn(ALOAD, idx);
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(owner+"."+name+desc);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    /* Adds code to make a call to checkTaint for a taint tag*/
    private void callCheckTaintTag(int tagIdx, int primitiveIdx, Type primitiveType) {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        // Wrap the primitive and tag together
        Type containerType = TaintUtils.getContainerReturnType(primitiveType);
        mv.visitTypeInsn(NEW, containerType.getInternalName());
        mv.visitInsn(DUP);
        super.visitVarInsn(ALOAD, tagIdx);
        super.visitVarInsn(primitiveType.getOpcode(ILOAD), primitiveIdx);
        mv.visitMethodInsn(INVOKESPECIAL, containerType.getInternalName(), "<init>", "("+Configuration.TAINT_TAG_DESC+
                primitiveType.getDescriptor()+")V", false);
        // Load the sink info
        super.visitLdcInsn(baseSink);
        super.visitLdcInsn(owner+"."+name+desc);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false);
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        // Call enteringSink before the original body code of the sink
        callEnteringSink();
        // Check every arg to see if is taint tag
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for (int i = 0; i < args.length; i++) {
            if(args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) {
                // arg is a taint tag
                callCheckTaintTag(idx, idx + args[i].getSize(), args[i+1]);
            } else if(args[i].getSort() == Type.OBJECT) {
                // arg is an object
                callCheckTaintObject(idx);
            } else if(args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT) {
                // arg is an array of objects (possibly wrapped primitive array objects)
                callCheckTaintObject(idx);
            }
            idx += args[i].getSize();
        }
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
        mv.visitFrame(F_NEW, 0, new Object[0], 1, new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(ASTORE, 1); // Push the throwable that was thrown onto the stack
        callExitingSink();
        mv.visitVarInsn(ALOAD, 1); // Pop the throwable that was thrown off the stack
        mv.visitInsn(ATHROW); // Throw the popped throwable
        super.visitMaxs(maxStack, maxLocals);
    }

    /* Adds code that makes a call to exitingSink at the end of a sink method. */
    private void callExitingSink() {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(owner+"."+name+desc);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "exitingSink", "(Ljava/lang/String;)V", false);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        this.numberOfRemainingTryCatchBlocks--;
        if(this.numberOfRemainingTryCatchBlocks == 0){
            addTryCatchBlockHeader();
        }
    }

    private void addTryCatchBlockHeader() {
        mv.visitTryCatchBlock(startLabel, endLabel, endLabel, null);
        super.mark(startLabel);
    }

    public void setNumberOfTryCatchBlocks(int num) {
        this.numberOfRemainingTryCatchBlocks = num;
    }
}
