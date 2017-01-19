package edu.columbia.cs.psl.phosphor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Infers additional methods to instrument apart from the list fed to phosphor
 * 
 * @author manojthakur
 *
 */
public class PartialInstrumentationInferencerCV extends ClassVisitor{
	public static Map<String, List<String>> classToSuperClass = new HashMap<String, List<String>>();
	private static String superClassesToConsider = "java/io/.+|java/nio/.+|java/util/.+";
	
	List<MethodDescriptor> methodCallingAsStream = new ArrayList<MethodDescriptor>();
	String className;
	boolean isInterface = false;
	Map<MethodDescriptor, List<MethodDescriptor>> map = new HashMap<MethodDescriptor, List<MethodDescriptor>>();
	String[] interfaces = null;
	String superClass;
	List<String> superClasses = new ArrayList<String>();
	
	public PartialInstrumentationInferencerCV()  {
		super(Opcodes.ASM5);
	}
	
	public PartialInstrumentationInferencerCV(final ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}
		
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		this.className = name;
		this.interfaces = interfaces;
		this.superClass = superName;
		for(String inface : interfaces) {
			superClasses.add(inface);
		}
		
		if((access & Opcodes.ACC_INTERFACE) != 0 || (access & Opcodes.ACC_ABSTRACT) != 0)
			isInterface = true;
		
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
			
		MethodDescriptor mdesc = new MethodDescriptor(name, className, desc);
		
		if(this.className.contains("NioEndpoint") && superClass.equals("java/util/concurrent/ConcurrentLinkedQueue") && name.equals("offer"))
			SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		if(this.className.equals("org/apache/catalina/startup/Catalina"))
			SelectiveInstrumentationManager.methodsToInstrument.add(mdesc);
		
		
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
		
		MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
		map.put(mdesc, new ArrayList<MethodDescriptor>());
		return next;
//		return new PartialInstrumentationInferencerMV(Opcodes.ASM5, mdesc, next, map, this.superClasses);
	}
}
