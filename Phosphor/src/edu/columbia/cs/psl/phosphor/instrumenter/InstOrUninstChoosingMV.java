package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.MethodVisitor;

public class InstOrUninstChoosingMV extends MethodVisitor {

    private UninstrumentedCompatMV umv;

    public InstOrUninstChoosingMV(TaintPassingMV tmv, UninstrumentedCompatMV umv) {
        super(Configuration.ASM_VERSION, tmv);
        this.umv = umv;
    }

    public void disableTainting() {
        this.mv = umv;
    }
}
