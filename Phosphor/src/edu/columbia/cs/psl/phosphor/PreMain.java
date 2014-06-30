package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassWriter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.JSRInlinerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.SerialVersionUIDAdder;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.CheckClassAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.util.TraceClassVisitor;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArray;

public class PreMain {
    private static Instrumentation instrumentation;

    static boolean DEBUG = false;

	public static ClassLoader bigLoader = PreMain.class.getClassLoader();
	public static final class PCLoggingTransformer implements ClassFileTransformer {
		private final class HackyClassWriter extends ClassWriter {
			
			private HackyClassWriter(ClassReader classReader, int flags) {
				super(classReader, flags);
			}

			private Class<?> getClass(String name) throws ClassNotFoundException
			{
				try {
					return Class.forName(name.replace("/", "."),false,bigLoader);
				} catch (SecurityException e) {
					throw new ClassNotFoundException("Security exception when loading class");
				} catch(NoClassDefFoundError e)
				{
					throw new ClassNotFoundException();
				}
				catch(Throwable e)
				{
					throw new ClassNotFoundException(); 
				}
			}
			protected String getCommonSuperClass(String type1, String type2) {
				Class<?> c, d;
				try {
					c = getClass(type1);
					d = getClass(type2);
				} catch (ClassNotFoundException e) {
//					System.err.println("Can not do superclass for " + type1 + " and " + type2);
					//					        	logger.debug("Error while finding common super class for " + type1 +"; " + type2,e);
					return "java/lang/Object";
					//					        	throw new RuntimeException(e);
				} catch (ClassCircularityError e) {
					return "java/lang/Object";
				}
				if (c.isAssignableFrom(d)) {
					return type1;
				}
				if (d.isAssignableFrom(c)) {
					return type2;
				}
				if (c.isInterface() || d.isInterface()) {
					return "java/lang/Object";
				} else {
					do {
						c = c.getSuperclass();
					} while (!c.isAssignableFrom(d));
//					System.out.println("Returning " + c.getName());
					return c.getName().replace('.', '/');
				}
			}
		}
		private void upgradeToForceFrames(ClassReader cr, ClassWriter cw) throws Throwable
		{
//			System.out.println("upgrading: " + cr.getClassName());
			ClassWriter cw2 = new HackyClassWriter(cr,ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			cr.accept(new ClassVisitor(Opcodes.ASM5,cw2) {
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//					System.out.println("old was " + version);
					version = 50;
					if((access & Opcodes.ACC_INTERFACE) != 0)
						access |= Opcodes.ACC_ABSTRACT;
					super.visit(version, access, name, signature, superName, interfaces);
				}
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
				}
			}, ClassReader.EXPAND_FRAMES);
			byte[] newClass = cw2.toByteArray();
			cr = new ClassReader(newClass);

