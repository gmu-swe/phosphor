package edu.columbia.cs.psl.phosphor.control;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

public class ControlAnalysisTestUtil {

    private ControlAnalysisTestUtil() {

    }

    public static MethodNode getMethodNode(Class<?> clazz, String methodName) throws NoSuchMethodException, IOException {
        ClassReader cr = new ClassReader(clazz.getName());
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for(MethodNode mn : classNode.methods) {
            if(mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }
}
