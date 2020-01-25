package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.control.ControlFlowAnalyzer;
import org.objectweb.asm.tree.MethodNode;

public class NoControlFlowAnalyzer implements ControlFlowAnalyzer {

    @Override
    public void annotate(String owner, MethodNode methodNode) {
        // No need to add any annotations
    }
}
