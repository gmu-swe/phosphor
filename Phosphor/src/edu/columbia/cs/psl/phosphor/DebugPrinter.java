package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer;
import edu.columbia.cs.psl.phosphor.instrumenter.UnnecessaryTaintLoadRemover;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassWriter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.JSRInlinerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.TraceClassVisitor;

public class DebugPrinter {
	public static void main(String[] args) throws Exception {
//		Configuration.IMPLICIT_TRACKING = true;
		File clazz = new File(args[0]);
		final ClassReader cr1 = new ClassReader(new FileInputStream(clazz));
		PrintWriter pw = new PrintWriter(new FileWriter("z.txt"));
		
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
		{
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				try{
				return super.getCommonSuperClass(type1, type2);
				}
				catch(Exception ex)
				{
//					System.err.println("err btwn " + type1 + " " +type2);
				return "java/lang/Unknown";
				}
			}
		};
		cr1.accept(new ClassVisitor(Opcodes.ASM4,cw1) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
			}
		}, ClassReader.EXPAND_FRAMES);
		final ClassReader cr = new ClassReader(cw1.toByteArray());

		TraceClassVisitor tcv = new TraceClassVisitor(pw){
			String className;
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, superName, interfaces);
				this.className =name;
			}
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				mv = new PrimitiveArrayAnalyzer(cr.getClassName(), access, name, desc, signature, exceptions, mv);
				NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(cr.getClassName(), access, name, desc, mv);
				((PrimitiveArrayAnalyzer)mv).setAnalyzer(an);
				if(!Configuration.IMPLICIT_TRACKING)
				mv = new UnnecessaryTaintLoadRemover(className, access, name, desc, signature, exceptions, an);
				return mv;
			}
		};
		cr.accept(tcv, ClassReader.EXPAND_FRAMES);
		pw.flush();
	}
}
