package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecordImpl;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.mask.MaskRegistry;
import edu.columbia.cs.psl.phosphor.runtime.mask.Mask;
import edu.columbia.cs.psl.phosphor.runtime.mask.SunUnsafeMasker;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class MaskRegistryPatchingCV extends ClassVisitor {
    private static final String MASK_REGISTRY_INTERNAL_NAME = Type.getInternalName(MaskRegistry.class);
    private static final String TARGET_METHOD_NAME = "initialize";
    private static final Class<?>[] MASKERS = new Class<?>[] {SunUnsafeMasker.class};

    public MaskRegistryPatchingCV(ClassVisitor cv) {
        super(Configuration.ASM_VERSION, cv);
    }

    public static Map<String, MaskRegistry.MaskInfo> readMasks() {
        Map<String, MaskRegistry.MaskInfo> map = new HashMap<>();
        for (Class<?> clazz : MASKERS) {
            for (Method method : clazz.getDeclaredMethods()) {
                for (Mask mask : method.getAnnotationsByType(Mask.class)) {
                    MethodRecordImpl record = new MethodRecordImpl(
                            Opcodes.INVOKEVIRTUAL,
                            Type.getInternalName(clazz),
                            method.getName(),
                            Type.getMethodDescriptor(method),
                            false);
                    String key = createKey(record, mask);
                    map.put(key, new MaskRegistry.MaskInfo(record));
                }
            }
        }
        return map;
    }

    private static String createKey(MethodRecordImpl record, Mask mask) {
        String className = Type.getInternalName(mask.owner());
        String descriptor = mask.isStatic() ? record.getDescriptor() : removeFirstParameter(record.getDescriptor());
        return MaskRegistry.getKey(className, record.getName(), descriptor);
    }

    private static String removeFirstParameter(String descriptor) {
        return '(' + descriptor.substring(descriptor.indexOf(';') + 1);
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
