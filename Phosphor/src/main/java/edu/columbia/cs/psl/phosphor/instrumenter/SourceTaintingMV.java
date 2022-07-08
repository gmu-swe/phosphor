package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class SourceTaintingMV extends MethodVisitor implements Opcodes {

    private static final SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance();
    private final String desc;
    private final boolean isStatic;
    private final Object lbl;
    /**
     * The original (before Phosphor added taint tracking information) descriptor of
     * the method being visited
     */
    private final String actualSource;

    public SourceTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv);
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.lbl = sourceSinkManager.getLabel(owner, name, desc);
        this.actualSource = owner + "." + name + desc;
    }

    /**
     * Adds code to make a call to autoTaint. Supplies the specified int as the
     * argument index.
     */
    private void callAutoTaint(int argIndex, Type expectedType) {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                Type.getDescriptor(TaintSourceWrapper.class));
        super.visitInsn(SWAP);
        super.visitLdcInsn(lbl);
        super.visitLdcInsn(actualSource);
        super.visitIntInsn(BIPUSH, argIndex);
        AUTO_TAINT.delegateVisit(this);
        if(TaintUtils.isWrappedType(expectedType)) {
            super.visitTypeInsn(CHECKCAST, TaintUtils.getWrapperType(expectedType).getInternalName());
        }
        else {
            super.visitTypeInsn(CHECKCAST, expectedType.getInternalName());
        }
    }

    private int[] shadowVarsForArgs;
    @Override
    public void visitLdcInsn(Object value) {
        if(shadowVarsForArgs == null && value instanceof String){
            String str = (String) value;
            if(str.startsWith("PhosphorArgTaintIndices=")){
                String[] parts = str.substring("PhosphorArgTaintIndices=".length()).split(",");
                shadowVarsForArgs = new int[parts.length];
                for(int i = 0; i< parts.length; i++){
                    shadowVarsForArgs[i] = Integer.parseInt(parts[i]);
                }
                autoTaintArguments();
            }
        }
        super.visitLdcInsn(value);
    }

    /**
     * Adds code to taint the arguments passed to this method.
     */
    private void autoTaintArguments() {
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for (int i = 0; i < args.length; i++) {
            if (args[i].getSort() == Type.OBJECT
                    || args[i].getSort() == Type.ARRAY) {
                super.visitVarInsn(ALOAD, idx); // load the argument onto the stack
                callAutoTaint(i, args[i]);
                super.visitVarInsn(ASTORE, idx); // replace the argument with the return of autoTaint
            }
            idx += args[i].getSize();
            //Also set the reference taint for this.
            super.visitVarInsn(ALOAD, shadowVarsForArgs[i]);
            callAutoTaint(i, Type.getType(Taint.class));
            super.visitVarInsn(ASTORE, shadowVarsForArgs[i]);
        }
        //Bad choices... if the method is non-static, there is one more taint than there is variables in the args array
        if(!isStatic) {
            super.visitVarInsn(ALOAD, shadowVarsForArgs[shadowVarsForArgs.length - 1]);
            callAutoTaint(0, Type.getType(Taint.class));
            super.visitVarInsn(ASTORE, shadowVarsForArgs[shadowVarsForArgs.length - 1]);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (OpcodesUtil.isReturnOpcode(opcode)) {
            autoTaintArguments();
            if (opcode != RETURN) {
                super.visitInsn(ACONST_NULL);
                STACK_FRAME_FOR_METHOD_DEBUG.delegateVisit(mv);
                super.visitInsn(DUP);

                GET_RETURN_TAINT.delegateVisit(mv);

                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                        Type.getDescriptor(TaintSourceWrapper.class));
                super.visitInsn(SWAP);
                super.visitLdcInsn(lbl);
                super.visitLdcInsn(actualSource);
                super.visitInsn(ICONST_M1);
                AUTO_TAINT.delegateVisit(this);
                super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);

                SET_RETURN_TAINT.delegateVisit(mv);
            }
        }
        super.visitInsn(opcode);
    }
}
