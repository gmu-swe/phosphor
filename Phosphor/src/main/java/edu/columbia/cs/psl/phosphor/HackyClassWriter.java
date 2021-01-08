package edu.columbia.cs.psl.phosphor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class HackyClassWriter extends ClassWriter {

    public HackyClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (PreMain.RUNTIME_INST) {
            return "java/lang/Object";
        }
        Class<?> clazz1;
        Class<?> clazz2;
        try {
            clazz1 = Class.forName(type1.replace('/', '.'), false, PreMain.bigLoader);
            clazz2 = Class.forName(type2.replace('/', '.'), false, PreMain.bigLoader);
        } catch (Throwable t) {
            return "java/lang/Object";
        }
        if (clazz1.isAssignableFrom(clazz2)) {
            return type1;
        } else if (clazz2.isAssignableFrom(clazz1)) {
            return type2;
        }
        if (clazz1.isInterface() || clazz2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                clazz1 = clazz1.getSuperclass();
            } while (!clazz1.isAssignableFrom(clazz2));
            return clazz1.getName().replace('.', '/');
        }
    }
}