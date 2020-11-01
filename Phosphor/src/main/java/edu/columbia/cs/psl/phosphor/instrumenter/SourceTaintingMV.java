package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.AUTO_TAINT;

public class SourceTaintingMV extends MethodVisitor implements Opcodes {

    private static final SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance();
    private final String desc;
    private final boolean isStatic;
    private final Object lbl;
    /**
     * The original (before Phosphor added taint tracking information) descriptor of the method being visited
     */
    private final String actualSource;

    public SourceTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv);
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.lbl = sourceSinkManager.getLabel(owner, name, desc);
        this.actualSource = TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, desc);
    }

    /**
     * Adds code to make a call to autoTaint. Supplies the specified int as the argument index.
     */
    private void callAutoTaint(int argIndex, String internalName) {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitInsn(SWAP);
        super.visitLdcInsn(lbl);
        super.visitLdcInsn(actualSource);
        super.visitIntInsn(BIPUSH, argIndex);
        AUTO_TAINT.delegateVisit(this);
        super.visitTypeInsn(CHECKCAST, internalName);
    }

    /**
     * Adds code to taint the arguments passed to this method.
     */
    private void autoTaintArguments() {
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for(int i = 0; i < args.length; i++) {
            if(args[i].getSort() == Type.OBJECT || args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT) {
                super.visitVarInsn(ALOAD, idx); // load the argument onto the stack
                callAutoTaint(i, args[i].getInternalName());
                super.visitVarInsn(ASTORE, idx); // replace the argument with the return of autoTaint
            }
            idx += args[i].getSize();
            if(args[i].getDescriptor().equals("Ledu/columbia/cs/psl/phosphor/struct/TaintedReferenceWithObjTag;")) {
                //Anything after this is not a real param...
                return;
            }
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        autoTaintArguments();
    }

    @Override
    public void visitInsn(int opcode) {
        if(OpcodesUtil.isReturnOpcode(opcode)) {
            autoTaintArguments();
        }
        if(opcode == ARETURN) {
            callAutoTaint(-1, Type.getReturnType(desc).getInternalName());
        }
        super.visitInsn(opcode);
    }
}