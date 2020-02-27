package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.type;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AnalyzerAdapter;

public final class VersionConfiguredAnalyzerAdapter extends AnalyzerAdapter {

    public VersionConfiguredAnalyzerAdapter(String owner, int access, String name, String descriptor, MethodVisitor methodVisitor) {
        super(Configuration.ASM_VERSION, owner, access, name, descriptor, methodVisitor);
    }
}
