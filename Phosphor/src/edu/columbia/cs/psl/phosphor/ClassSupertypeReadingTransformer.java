package edu.columbia.cs.psl.phosphor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;

/* Creates a class node containing information about its supertypes for each loaded class. */
public class ClassSupertypeReadingTransformer extends PhosphorBaseTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        ClassReader cr = new ClassReader(classfileBuffer);
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                ClassNode cn = new ClassNode();
                cn.name = name;
                cn.superName = superName;
                cn.interfaces = new ArrayList<>(Arrays.asList(interfaces));
                Instrumenter.classes.put(name, cn);
            }
        }, ClassReader.SKIP_CODE);
        return null;
    }
}