			try{
			cr.accept(
//					new CheckClassAdapter(
							new SerialVersionUIDAdder(new TaintTrackingClassVisitor(cw))
//							)
					, ClassReader.EXPAND_FRAMES);
			}
			catch(Throwable ex)
			{
				PrintWriter pw = null;
				try {
					pw = new PrintWriter(new FileWriter("lastClass.txt"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				TraceClassVisitor cv= new TraceClassVisitor(null,pw);
				try{
					ex.printStackTrace();
					System.err.println("running again to trace it");
					cr = new ClassReader(cw2.toByteArray());
				cr.accept(
						new CheckClassAdapter(
								new SerialVersionUIDAdder(new TaintTrackingClassVisitor(cv))
								)
						, ClassReader.EXPAND_FRAMES);
				}
				catch(Throwable ex2)
				{
					innerException = true;
					System.err.println("writing out to " + cr.getClassName());
					
					cv.p.print(pw);
					pw.flush();
					pw.close();
					System.err.println("written");
					File f = new File("debug/" + cr.getClassName().replace("/", ".") + ".class");
					try {
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cr.b);
						fos.close();
					} catch (Exception ex5) {
						ex5.printStackTrace();
					}
					System.err.println("ex2");
					ex2.printStackTrace();
					System.out.println("exiting");
					System.exit(-1);
					System.out.println("ASDHFASKJFALDSFJALSDFJADSLKFJADSF STUPID");
				}
				System.err.println("ex1");
				ex.printStackTrace();
				throw ex;
			}
		}

		static boolean innerException = false;
		
		public TaintedByteArray transform$$INVIVO_PC(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, int[] classtaint, byte[] classfileBuffer, TaintedByteArray ret) throws IllegalClassFormatException
		{
	        bigLoader = loader;
	        Instrumenter.loader = bigLoader;
			if(className.startsWith("sun")) //there are dynamically generated accessors for reflection, we don't want to instrument those.
				ret.val = classfileBuffer;
			else
				ret.val = transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			ret.taint = new int[ret.val.length];
			return ret;
		}
		
		public byte[] transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			ClassReader cr = new ClassReader(classfileBuffer);
			String className = cr.getClassName();
			innerException = false;
			if(Instrumenter.isIgnoredClass(className))
			{
//				System.out.println("Premain.java ignore: " + className);
				return classfileBuffer;
			}
//			if(className.equals("java/lang/Integer"))
//				System.out.println(className);
			final boolean[] shouldBeDoneBetter = new boolean[1];
			shouldBeDoneBetter[0]=false;
			cr.accept(new ClassVisitor(Opcodes.ASM5) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if(desc.equals(Type.getDescriptor(TaintInstrumented.class)))
					{
						shouldBeDoneBetter[0] = true;
					}
					return super.visitAnnotation(desc, visible);
				}
			}, ClassReader.SKIP_CODE);
			if(shouldBeDoneBetter[0])
				return classfileBuffer;
//			System.out.println("Instrumenting: " + className);
//			System.out.println(classBeingRedefined);
			TraceClassVisitor cv =null;
			try {
				
				ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				
				try{
					cr.accept(
//							new CheckClassAdapter(
									new SerialVersionUIDAdder(new TaintTrackingClassVisitor(cw))
//									)
							, ClassReader.EXPAND_FRAMES);
				}
				catch(ClassFormatError ex)
				{
					upgradeToForceFrames(cr, cw);
				}
				catch(ArrayIndexOutOfBoundsException ex)
				{
					
				}
				if (DEBUG) {
					File debugDir = new File("debug");
					if (!debugDir.exists())
						debugDir.mkdir();
					File f = new File("debug/" + className.replace("/", ".") + ".class");
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(cw.toByteArray());
					fos.close();
				}
				{
//					if(TaintUtils.DEBUG_FRAMES)
//						System.out.println("NOW IN CHECKCLASSADAPTOR");
					if (TaintUtils.VERIFY_CLASS_GENERATION && !className.endsWith("org/codehaus/janino/UnitCompiler")) {
						cr = new ClassReader(cw.toByteArray());
						cr.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
					}
				}
//				System.out.println("Succeeded w " + className);
				return cw.toByteArray();
			} catch (Throwable ex) {
				cv= new TraceClassVisitor(null,null);
				try{
					cr.accept(
//							new CheckClassAdapter(
									new SerialVersionUIDAdder(new TaintTrackingClassVisitor(cv))
//									)
							, ClassReader.EXPAND_FRAMES);
				}
				catch(Throwable ex2)
				{				}
				ex.printStackTrace();
				System.err.println("method so far:");
				if (!innerException) {
					PrintWriter pw = null;
					try {
						pw = new PrintWriter(new FileWriter("lastClass.txt"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cv.p.print(pw);
					pw.flush();
				}
				System.out.println("Saving " + className);
					File f = new File("debug/"+className.replace("/", ".")+".class");
					try{
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(classfileBuffer);
					fos.close();
					}
					catch(Exception ex2)
					{
						ex.printStackTrace();
					}
					System.exit(-1);
					return new byte[0];

			}
		}
	}

	public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
        if(Instrumenter.loader == null)
        	Instrumenter.loader = bigLoader;
		ClassFileTransformer transformer = new PCLoggingTransformer();
		inst.addTransformer(transformer);

	}
}
