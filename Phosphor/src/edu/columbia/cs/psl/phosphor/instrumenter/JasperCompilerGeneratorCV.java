package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class JasperCompilerGeneratorCV extends ClassVisitor {

    // The name of the class being visited
    private String className;
    // The name of the field holding the writer
    private static final String writerFieldName = "out";

    public JasperCompilerGeneratorCV(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(name.equals("genCommonPostamble")) {
            mv = new MethodVisitor(Opcodes.ASM5, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    // Load this onto the stack
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    // generateInheritedPlaceholders
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "generateInheritedPlaceholders", "()V", false);
                }
            };
        }
        return mv;
    }

    public void visitEnd() {
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "generateInheritedPlaceholders", "()V", null, null);
        mv.visitCode();
        createMethodBody(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        super.visitEnd();
    }

    /* Writes a new method using this generator instance's writer field with the specified method signature with the
     * specified dummy return line. */
    private void writePlaceholderMethod(MethodVisitor mv, String methodSignature, String returnLine) {
        // Load this onto the stack
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // Load this instance's writer field onto the stack
        mv.visitFieldInsn(Opcodes.GETFIELD, className, writerFieldName, "Lorg/apache/jasper/compiler/ServletWriter;");
        // Load another 5 copies of the writer field onto the stack
        for(int i = 0; i < 5; i++) {
            mv.visitInsn(Opcodes.DUP);
        }
        // Write the method signature
        mv.visitLdcInsn(methodSignature);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "printil", "(Ljava/lang/String;)V", false);
        // Write a new indent
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "pushIndent", "()V", false);
        // Write the return line
        mv.visitLdcInsn(returnLine);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "printil", "(Ljava/lang/String;)V", false);
        // Pop the indent
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "popIndent", "()V", false);
        // Write the closing bracket
        mv.visitLdcInsn("}");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "printil", "(Ljava/lang/String;)V", false);
        // Write the newline
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/jasper/compiler/ServletWriter", "println", "()V", false);
    }

    private void createMethodBody(MethodVisitor mv) {
        writePlaceholderMethod(mv, "public Object getPHOSPHOR_TAG() {", "return null;");
        writePlaceholderMethod(mv, "public void setPHOSPHOR_TAG(Object var1) {", "return;");
        writePlaceholderMethod(mv, "public void setPHOSPHOR_TAG(int t) {", "return;");
        writePlaceholderMethod(mv, "public Class getClass$$PHOSPHORTAGGED(edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack var1) {", "return null;");
        writePlaceholderMethod(mv, "public Class getClass$$PHOSPHORTAGGED() {", "return null;");
        writePlaceholderMethod(mv, "public String toString$$PHOSPHORTAGGED(edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack var1) {", "return null;");
        writePlaceholderMethod(mv, "public String toString$$PHOSPHORTAGGED() {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack phosphorJumpControlTag, edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag var2) {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag var1) {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag var1) {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack phosphorJumpControlTag, edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag Phosphor$$ReturnPreAllocated) {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag Phosphor$$ReturnPreAllocated) {", "return null;");
        writePlaceholderMethod(mv, "public edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(Object o, edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag Phosphor$$ReturnPreAllocated) {", "return null;");
    }
}
