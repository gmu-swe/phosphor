package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkTransformer;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;

/* Visits the <clint> method to add code to retransform the class on class initialization. */
public class ClinitRetransformMV extends MethodVisitor {

    // The name of the class that owns the method being visited
    private final String className;
    // Whether or not the version is at least the required version 1.5 for the ldc of a constant class
    private final boolean fixLdcClass;

    public ClinitRetransformMV(MethodVisitor mv, String className, boolean fixLdcClass) {
        super(Configuration.ASM_VERSION, mv);
        this.className = className;
        this.fixLdcClass = fixLdcClass;
    }

    @Override
    public void visitInsn(int opcode) {
        if(OpcodesUtil.isReturnOpcode(opcode)) {
            if(fixLdcClass) {
                // Since the class is not at least the required version 1.5 for the ldc of a constant class, push the class
                // onto the stack by making a call to Class.forName
                super.visitLdcInsn(className.replace("/", "."));
                NEW_EMPTY_TAINT.delegateVisit(mv);
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    /*If in implicit mode,  Class.forName is wrapped for the control tags.
                      If we call the wrapper, we'll get NoClassDefFound, because it will look
                      at the caller's  class in order to decide which class loader to use - and the caller
                      will be Java.lang.Class.forName(), which likely won't be in the same classloader
                      as the target class.
                    */
                    Configuration.controlFlowManager.visitCreateStack(mv, true);
                    super.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TaintedReferenceWithObjTag.class));
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TaintedReferenceWithObjTag.class), "<init>", "()V", false);
                    super.visitInsn(Opcodes.ACONST_NULL);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + CONTROL_STACK_DESC + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                } else {
                    super.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TaintedReferenceWithObjTag.class));
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TaintedReferenceWithObjTag.class), "<init>", "()V", false);
                    super.visitInsn(Opcodes.ACONST_NULL);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + Type.getDescriptor(TaintedReferenceWithObjTag.class) + "Ljava/lang/Class;)" + Type.getDescriptor(TaintedReferenceWithObjTag.class), false);
                }
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TaintedReferenceWithObjTag.class), "val", "Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");

            } else {
                // Directly push the class onto the stack
                mv.visitLdcInsn(Type.getObjectType(className));
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(SourceSinkTransformer.class), "retransform", "(Ljava/lang/Class;)V", false);
        }
        super.visitInsn(opcode);
    }

}
