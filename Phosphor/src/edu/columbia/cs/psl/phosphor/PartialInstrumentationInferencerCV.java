package edu.columbia.cs.psl.phosphor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.InstMethodSinkInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.SinkableArrayValue;

/**
 * Infers additional methods to instrument apart from the list fed to phosphor
 * 
 * @author manojthakur
 *
 */
public class PartialInstrumentationInferencerCV extends ClassVisitor {
	public static Map<String, List<String>> classToSuperClass = new HashMap<String, List<String>>();
	private static String superClassesToConsider = "java/io/.+|java/nio/.+|java/util/.+";

	List<MethodDescriptor> methodCallingAsStream = new ArrayList<MethodDescriptor>();
	String className;
	boolean isInterface = false;
	//	Map<MethodDescriptor, List<MethodDescriptor>> map = new HashMap<MethodDescriptor, List<MethodDescriptor>>();
	String[] interfaces = null;
	String superClass;
	List<String> superClasses = new ArrayList<String>();

	public PartialInstrumentationInferencerCV() {
		super(Opcodes.ASM5);
	}

	public PartialInstrumentationInferencerCV(final ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.interfaces = interfaces;
		this.superClass = superName;
		for (String inface : interfaces) {
			superClasses.add(inface);
		}

		if ((access & Opcodes.ACC_INTERFACE) != 0 || (access & Opcodes.ACC_ABSTRACT) != 0)
			isInterface = true;

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		final MethodDescriptor mdesc = new MethodDescriptor(name, className, desc);
		if (this.className.contains("NioEndpoint") && superClass.equals("java/util/concurrent/ConcurrentLinkedQueue") && name.equals("offer"))
			SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		if (this.className.equals("org/apache/catalina/startup/Catalina"))
			SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		if ((access & Opcodes.ACC_NATIVE) != 0)
			SelectiveInstrumentationManager.methodsGoEitherWay.add(mdesc);

		//		if(!this.className.startsWith("java/") && !this.className.startsWith("sun/") && !this.className.startsWith("javax/")) {
		//			Set<String> supers = ClassHierarchyCreator.allSupers(this.className);
		//			
		//			for(String inter : supers) {
		//				MethodDescriptor supr = new MethodDescriptor(name, inter, desc);
		//				if(SelectiveInstrumentationManager.methodsToInstrument.contains(supr)) {
		//					SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		//					break;
		//				}	
		//			}
		//		}

		//		for(String inter : interfaces) {
		//			MethodDescriptor supr = new MethodDescriptor(name, inter, desc);
		//			if(SelectiveInstrumentationManager.methodsToInstrument.contains(supr)) {
		//				SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		//				break;
		//			}	
		//		}
		//		MethodDescriptor supr = new MethodDescriptor(name, superClass, desc);
		//		
		//		if(SelectiveInstrumentationManager.methodsToInstrument.contains(supr))
		//			SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);

		//		MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
		//		map.put(mdesc, new ArrayList<MethodDescriptor>());
		//		return next;
		//		if (Instrumenter.isIgnoredMethodFromOurAnalysis(className, name, desc))
		//			return new MethodVisitor(Opcodes.ASM5, next) {
		//				@Override
		//				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		//					boolean hasPrimArrays = false;
		//					for (Type t : Type.getArgumentTypes(desc)) {
		//						if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT)
		//							hasPrimArrays = true;
		//					}
		//					if (hasPrimArrays && !Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc)) {
		//						SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		//					}
		//				}
		//			};
		//		else
		Type[] params = Type.getArgumentTypes(desc);
		for (Type t : params) {
			if (t.getSort() == Type.OBJECT || (t.getSort() == Type.ARRAY && (t.getDimensions() > 1 || t.getElementType().getSort() == Type.OBJECT))) {
				return null;
			}
		}
		return new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			boolean isBad = false;

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				super.visitMethodInsn(opcode, owner, name, desc, itf);
				if (!Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc, true))
					isBad = true;
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
					isBad = true;
				}
				super.visitFieldInsn(opcode, owner, name, desc);
			}

			@Override
			public void visitEnd() {
				if (!isBad) {
					SelectiveInstrumentationManager.methodsGoEitherWay.add(mdesc);
				} else if (!SelectiveInstrumentationManager.methodsGoEitherWay.contains(mdesc)) {
					isBad = false;
					Analyzer a = new Analyzer(new InstMethodSinkInterpreter(null));
					try {

						Frame[] fr = a.analyze(className, this);
						AbstractInsnNode insn = this.instructions.getFirst();
						int i = 0;
						while (insn != null) {
							Frame f = fr[i];
							if (f != null) {
								switch (insn.getOpcode()) {
								case Opcodes.AALOAD:
								case Opcodes.IALOAD:
								case Opcodes.FALOAD:
								case Opcodes.DALOAD:
								case Opcodes.CALOAD:
								case Opcodes.BALOAD:
								case Opcodes.SALOAD:
								case Opcodes.LALOAD:
									//are we loading from an array that came from a field
									Value v = f.getStack(f.getStackSize() - 2);
									if (v instanceof SinkableArrayValue)
										isBad = true;
									break;
								case Opcodes.INVOKEVIRTUAL:
								case Opcodes.INVOKESPECIAL:
								case Opcodes.INVOKESTATIC:
									MethodInsnNode min = (MethodInsnNode) insn;
									if (!Instrumenter.isIgnoredMethodFromOurAnalysis(min.owner, min.name, min.desc, true)) {
										//Check out the params...
										Type[] args = Type.getArgumentTypes(min.desc);
										for (int k = 0; k < args.length / 2; k++) {
											Type temp = args[k];
											args[k] = args[args.length - k - 1];
											args[args.length - k - 1] = temp;
										}
										int c = 0;
										for (Type t : args) {
											c += 1;
											v = f.getStack(f.getStackSize() - c);
											if (v instanceof SinkableArrayValue)
												isBad = true;
										}
									}
									//are we loading from an array that came from a field
									//									 v = (BasicValue) f.getStack(f.getStackSize()-2);
									//									if(v instanceof SinkableArrayValue)
									//										isBad = true;
									//									break;
								}
							}
							insn = insn.getNext();
							i++;
						}
					} catch (AnalyzerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (!isBad)
						SelectiveInstrumentationManager.methodsGoEitherWay.add(mdesc);
				}
				super.visitEnd();
			}
		};
	}
}
