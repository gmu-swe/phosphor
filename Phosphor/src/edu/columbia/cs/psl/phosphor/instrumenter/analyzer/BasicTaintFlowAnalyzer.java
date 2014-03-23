package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;

public class BasicTaintFlowAnalyzer extends ClassVisitor {

	public BasicTaintFlowAnalyzer(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}
	String className;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		BasicDataflowAnalysis bda = new BasicDataflowAnalysis(className, access, name, desc, signature, exceptions, mv);
		
		return bda;
	}

}
