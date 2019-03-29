package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.List;

import edu.columbia.cs.psl.phosphor.instrumenter.ClinitRetransformClassVisitor;
import edu.columbia.cs.psl.phosphor.instrumenter.EclipseCompilerCV;
import edu.columbia.cs.psl.phosphor.instrumenter.HidePhosphorFromASMCV;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurJSRInlinerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurSerialVersionUIDAdder;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.tree.*;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingClassReader;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public class PreMain {
	private static Instrumentation instrumentation;

	public static boolean DEBUG = System.getProperty("phosphor.debug") != null;
	public static boolean RUNTIME_INST = false;
	public static boolean INSTRUMENTATION_EXCEPTION_OCURRED = false;

	public static ClassLoader bigLoader = PreMain.class.getClassLoader();
	/**
	 * As I write this I realize what a multithreaded classloader mess this can create... let's see how bad it is.
	 */
	public static ClassLoader curLoader;

	public static final class PCLoggingTransformer extends PhosphorBaseTransformer {
		public PCLoggingTransformer(){
			TaintUtils.VERIFY_CLASS_GENERATION = System.getProperty("phosphor.verify") != null;
		}
		private static final class HackyClassWriter extends ClassWriter {

			private HackyClassWriter(ClassReader classReader, int flags) {
				super(classReader, flags);
			}

			private Class<?> getClass(String name) throws ClassNotFoundException {
				if(RUNTIME_INST)
					throw new ClassNotFoundException();
				try {
					return Class.forName(name.replace("/", "."), false, bigLoader);
				} catch (SecurityException e) {
					throw new ClassNotFoundException("Security exception when loading class");
				} catch (NoClassDefFoundError e) {
					throw new ClassNotFoundException();
				} catch (Throwable e) {
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

		static boolean innerException = false;

		@Override
		public byte[] transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
				throws IllegalClassFormatException {
			byte[] ret = _transform(loader, className2, classBeingRedefined, protectionDomain, classfileBuffer);

			return ret;
		}

		static MessageDigest md5inst;

		public static byte[] _transform(ClassLoader loader, final String className2, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
				throws IllegalClassFormatException {
			ClassReader cr = (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(classfileBuffer) : new ClassReader(classfileBuffer));
			String className = cr.getClassName();
			innerException = false;
			curLoader = loader;
//			bigLoader = loader;
//			Instrumenter.loader = bigLoader;
			if (Instrumenter.isIgnoredClass(className)) {
				if(className.equals("java/lang/Boolean"))
				{
					return processBoolean(classfileBuffer);
				}
				else if(className.equals("java/lang/Byte"))
				{
					return processBoolean(classfileBuffer);
				}
				else if(className.equals("java/lang/Character"))
				{
					return processBoolean(classfileBuffer);
				}
				else if(className.equals("java/lang/Short"))
				{
					return processBoolean(classfileBuffer);
				}

				return classfileBuffer;
			}			

			Configuration.taintTagFactory.instrumentationStarting(className);
			try {
				ClassNode cn = new ClassNode();
				cr.accept(cn, (Configuration.ALWAYS_CHECK_FOR_FRAMES ? 0 : ClassReader.SKIP_CODE));
				boolean skipFrames = false;
				boolean upgradeVersion = false;
				if (className.equals("org/jruby/parser/Ruby20YyTables")) {
					cn.version = 51;
					upgradeVersion = true;
				}
				if (cn.version >= 100 || cn.version <= 50 || className.endsWith("$Access4JacksonSerializer") || className.endsWith("$Access4JacksonDeSerializer"))
					skipFrames = true;
				else if(Configuration.ALWAYS_CHECK_FOR_FRAMES)
				{
					for(MethodNode mn : cn.methods)
					{
						boolean hasJumps = false;
						boolean foundFrame = false;
						AbstractInsnNode ins = mn.instructions.getFirst();
						if(mn.tryCatchBlocks.size() > 0)
							hasJumps = true;
						while(ins != null && !foundFrame)
						{
							if(ins instanceof JumpInsnNode || ins instanceof TableSwitchInsnNode || ins instanceof LookupSwitchInsnNode)
								hasJumps = true;
							if(ins instanceof FrameNode)
							{
								foundFrame = true;
								break;
							}
							ins = ins.getNext();
						}
						if(foundFrame)
							break;
						if(hasJumps)
						{
							skipFrames = true;
							break;
						}
					}
				}
				if (cn.visibleAnnotations != null)
					for (Object o : cn.visibleAnnotations) {
						AnnotationNode an = (AnnotationNode) o;
						if (an.desc.equals(Type.getDescriptor(TaintInstrumented.class))) {
							return classfileBuffer;
						}
					}
				if (cn.interfaces != null)
					for (Object s : cn.interfaces) {
						if (s.equals(Type.getInternalName(TaintedWithObjTag.class)) || s.equals(Type.getInternalName(TaintedWithIntTag.class))) {
							return classfileBuffer;
						}
					}
				for (Object mn : cn.methods)
					if (((MethodNode) mn).name.equals("getPHOSPHOR_TAG")) {
						return classfileBuffer;
					}
				if (Configuration.CACHE_DIR != null) {
					String cacheKey = className.replace("/", ".");
					File f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".md5sum");
					if (f.exists()) {
						try {
							FileInputStream fis = new FileInputStream(f);
							byte[] cachedDigest = new byte[1024];
							fis.read(cachedDigest);
							fis.close();
							if (md5inst == null)
								md5inst = MessageDigest.getInstance("MD5");
							byte[] checksum = null;
							synchronized (md5inst) {
								checksum = md5inst.digest(classfileBuffer);
							}
							boolean matches = true;
							if (checksum.length > cachedDigest.length)
								matches = false;
							if (matches)
								for (int i = 0; i < checksum.length; i++) {
									if (checksum[i] != cachedDigest[i]) {
										matches = false;
										break;
									}
								}
							if (matches) {
								byte[] ret = Files.readAllBytes(new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".class").toPath());
								return ret;
							}
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
				if (DEBUG) {
					try {
						File debugDir = new File("debug-preinst");
						if (!debugDir.exists())
							debugDir.mkdir();
						File f = new File("debug-preinst/" + className.replace("/", ".") + ".class");
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(classfileBuffer);
						fos.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}

				boolean isiFace = (cn.access & Opcodes.ACC_INTERFACE) != 0;
				List<FieldNode> fields = cn.fields;
				if (skipFrames) {
					cn = null;
					// This class is old enough to not guarantee frames.
					// Generate new frames for analysis reasons, then make sure
					// to not emit ANY frames.
					ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
						@Override
						public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
							return new OurJSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
						}
					}, 0);

					cr = (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(cw.toByteArray()) : new ClassReader(cw.toByteArray()));
				}
				// System.out.println("Instrumenting: " + className);
				// System.out.println(classBeingRedefined);
				// Find out if this class already has frames
				TraceClassVisitor cv = null;
				try {

					ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS);
					ClassVisitor _cv = cw;
					if (Configuration.extensionClassVisitor != null) {
						Constructor<? extends ClassVisitor> extra = Configuration.extensionClassVisitor.getConstructor(ClassVisitor.class, Boolean.TYPE);
						_cv = extra.newInstance(_cv, skipFrames);
					}
					if (DEBUG || TaintUtils.VERIFY_CLASS_GENERATION)
						_cv = new CheckClassAdapter(_cv, false);
                    _cv = new ClinitRetransformClassVisitor(_cv);
					if(isiFace)
						_cv = new TaintTrackingClassVisitor(_cv, skipFrames, fields);
					else
						_cv = new OurSerialVersionUIDAdder(new TaintTrackingClassVisitor(_cv, skipFrames, fields));
					if(EclipseCompilerCV.isEclipseCompilerClass(className)) {
						_cv = new EclipseCompilerCV(_cv);
					}
					_cv = new HidePhosphorFromASMCV(_cv, upgradeVersion);
					if (Configuration.WITH_SELECTIVE_INST)
						cr.accept(new PartialInstrumentationInferencerCV(), ClassReader.EXPAND_FRAMES);
					cr.accept(
					// new CheckClassAdapter(
							_cv
							// )
							, ClassReader.EXPAND_FRAMES);
					byte[] instrumentedBytes = null;
					try{
						instrumentedBytes = cw.toByteArray();
					} catch(MethodTooLargeException ex){
						cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS);
						_cv = cw;
						if (Configuration.extensionClassVisitor != null) {
							Constructor<? extends ClassVisitor> extra = Configuration.extensionClassVisitor.getConstructor(ClassVisitor.class, Boolean.TYPE);
							_cv = extra.newInstance(_cv, skipFrames);
						}
						if (DEBUG || TaintUtils.VERIFY_CLASS_GENERATION)
							_cv = new CheckClassAdapter(_cv, false);
						_cv = new ClinitRetransformClassVisitor(_cv);
						if(isiFace)
							_cv = new TaintTrackingClassVisitor(_cv, skipFrames, fields, true);
						else
							_cv = new OurSerialVersionUIDAdder(new TaintTrackingClassVisitor(_cv, skipFrames, fields, true));
						_cv = new HidePhosphorFromASMCV(_cv, upgradeVersion);
						if (Configuration.WITH_SELECTIVE_INST)
							cr.accept(new PartialInstrumentationInferencerCV(), ClassReader.EXPAND_FRAMES);
						cr.accept(
								// new CheckClassAdapter(
								_cv
								// )
								, ClassReader.EXPAND_FRAMES);
						instrumentedBytes = cw.toByteArray();
					}

					if (DEBUG) {
						File f = new File("debug/" + className + ".class");
						f.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(instrumentedBytes);
						fos.close();
					}
					{
						if (DEBUG || TaintUtils.VERIFY_CLASS_GENERATION
) {
							ClassReader cr2 = new ClassReader(instrumentedBytes);
							cr2.accept(new CheckClassAdapter(new ClassWriter(0), true), ClassReader.EXPAND_FRAMES);
						}
					}

					if (Configuration.CACHE_DIR != null) {
						String cacheKey = className.replace("/", ".");
						File f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".class");
						FileOutputStream fos = new FileOutputStream(f);
						byte[] ret = instrumentedBytes;
						fos.write(ret);
						fos.close();
						if (md5inst == null)
							md5inst = MessageDigest.getInstance("MD5");
						byte[] checksum = null;
						synchronized (md5inst) {
							checksum = md5inst.digest(classfileBuffer);
						}
						f = new File(Configuration.CACHE_DIR + File.separator + cacheKey + ".md5sum");
						fos = new FileOutputStream(f);

						fos.write(checksum);
						fos.close();
						return ret;
					}
					return instrumentedBytes;
				} catch (Throwable ex) {
					INSTRUMENTATION_EXCEPTION_OCURRED = true;
					ex.printStackTrace();
					cv = new TraceClassVisitor(null, null);
					try {
						ClassVisitor _cv = cv;
						if (Configuration.extensionClassVisitor != null) {
							Constructor<? extends ClassVisitor> extra = Configuration.extensionClassVisitor.getConstructor(ClassVisitor.class, Boolean.TYPE);
							_cv = extra.newInstance(_cv, skipFrames);
						}
						_cv = new SerialVersionUIDAdder(new TaintTrackingClassVisitor(_cv, skipFrames, fields));
						_cv = new HidePhosphorFromASMCV(_cv, false);

						cr.accept(_cv, ClassReader.EXPAND_FRAMES);
					} catch (Throwable ex2) {
					}
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
					File f = new File("debug/" + className.replace("/", ".") + ".class");
					f.getParentFile().mkdirs();
					try {
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(classfileBuffer);
						fos.close();
					} catch (Exception ex2) {
						ex.printStackTrace();
					}
					return new byte[0];

				}
			} finally {
				Configuration.taintTagFactory.instrumentationEnding(className);

			}
		}

		private static byte[] processBoolean(byte[] classfileBuffer) {
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassNode cn = new ClassNode(Opcodes.ASM5);
			cr.accept(cn, 0);
			boolean addField = true;
			for(Object  o :cn.fields)
			{
				FieldNode fn = (FieldNode) o;
				if(fn.name.equals("valueOf"))
					addField = false;
			}
			for(Object o : cn.methods){
				MethodNode mn = (MethodNode) o;
				if(mn.name.startsWith("toUpperCase") || mn.name.startsWith("codePointAtImpl") || mn.name.startsWith("codePointBeforeImpl"))
					mn.access = mn.access | Opcodes.ACC_PUBLIC;
			}
			if(addField)
			{
				cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "valueOf", "Z", null, false));
				ClassWriter cw = new ClassWriter(0);
				cn.accept(cw);
				return cw.toByteArray();
			}
			return classfileBuffer;
		}
	}

	public static void premain$$PHOSPHORTAGGED(String args, Instrumentation inst, ControlTaintTagStack ctrl) {
		Configuration.IMPLICIT_TRACKING = true;
		Configuration.MULTI_TAINTING = true;
		Configuration.init();
		premain(args, inst);
	}

	public static void premain(String args, Instrumentation inst) {
		inst.addTransformer(new ClassSupertypeReadingTransformer());
		RUNTIME_INST = true;
		if (args != null) {
			String[] aaa = args.split(",");
			for (String s : aaa) {
				if (s.equals("acmpeq"))
					Configuration.WITH_UNBOX_ACMPEQ = true;
				else if (s.equals("enum"))
					Configuration.WITH_ENUM_BY_VAL = true;
				else if (s.startsWith("cacheDir=")) {
					Configuration.CACHE_DIR = s.substring(9);
					File f = new File(Configuration.CACHE_DIR);
					if (!f.exists()) {
						if(!f.mkdir()) {
							// The cache directory did not exist and the attempt to create it failed
							System.err.printf("Failed to create cache directory: %s. Generated files are not being cached.\n", Configuration.CACHE_DIR);
							Configuration.CACHE_DIR = null;
						}
					}
				}
				else if(s.equals("objmethods"))
					Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE = true;
				else if(s.equals("arraylength"))
					Configuration.ARRAY_LENGTH_TRACKING = true;
				else if(s.equals("lightImplicit"))
					Configuration.IMPLICIT_LIGHT_TRACKING = true;
				else if(s.equals("arrayindex"))
				{
//					Configuration.ARRAY_LENGTH_TRACKING = true;
					Configuration.ARRAY_INDEX_TRACKING = true;
				}
				else if(s.startsWith("withSelectiveInst="))
				{
					Configuration.WITH_SELECTIVE_INST=true;
					Configuration.selective_inst_config = s.substring(18);
					SelectiveInstrumentationManager.populateMethodsToInstrument(Configuration.selective_inst_config);
				} else if (s.startsWith("taintSources=")) {
					try {
						Instrumenter.sourcesFile = new FileInputStream(s.substring(13));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else if (s.startsWith("taintSinks=")) {
					try {
						Instrumenter.sinksFile = new FileInputStream(s.substring(11));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else if (s.startsWith("taintThrough=")) {
					try {
						Instrumenter.taintThroughFile = new FileInputStream(s.substring(13));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else if(s.startsWith("taintSourceWrapper=")) {
					Class c;
					try {
						c = Class.forName(s.substring(19));
						Configuration.autoTainter = (TaintSourceWrapper) c.newInstance();
					} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
						e.printStackTrace();
					}
				} else if(s.startsWith("serialization"))
				{
					Configuration.TAINT_THROUGH_SERIALIZATION = true;
				} else if(s.startsWith("implicitExceptions")){
					Configuration.IMPLICIT_EXCEPTION_FLOW = true;
				} else if (s.startsWith("ignore=")) {
					Configuration.ADDL_IGNORE = s.substring(7);
				} else if (s.equals("withoutBranchNotTaken")) {
					Configuration.WITHOUT_BRANCH_NOT_TAKEN = true;
				}
			}
		}
		if (Instrumenter.loader == null)
			Instrumenter.loader = bigLoader;
		// Ensure that BasicSourceSinkManager & anything needed to call isSourceOrSinkOrTaintThrough gets initialized
		BasicSourceSinkManager.getInstance().isSourceOrSinkOrTaintThrough(Object.class);
		inst.addTransformer(new PCLoggingTransformer());
		inst.addTransformer(new SourceSinkTransformer(), true);
		instrumentation = inst;
	}

	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

}
