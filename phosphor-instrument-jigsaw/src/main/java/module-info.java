module edu.columbia.cs.psl.jigsaw.phosphor.instrumenter {
    exports edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;
    opens edu.columbia.cs.psl.phosphor.instrumenter;
    opens edu.columbia.cs.psl.phosphor.struct.harmony.util;
    opens edu.columbia.cs.psl.phosphor.org.objectweb.asm;
    requires jdk.jlink;
    requires java.instrument;
}
