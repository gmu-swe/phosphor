package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

public final class ClassNodeCache {
    public static Map<String, ClassNode> classes = Collections.synchronizedMap(new HashMap<>());

    static {
        classes.putAll(ClassSupertypeReadingTransformer.classNodes);
        ClassSupertypeReadingTransformer.classNodes = null;
    }

    private ClassNodeCache() {
        throw new AssertionError("Tried to instantiate static utility class: " + getClass());
    }

    /* Returns the class node associated with the specified class name or null if none exists and a new one could not
     * successfully be created for the class name. */
    public static ClassNode getClassNode(String className) {
        ClassNode cn = classes.get(className);
        if (cn == null) {
            // Class was loaded before ClassSupertypeReadingTransformer was added
            return tryToAddClassNode(className);
        } else {
            return cn;
        }
    }

    /* Attempts to create a ClassNode populated with supertype information for this class. */
    private static ClassNode tryToAddClassNode(String className) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(className + ".class")) {
            if (is == null) {
                return null;
            }
            ClassReader cr = new ClassReader(is);
            cr.accept(
                    new ClassVisitor(Configuration.ASM_VERSION) {
                        private ClassNode cn;

                        @Override
                        public void visit(
                                int version,
                                int access,
                                String name,
                                String signature,
                                String superName,
                                String[] interfaces) {
                            super.visit(version, access, name, signature, superName, interfaces);
                            cn = new ClassNode();
                            cn.name = name;
                            cn.superName = superName;
                            cn.interfaces = new java.util.ArrayList<>(java.util.Arrays.asList(interfaces));
                            cn.methods = new java.util.LinkedList<>();
                            classes.put(name, cn);
                        }

                        @Override
                        public MethodVisitor visitMethod(
                                int access, String name, String descriptor, String signature, String[] exceptions) {
                            cn.methods.add(new MethodNode(access, name, descriptor, signature, exceptions));
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    },
                    ClassReader.SKIP_CODE);
            return classes.get(className);
        } catch (Exception e) {
            return null;
        }
    }
}
