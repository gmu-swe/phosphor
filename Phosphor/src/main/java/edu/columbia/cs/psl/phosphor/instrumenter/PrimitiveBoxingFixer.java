package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PrimitiveBoxingFixer extends TaintAdapter implements Opcodes {

    // private boolean hackOnTaint = false;
    public PrimitiveBoxingFixer(int access, String className, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
        super(access, className, name, desc, signature, exceptions, mv, analyzer);
        //     this.hackOnTaint = (className.equals("java/lang/Float") || className.equals("java/lang/Double") && name.equals("valueOf$$PHOSPHORTAGGED"));
    }
}
