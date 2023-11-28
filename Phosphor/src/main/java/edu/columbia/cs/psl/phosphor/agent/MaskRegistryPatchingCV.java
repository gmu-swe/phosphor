package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecordImpl;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.mask.MaskRegistry;
import edu.columbia.cs.psl.phosphor.runtime.mask.Mask;
import edu.columbia.cs.psl.phosphor.runtime.mask.SunUnsafeMasker;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MaskRegistryPatchingCV extends ClassVisitor {
    private static final String MASK_REGISTRY_INTERNAL_NAME = Type.getInternalName(MaskRegistry.class);
    private static final String TARGET_METHOD_NAME = "initialize";
    private static final String MONITOR_DESC = Type.getDescriptor(Mask.class);
    private static final Class<?>[] MASKERS = new Class<?>[] {SunUnsafeMasker.class};

    public MaskRegistryPatchingCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    public static Map<String, MaskRegistry.MaskInfo> readMasks() {
        Map<String, MaskRegistry.MaskInfo> map = new HashMap<>();
        for (Class<?> clazz : MASKERS) {
            ClassNode cn = getClassNode(clazz);
            for (MethodNode mn : cn.methods) {
                readMasks(cn.name, mn, map);
            }
        }
        return map;
    }

    private static ClassNode getClassNode(Class<?> clazz) {
        try {
            ClassNode cn = new ClassNode();
            String name = clazz.getName().replace('.', '/') + ".class";
            ClassLoader classLoader = clazz.getClassLoader();
            try (InputStream in = classLoader == null || Phosphor.RUNTIME_INST
                    ? ClassLoader.getSystemResourceAsStream(name)
                    : classLoader.getResourceAsStream(name)) {
                if (in == null) {
                    throw new IllegalStateException("Failed to read class: " + clazz);
                }
                new ClassReader(in).accept(cn, ClassReader.SKIP_CODE);
            }
            return cn;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read class: " + clazz, e);
        }
    }

    private static void readMasks(String className, MethodNode mn, Map<String, MaskRegistry.MaskInfo> map) {
        if ((mn.access & Opcodes.ACC_STATIC) != 0 && mn.invisibleAnnotations != null) {
            MethodRecordImpl record = new MethodRecordImpl(Opcodes.INVOKESTATIC, className, mn.name, mn.desc, false);
            for (AnnotationNode an : mn.invisibleAnnotations) {
                if (MONITOR_DESC.equals(an.desc)) {
                    map.put(createKey(mn, an), new MaskRegistry.MaskInfo(record));
                }
            }
        }
    }

    private static String createKey(MethodNode mn, AnnotationNode an) {
        List<Object> values = an.values;
        String className = ((Type) values.get(1)).getInternalName();
        String methodName = mn.name;
        boolean isStatic = false;
        if (values.size() == 4) {
            isStatic = (boolean) values.get(3);
        }
        String descriptor = isStatic ? mn.desc : removeFirstParameter(mn.desc);
        return MaskRegistry.getKey(className, methodName, descriptor);
    }

    private static String removeFirstParameter(String descriptor) {
        int index = descriptor.indexOf(';');
        return '(' + descriptor.substring(index + 1);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return !name.equals(TARGET_METHOD_NAME) ? mv : new MaskRegistryPatchingMV(mv);
    }

    public static boolean isApplicable(String className) {
        return className.equals(MASK_REGISTRY_INTERNAL_NAME);
    }

    private static class MaskRegistryPatchingMV extends MethodVisitor {
        private final MethodVisitor target;

        public MaskRegistryPatchingMV(MethodVisitor mv) {
            super(Configuration.ASM_VERSION, null);
            target = mv;
        }

        @Override
        public void visitCode() {
            target.visitCode();
            Map<String, MaskRegistry.MaskInfo> masks = readMasks();
            for (String key : masks.keySet()) {
                MaskRegistry.MaskInfo mask = masks.get(key);
                target.visitLdcInsn(key);
                target.visitLdcInsn(mask.getRecord().getOwner());
                target.visitLdcInsn(mask.getRecord().getName());
                target.visitLdcInsn(mask.getRecord().getDescriptor());
                TaintMethodRecord.ADD_MASK.delegateVisit(target);
            }
            target.visitInsn(Opcodes.RETURN);
            target.visitMaxs(-1, -1);
            target.visitEnd();
        }
    }
}
