package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import org.objectweb.asm.MethodVisitor;

public abstract class ReflectionMV extends MethodVisitor {
    protected ReflectionMV(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    public void setLvs(LocalVariableManager lvs) {
    }
}
