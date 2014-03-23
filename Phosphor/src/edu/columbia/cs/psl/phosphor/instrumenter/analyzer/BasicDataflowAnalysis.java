package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.Analyzer;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.AnalyzerException;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.Frame;

public class BasicDataflowAnalysis extends MethodVisitor {

	public BasicDataflowAnalysis(final String className, final int access, final String name, final String desc, final String signature, final String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {

			@Override
			public void visitEnd() {
				super.visitEnd();
				Analyzer<BasicTaintedValue> analyzer = new Analyzer<BasicTaintedValue>(new BasicTaintedInterpreter());
				try {
					System.out.println(className+"."+name+desc);
					Frame[] res = analyzer.analyze(className, this);
					for(int i = 0; i < res.length; i++)
					{
						if(res[i] != null)
						System.out.println(i+": "+res[i].toString());
					}

				} catch (AnalyzerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(cmv != null)
					this.accept(cmv);
			}
		});

	}

}
