package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Type.*;

/* Visits a Java class modifying the code for sink, source, and taintThrough methods. */
public class SourceSinkTaintingClassVisitor extends ClassVisitor {

    // The name of the class being visited
    private String className;

    public SourceSinkTaintingClassVisitor(ClassVisitor cv) {
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
        // Adds a SourceSinkTaintingMV to the chain if the method is not a native method and it is not a method for which
        // $$PHOSPHORTAGGED version should have been created.
        if(((access & Opcodes.ACC_NATIVE) == 0) && (name.contains(TaintUtils.METHOD_SUFFIX)) || !containsPrimitiveType(desc)) {
            final SourceSinkTaintingMV smv = new SourceSinkTaintingMV(mv, access, className, name, desc, desc);
            mv = new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions){
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    smv.setNumberOfTryCatchBlocks(this.tryCatchBlocks.size());
                    this.accept(smv);
                }
            };

        }
        return mv;
    }

    /* Returns whether the specified Type is a primitive type. */
    public static boolean isPrimitive(Type t) {
        switch(t.getSort()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return true;
            case ARRAY:
                return isPrimitive(t.getElementType());
            default:
                return false;
        }
    }

    /* Returns whether the specified method description indicates primitive or primitive array type in the parameter list */
    public static boolean containsPrimitiveType(String desc) {
        Type[] types = Type.getArgumentTypes(desc);
        for(Type type : types) {
            if(isPrimitive(type)) {
                return true;
            }
        }
        return false;
    }
}
