package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TaintThroughTaintingMV extends MethodVisitor implements Opcodes {

    private final String owner;
    private final String desc;
    private final boolean isStatic;

    public TaintThroughTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv);
        this.owner = owner;
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }


    private void loadReferenceTaintForThisToStack(){
        super.visitVarInsn(ALOAD, shadowVarsForArgs[0]);
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
            }
            taintArguments();
        }
        super.visitLdcInsn(value);
    }

    /* Adds code to add this instance's taint tags to the arguments passed to this method. */
    private void taintArguments() {
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for(int i = 0; i < args.length; i++) {
            if((args[i].getSort() == Type.OBJECT) || (args[i].getSort() == Type.ARRAY)) {
                // Argument is an object or array of objects
                super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                super.visitVarInsn(ALOAD, idx); // Load the argument onto the stack
                loadReferenceTaintForThisToStack();
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "addTaint", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
            }
            idx += args[i].getSize();
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(OpcodesUtil.isReturnOpcode(opcode)) {
            taintArguments();
            if (opcode != RETURN && this.shadowVarsForArgs != null) {

                super.visitInsn(ACONST_NULL);
                TaintMethodRecord.STACK_FRAME_FOR_METHOD_DEBUG.delegateVisit(mv); //Frame
                super.visitInsn(DUP); //Frame Frame
                TaintMethodRecord.GET_RETURN_TAINT.delegateVisit(mv); //Frame RetTaint
                loadReferenceTaintForThisToStack(); //Frame RetTaint thisTaint
                super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
                //Frame Taint
                TaintMethodRecord.SET_RETURN_TAINT.delegateVisit(mv);
            }
        }
        super.visitInsn(opcode);
    }
}
