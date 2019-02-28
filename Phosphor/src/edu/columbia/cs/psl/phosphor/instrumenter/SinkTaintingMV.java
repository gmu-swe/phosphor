package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SinkTaintingMV extends MethodVisitor implements Opcodes {

    private final String owner;
    private final String name;
    private final String desc;
    private final boolean isStatic;
    private int numberOfRemainingTryCatchBlocks = 0;

    // Starts the scope of the try block
    private final Label startLabel;
    // Ends the scope of the try block starts the finally block
    private final Label endLabel;

    public SinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(ASM5, mv);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.startLabel = new Label();
        this.endLabel = new Label();
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // Check every arg to see if is taint tag
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        boolean skipNextPrimitive = false;
        for (int i = 0; i < args.length; i++) {
            if ((args[i].getSort() == Type.OBJECT && !args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) || args[i].getSort() == Type.ARRAY) {
                if ((args[i].getSort() == Type.ARRAY && (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC))
                        && args[i].getDimensions() == 1) || args[i].getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    if (!skipNextPrimitive) {
                        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                        super.visitVarInsn(ALOAD, idx);
                        super.visitLdcInsn(owner+"."+name+desc);
                        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                    }
                    skipNextPrimitive = !skipNextPrimitive;
                } else {
                    super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                    super.visitVarInsn(ALOAD, idx);
                    super.visitLdcInsn(owner+"."+name+desc);
                    super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
            } else if (!skipNextPrimitive) {
                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
                super.visitLdcInsn(owner+"."+name+desc);
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(" + Configuration.TAINT_TAG_DESC + "Ljava/lang/String;)V", false);
                skipNextPrimitive = true;
            } else if (skipNextPrimitive) {
                skipNextPrimitive = false;
            }
            idx += args[i].getSize();
        }
        // Call enteringSink before the original body code of the sink
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitLdcInsn(owner+"."+name+desc);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "enteringSink", "(Ljava/lang/String;)V", false);
        // If there are no other exception handlers for this method begin the try-finally block around the sink
        if(numberOfRemainingTryCatchBlocks == 0) {
            addTryCatchBlockHeader();
        }
    }

    @Override
    public void visitInsn(int opcode) {
        // Add the "finally" code before any return instructions
        if(TaintUtils.isReturnOpcode(opcode)) {
            sinkFinallyBlock();
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(endLabel); // Ends try block and starts finally block
        mv.visitFrame(F_NEW, 0, new Object[0], 1, new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(ASTORE, 1); // Push the throwable that was thrown onto the stack
        sinkFinallyBlock();
        mv.visitVarInsn(ALOAD, 1); // Pop the throwable that was thrown off the stack
        mv.visitInsn(ATHROW); // Throw the popped throwable
        super.visitMaxs(maxStack, maxLocals);
    }

    /* Adds code that makes a call to exitingSink at the end of a sink method. */
    private void sinkFinallyBlock() {
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
    private void addTryCatchBlockHeader(){
        mv.visitTryCatchBlock(startLabel, endLabel, endLabel, null);
        super.visitLabel(startLabel);
    }

    public void setNumberOfTryCatchBlocks(int num) {
        this.numberOfRemainingTryCatchBlocks = num;
    }
}
