module edu.columbia.cs.psl.jigsaw.phosphor.instrumenter {
    exports edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;
    opens edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;
    opens edu.columbia.cs.psl.phosphor.instrumenter.analyzer;
    opens edu.columbia.cs.psl.phosphor.struct.harmony.util;
    opens edu.columbia.cs.psl.phosphor.control.graph;
    opens edu.columbia.cs.psl.phosphor.instrumenter;
    opens edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported;
    opens edu.columbia.cs.psl.phosphor.control.type;
    opens edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree;
    opens edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons;
    opens edu.columbia.cs.psl.phosphor.struct;
    opens edu.columbia.cs.psl.phosphor.control.standard;
    opens edu.columbia.cs.psl.phosphor.instrumenter.asm;
    opens edu.columbia.cs.psl.phosphor.control;
    opens edu.columbia.cs.psl.phosphor.org.objectweb.asm.analysis;
    opens edu.columbia.cs.psl.phosphor;
    opens edu.columbia.cs.psl.phosphor.org.objectweb.asm;
    opens edu.columbia.cs.psl.phosphor.runtime;
    opens edu.columbia.cs.psl.phosphor.runtime.proxied;
    requires jdk.jlink;
    requires java.instrument;
}
