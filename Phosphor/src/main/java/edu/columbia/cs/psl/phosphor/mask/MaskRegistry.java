package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.agent.MaskRegistryPatchingCV;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecordImpl;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Opcodes;

public final class MaskRegistry {
    private static final Map<String, MaskInfo> masks = new HashMap<>();

    private MaskRegistry() {
        throw new AssertionError();
    }

    static {
        initialize();
    }

    public static MaskInfo getMask(String className, String methodName, String descriptor) {
        return masks.get(getKey(className, methodName, descriptor));
    }

    public static String getKey(String className, String methodName, String descriptor) {
        return className + "." + methodName + descriptor;
    }

    /**
     * The body of this method is replaced by {@link MaskRegistryPatchingCV} when the Phosphor is
     * JAR is patched.
     */
    private static void initialize() {
        masks.putAll(MaskRegistryPatchingCV.readMasks());
        if (Phosphor.RUNTIME_INST) {
            throw new AssertionError("Calling unpatched method body");
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.ADD_MASK)
    private static void addMask(String key, String owner, String name, String descriptor) {
        MethodRecordImpl record = new MethodRecordImpl(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
        masks.put(key, new MaskInfo(record));
    }

    public static class MaskInfo {
        private final MethodRecordImpl record;

        public MaskInfo(MethodRecordImpl record) {
            if (record == null) {
                throw new NullPointerException();
            }
            this.record = record;
        }

        public MethodRecordImpl getRecord() {
            return record;
        }
    }
}
