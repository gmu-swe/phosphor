package edu.columbia.cs.psl.phosphor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import edu.columbia.cs.psl.phosphor.struct.CallGraph;
import edu.columbia.cs.psl.phosphor.struct.MethodInformation;
import edu.columbia.cs.psl.phosphor.struct.MiniClassNode;

public class Instrumenter {
	public static ClassLoader loader;

	//	private static Logger logger = Logger.getLogger("Instrumenter");

	public static int pass_number = 0;

	private static File rootOutputDir;
	private static String lastInstrumentedClass;

	public static int MAX_SANDBOXES = 2;

	static int nChanges = 0;
	static boolean analysisInvalidated = false;

	static void propogateUp(String owner, String name, String desc, MethodInformation toPropogate) {
		propogateUp(owner, name, desc, toPropogate, new HashSet<String>());
	}

	static void propogateUp(String owner, String name, String desc, MethodInformation toPropogate, HashSet<String> tried) {
		if (tried.contains(owner))
			return;
		tried.add(owner);
		if (name.equals("<clinit>"))
			return;
		MiniClassNode c = callgraph.getClassNode(owner);
		if (!owner.equals(toPropogate.getOwner())) {
			MethodInformation m = callgraph.getMethodNodeIfExists(owner, name, desc);
			if (m != null) {
				//				System.out.println(owner+"."+name+desc +" from " + toPropogate.getOwner());
				boolean wasPure = m.isPure();
				boolean wasCallsTainted = m.callsTaintSourceMethods();
				if (wasPure && !toPropogate.isPure()) {
					m.setPure(false);
					analysisInvalidated = true;
					nChanges++;
				}
				if (!wasCallsTainted && toPropogate.callsTaintSourceMethods()) {
					m.setDoesNotCallTaintedMethods(false);
					m.setCallsTaintedMethods(true);
					analysisInvalidated = true;
					nChanges++;
					;
				}
			}
		}
		if (c.superName != null && !c.superName.equals(owner))
			propogateUp(c.superName, name, desc, toPropogate, tried);
		if (c.interfaces != null)
			for (String s : c.interfaces)
				propogateUp(s, name, desc, toPropogate, tried);
	}

	static boolean callsTaintSourceMethods(MethodInformation m) {
		if (m.isCalculated() || m.callsTaintSourceMethods()) {
			//			if(m.callsTaintSourceMethods() && ! m.isCalculated())
			//			System.out.println("defaulting on " + m);
			if (m.callsTaintSourceMethods())
				return true;
			if (m.doesNotCallTaintSourceMethods())
				return false;
		}
		if (callgraph.getClassNode(m.getOwner()).interfaces == null || !m.isVisited()) {
			m.setPure(false);
			m.setCallsTaintedMethods(true);
			return true;
		}
		if (m.isTaintCallExplorationInProgress())
			return false;
		if (BasicSourceSinkManager.getInstance(callgraph).isSource(m.getOwner() + "." + m.getName() + m.getDesc())) {
			m.setCallsTaintedMethods(true);
			m.setPure(false);
			m.setCalculated(true);
			return true;
		}
		m.setTaintCallExplorationInProgress(true);
		HashSet<MethodInformation> origCalled = new HashSet<MethodInformation>(m.getMethodsCalled());
		for (MethodInformation mm : origCalled) {
			if (!mm.isVisited() && !mm.getName().equals("<clinit>")) {
				m.getMethodsCalled().remove(mm);
				MethodInformation omm = mm;
				mm = callgraph.getMethodNodeIfExistsInHierarchy(mm.getOwner(), mm.getName(), mm.getDesc());
				if (mm == null) {
					if (TaintUtils.DEBUG_PURE)
						System.err.println("Unable to find info about method " + omm.getOwner() + "." + omm.getName() + omm.getDesc());
					m.setPure(false);
					m.setCallsTaintedMethods(true);
					m.setTaintCallExplorationInProgress(false);
					m.setCalculated(true);
					propogateUp(m.getOwner(), m.getName(), m.getDesc(), m);
					return true;
				} else
					m.getMethodsCalled().add(mm);
			}
			if (callsTaintSourceMethods(mm)) {
				//				boolean wasPure = m.isPure(); 
				//				boolean wasCallsTainted = m.callsTaintSourceMethods();
				m.setPure(false);
				m.setCallsTaintedMethods(true);
				m.setTaintCallExplorationInProgress(false);
				m.setCalculated(true);
				analysisInvalidated = true;
				nChanges++;
				//				if((wasPure&& !m.isPure()) || (!wasCallsTainted && m.callsTaintSourceMethods()))
				propogateUp(m.getOwner(), m.getName(), m.getDesc(), m);
				return true;
			}
			boolean wasPure = m.isPure();
			//			if(!mm.isVisited())
			//			{
			//				m.setPure(false);
			//				m.setCallsTaintedMethods(true);
			//				m.setDoesNotCallTaintedMethods(false);
			//				m.setCalculated(true);
			//			}
			//			else
			if (mm.isVisited()) {
				m.setPure(m.isPure() && mm.isPure());
				//			if (!m.isPure() && wasPure)
				propogateUp(m.getOwner(), m.getName(), m.getDesc(), m);
			}
		}
		m.setDoesNotCallTaintedMethods(true);
		m.setCallsTaintedMethods(false);
		m.setTaintCallExplorationInProgress(false);
		m.setCalculated(true);
		return false;
	}

	public static void preAnalysis() {
		File graphDir = new File("pc-graphs");
		if (!graphDir.exists())
			graphDir.mkdir();
		for (File f : graphDir.listFiles()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
				CallGraph g = (CallGraph) ois.readObject();
				callgraph.addAll(g);
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
	}

	public static boolean isPure(String owner, String name, String desc) {
		MethodInformation min = callgraph.getMethodNodeIfExistsInHierarchy(owner, name, desc);
		if (min == null) {
			//			System.err.println("Can't find method info for " + owner + "." + name + desc);
			return false;
		}
		return min.isPure();
	}

	public static void finishedAnalysis() {
		int iter = 0;
//		do {
//			System.out.println("iterating.." + nChanges);
//			nChanges = 0;
//			analysisInvalidated = false;
//			if (iter > 0) {
//				for (MethodInformation m : callgraph.getMethods()) {
//					m.setCalculated(false);
//				}
//			}
//			for (MethodInformation m : callgraph.getMethods()) {
//				callsTaintSourceMethods(m);
//				iter++;
//				//				if(m.isPure())
//				//				{
//				//					System.out.println("pure: " + m);
//				//				}
//				//				if (m.getOwner().startsWith("java/io/FileOutput") || m.getOwner().startsWith("java/io/OutputStream")) {
//				//					System.out.println((m.isPure() ? "Pure: " : "") + (m.callsTaintSourceMethods() ? " SOURCE " : "" ) + m.toString());
//				//				}
//			}
//		} while (analysisInvalidated);
		//System.exit(-1);
				File graphDir = new File("pc-graphs");
				if (!graphDir.exists())
					graphDir.mkdir();
//						File outFile = new File("pc-graphs/graph-" + System.currentTimeMillis());
//						try {
//							ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outFile));
//							oos.writeObject(callgraph);
//							oos.close();
//						} catch (Exception ex) {
//							ex.printStackTrace();
//						}
		System.out.println("Analysis Completed: Beginning Instrumentation Phase");

	}

	static String curPath;

	static HashSet<String> notInterfaces = new HashSet<String>();
	static HashSet<String> annotations = new HashSet<String>();
	static HashSet<String> notAnnotations = new HashSet<String>();

	public static boolean isAnnotation(String owner) {
//		if (annotations.contains(owner))
//			return true;
//		if (notAnnotations.contains(owner))
//			return false;
//		try {
//			Class c;
//			try {
//				if (loader == null)
//					c = Class.forName(owner.replace("/", "."));
//				else
//					c = loader.loadClass(owner.replace("/", "."));
//				if (c.isAnnotation()) {
//					annotations.add(owner);
////					System.out.println("Annotation: " + c);
//					return true;
//				}
//				notAnnotations.add(owner);
//			} catch (Throwable ex) {
//				//TODO fix this
//			}
//			return false;
//		} catch (Exception ex) {
//			//			System.out.println("Unable to load for annotation-checking purposes: " + owner);
//			notAnnotations.add(owner);
//		}
		return false;
	}

	public static boolean isCollection(String internalName) {
		try {
			Class c;
			if(TaintTrackingClassVisitor.IS_RUNTIME_INST && ! internalName.startsWith("java/"))
				return false;
			if (loader == null)
				c = Class.forName(internalName.replace("/", "."));
			else
				c = loader.loadClass(internalName.replace("/", "."));
			if (Collection.class.isAssignableFrom(c))
				return true;
		} catch (Throwable ex) {
		}
		return false;
	}

	public static boolean isInterface(String internalName) {
		if (interfaces.contains(internalName))
			return true;
		//		if(notInterfaces.contains(internalName))
		//			return false;
		//		try
		//		{
		//			Class c = Class.forName(internalName.replace("/", "."));
		//			if(c.isInterface())
		//			{
		//				interfaces.add(internalName);
		//				return true;
		//			}
		//		}
		//		catch(Throwable t)
		//		{
		//			
		//		}
		//		notInterfaces.add(internalName);
		return false;
	}
    public static boolean IS_KAFFE_INST = Boolean.valueOf(System.getProperty("KAFFE", "false"));
    public static boolean IS_HARMONY_INST = Boolean.valueOf(System.getProperty("HARMONY", "false"));

	public static boolean IS_ANDROID_INST = Boolean.valueOf(System.getProperty("ANDROID", "false"));
	public static boolean isClassWithHashmapTag(String clazz) {
		if(IS_ANDROID_INST)
			return false;
		return clazz.startsWith("java/lang/Boolean") || clazz.startsWith("java/lang/Character") || clazz.startsWith("java/lang/Byte") || clazz.startsWith("java/lang/Short");
	}

	public static boolean isIgnoredClass(String owner) {
		if(IS_ANDROID_INST && ! TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
//			System.out.println("IN ANDROID INST:");
			return owner.startsWith("java/lang/Object")
					|| owner.startsWith("java/lang/Number") || owner.startsWith("java/lang/Comparable") 
					|| owner.startsWith("java/lang/ref/SoftReference") || owner.startsWith("java/lang/ref/Reference")
					|| owner.startsWith("java/lang/ref/FinalizerReference")
					//																|| owner.startsWith("java/awt/image/BufferedImage")
					//																|| owner.equals("java/awt/Image")
				|| (owner.startsWith("edu/columbia/cs/psl/phosphor") && ! owner.equals(Type.getInternalName(Tainter.class)))
					||owner.startsWith("sun/awt/image/codec/");
		}
		else if(IS_KAFFE_INST || IS_HARMONY_INST)
		{
			return owner.startsWith("java/lang/Object") || owner.startsWith("java/lang/Boolean") || owner.startsWith("java/lang/Character")
					|| owner.startsWith("java/lang/Byte")
					|| owner.startsWith("java/lang/Short")
//					|| owner.startsWith("java/lang/System")
//					|| owner.startsWith("org/apache/harmony/drlvm/gc_gen/GCHelper")
//					|| owner.startsWith("edu/columbia/cs/psl/microbench")
//					|| owner.startsWith("java/lang/Number") 
					|| owner.startsWith("java/lang/VMObject")
					|| owner.startsWith("java/lang/VMString")
					|| (IS_KAFFE_INST && owner.startsWith("java/lang/reflect"))
//					|| owner.startsWith("gnu/")
										|| owner.startsWith("java/lang/VMClass")

					|| owner.startsWith("java/lang/Comparable") || owner.startsWith("java/lang/ref/SoftReference") || owner.startsWith("java/lang/ref/Reference")
					//																|| owner.startsWith("java/awt/image/BufferedImage")
					//																|| owner.equals("java/awt/Image")
					|| (owner.startsWith("edu/columbia/cs/psl/phosphor") && ! owner.equals(Type.getInternalName(Tainter.class)))
					||owner.startsWith("sun/awt/image/codec/") || (IS_HARMONY_INST && (owner.equals("java/io/Serializable")));
		}
		else
		return owner.startsWith("java/lang/Object") || owner.startsWith("java/lang/Boolean") || owner.startsWith("java/lang/Character")
				|| owner.startsWith("java/lang/Byte")
				|| owner.startsWith("java/lang/Short")
				|| owner.startsWith("org/jikesrvm") || owner.startsWith("com/ibm/tuningfork") || owner.startsWith("org/mmtk") || owner.startsWith("org/vmmagic")
//				|| owner.startsWith("edu/columbia/cs/psl/microbench")
				|| owner.startsWith("java/lang/Number") || owner.startsWith("java/lang/Comparable") || owner.startsWith("java/lang/ref/SoftReference") || owner.startsWith("java/lang/ref/Reference")
				//																|| owner.startsWith("java/awt/image/BufferedImage")
				//																|| owner.equals("java/awt/Image")
				|| (owner.startsWith("edu/columbia/cs/psl/phosphor") && ! owner.equals(Type.getInternalName(Tainter.class)))
				||owner.startsWith("sun/awt/image/codec/")
								|| (owner.startsWith("sun/reflect/Reflection")) //was on last
				|| owner.equals("java/lang/reflect/Proxy") //was on last
				|| owner.startsWith("sun/reflection/annotation/AnnotationParser") //was on last
				|| owner.startsWith("sun/reflect/MethodAccessor") //was on last
				|| owner.startsWith("org/apache/jasper/runtime/JspSourceDependent")
				|| owner.startsWith("sun/reflect/ConstructorAccessor") //was on last
				|| owner.startsWith("sun/reflect/SerializationConstructorAccessor")

				|| owner.startsWith("sun/reflect/GeneratedMethodAccessor") || owner.startsWith("sun/reflect/GeneratedConstructorAccessor")
				|| owner.startsWith("sun/reflect/GeneratedSerializationConstructor") || owner.startsWith("sun/awt/image/codec/")
				|| owner.startsWith("java/lang/invoke/LambdaForm")
				;
	}

	public static HashSet<String> interfaces = new HashSet<String>();
	public static CallGraph callgraph = new CallGraph();

	public static HashMap<String, ClassNode> classes = new HashMap<String, ClassNode>();
	public static void analyzeClass(InputStream is) {
		ClassReader cr;
		nTotal++;
		try {
			cr = new ClassReader(is);
			if (callgraph.containsClass(cr.getClassName()))
				return;
			cr.accept(new CallGraphBuildingClassVisitor(new ClassVisitor(Opcodes.ASM5) {
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					super.visit(version, access, name, signature, superName, interfaces);
					ClassNode cn = new ClassNode();
					cn.name = name;
					cn.methods = new LinkedList();
					cn.superName = superName;
					cn.interfaces = new ArrayList<String>();
					for(String s : interfaces)
						cn.interfaces.add(s);
					Instrumenter.classes.put(name, cn);
					if ((access & Opcodes.ACC_INTERFACE) != 0)
						Instrumenter.interfaces.add(name);
				}
			}, callgraph), 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static int nTotal = 0;
	static int n = 0;
	public static byte[] instrumentClass(String path, InputStream is, boolean renameInterfaces) {
		try {
			n++;
			if(n % 1000 ==0)
				System.out.println("Processed: " + n + "/"+nTotal);
			curPath = path;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			PreMain.PCLoggingTransformer transformer = new PreMain.PCLoggingTransformer();
			byte[] ret = transformer.transform(Instrumenter.loader, path, null, null, buffer.toByteArray());
			curPath = null;
			return ret;
		} catch (Exception ex) {
			curPath = null;
			ex.printStackTrace();
			return null;
		}
	}

	static Option opt_taintSources = OptionBuilder.withArgName("taintSources").hasArg().withDescription("File with listing of taint sources to auto-taint").create("taintSources");
	static Option opt_taintSinks =   OptionBuilder.withArgName("taintSinks").hasArg().withDescription("File with listing of taint sinks to use to check for auto-taints").create("taintSinks");
	static Option opt_dataTrack = new Option("withoutDataTrack", "Disable taint tracking through data flow (on by default)");
	static Option opt_controlTrack = new Option("controlTrack", "Enable taint tracking through control flow");
	static Option opt_multiTaint = new Option("multiTaint", "Support for 2^32 tags instead of just 32");
	static Option help = new Option( "help", "print this message" );

	public static String sourcesFile;
	public static String sinksFile;
	
	public static void main(String[] args) {
		
		Options options = new Options();
		options.addOption(help);
		options.addOption(opt_multiTaint);
		options.addOption(opt_controlTrack);
		options.addOption(opt_dataTrack);
		options.addOption(opt_taintSinks);
		options.addOption(opt_taintSources);
		
	    CommandLineParser parser = new BasicParser();
	    CommandLine line = null;
	    try {
	        line = parser.parse( options, args );
	    }
	    catch( org.apache.commons.cli.ParseException exp ) {

			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar phosphor.jar [OPTIONS] [input] [output]", options);
	        System.err.println(exp.getMessage() );
	        return;
		}
		if (line.hasOption("help") || line.getArgs().length != 2) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar phosphor.jar [OPTIONS] [input] [output]", options);
			return;
		}
		
		sourcesFile = line.getOptionValue("taintSources");
		sinksFile = line.getOptionValue("taintSinks");
		Configuration.MULTI_TAINTING = line.hasOption("multiTaint");
		Configuration.IMPLICIT_TRACKING = line.hasOption("controlTrack");
		Configuration.DATAFLOW_TRACKING = !line.hasOption("withoutDataTrack");
		if(Configuration.IMPLICIT_TRACKING)
			Configuration.MULTI_TAINTING = true;

		Configuration.init();
		
		
		if(Configuration.DATAFLOW_TRACKING)
			System.out.println("Data flow tracking: enabled");
		else
			System.out.println("Data flow tracking: disabled");
		if(Configuration.IMPLICIT_TRACKING)
		{
			System.out.println("Control flow tracking: enabled");
		}
		else
			System.out.println("Control flow tracking: disabled");
		
		if(Configuration.MULTI_TAINTING)
			System.out.println("Multi taint: enabled");
		else
			System.out.println("Taints will be combined with logical-or.");

		TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
		ANALYZE_ONLY = true;
		System.out.println("Starting analysis");
//		preAnalysis();
		_main(line.getArgs());
		System.out.println("Analysis Completed: Beginning Instrumentation Phase");
//		finishedAnalysis();
		ANALYZE_ONLY = false;
		_main(line.getArgs());
		System.out.println("Done");

	}

	static boolean ANALYZE_ONLY;

	public static void _main(String[] args) {

		String outputFolder = args[1];
		rootOutputDir = new File(outputFolder);
		if (!rootOutputDir.exists())
			rootOutputDir.mkdir();
		String inputFolder = args[0];
		// Setup the class loader
		final ArrayList<URL> urls = new ArrayList<URL>();
		Path input = FileSystems.getDefault().getPath(args[0]);
		try {
			if (Files.isDirectory(input))
				Files.walkFileTree(input, new FileVisitor<Path>() {

//					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

//					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.getFileName().toString().endsWith(".jar"))
							urls.add(file.toUri().toURL());
						return FileVisitResult.CONTINUE;
					}

//					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

//					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
			else if (inputFolder.endsWith(".jar"))
				urls.add(new File(inputFolder).toURI().toURL());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			urls.add(new File(inputFolder).toURI().toURL());
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		System.out.println(urls);
		if (args.length == 3) {
			System.out.println("Using extra classpath file: " + args[2]);
			try {
				File f = new File(args[2]);
				if (f.exists() && f.isFile()) {
					Scanner s = new Scanner(f);
					while (s.hasNextLine()) {
						urls.add(new File(s.nextLine()).getCanonicalFile().toURI().toURL());
					}
				} else if (f.isDirectory())
					urls.add(f.toURI().toURL());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (args.length > 3) {
			for (int i = 2; i < args.length; i++) {
				File f = new File(args[i]);
				if (!f.exists()) {
					System.err.println("Unable to read path " + args[i]);
					System.exit(-1);
				}
				if (f.isDirectory() && !f.getAbsolutePath().endsWith("/"))
					f = new File(f.getAbsolutePath() + "/");
				try {
					if (f.isDirectory()) {

					}
					urls.add(f.getCanonicalFile().toURI().toURL());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		URL[] urlArray = new URL[urls.size()];
		urlArray = urls.toArray(urlArray);
		loader = new URLClassLoader(urlArray, Instrumenter.class.getClassLoader());
		PreMain.bigLoader = loader;

		File f = new File(inputFolder);
		if (!f.exists()) {
			System.err.println("Unable to read path " + inputFolder);
			System.exit(-1);
		}
		if (f.isDirectory())
			processDirectory(f, rootOutputDir, true);
		else if (inputFolder.endsWith(".jar") || inputFolder.endsWith(".war"))
			//				try {
			//					FileOutputStream fos =  new FileOutputStream(rootOutputDir.getPath() + File.separator + f.getName());
			processJar(f, rootOutputDir);
		//				} catch (FileNotFoundException e1) {
		//					// TODO Auto-generated catch block
		//					e1.printStackTrace();
		//				}
		else if (inputFolder.endsWith(".class"))
			try {
				processClass(f.getName(), new FileInputStream(f), rootOutputDir);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else if (inputFolder.endsWith(".zip")) {
			processZip(f, rootOutputDir);
		} else {
			System.err.println("Unknown type for path " + inputFolder);
			System.exit(-1);
		}

		//		generateInterfaceCLinits(rootOutputDir);
		// }

	}

	private static void processClass(String name, InputStream is, File outputDir) {

		try {
			FileOutputStream fos = new FileOutputStream(outputDir.getPath() + File.separator + name);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			lastInstrumentedClass = outputDir.getPath() + File.separator + name;

			if (ANALYZE_ONLY)
				analyzeClass(is);
			else {
				byte[] c = instrumentClass(outputDir.getAbsolutePath(), is, true);
				bos.write(c);
				bos.writeTo(fos);
				fos.close();
			}
			is.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processDirectory(File f, File parentOutputDir, boolean isFirstLevel) {
		if (f.getName().equals(".AppleDouble"))
			return;
		File thisOutputDir;
		if (isFirstLevel) {
			thisOutputDir = parentOutputDir;
		} else {
			thisOutputDir = new File(parentOutputDir.getAbsolutePath() + File.separator + f.getName());
			thisOutputDir.mkdir();
		}
		for (File fi : f.listFiles()) {
			if (fi.isDirectory())
				processDirectory(fi, thisOutputDir, false);
			else if (fi.getName().endsWith(".class"))
				try {
					processClass(fi.getName(), new FileInputStream(fi), thisOutputDir);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else if (fi.getName().endsWith(".jar") || fi.getName().endsWith(".war"))
				//				try {
				//					FileOutputStream fos = new FileOutputStream(thisOutputDir.getPath() + File.separator + f.getName());
				processJar(fi, thisOutputDir);
			//					fos.close();
			//				} catch (IOException e1) {
			// TODO Auto-generated catch block
			//					e1.printStackTrace();
			//				}
			else if (fi.getName().endsWith(".zip"))
				processZip(fi, thisOutputDir);
			else {
				File dest = new File(thisOutputDir.getPath() + File.separator + fi.getName());
				FileChannel source = null;
				FileChannel destination = null;

				try {
					source = new FileInputStream(fi).getChannel();
					destination = new FileOutputStream(dest).getChannel();
					destination.transferFrom(source, 0, source.size());
				} catch (Exception ex) {
					System.err.println("error copying file " + fi);
					ex.printStackTrace();
					//					logger.log(Level.SEVERE, "Unable to copy file " + fi, ex);
					//					System.exit(-1);
				} finally {
					if (source != null) {
						try {
							source.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (destination != null) {
						try {
							destination.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}

	}

	public static void processJar(File f, File outputDir) {
		try {
			//			@SuppressWarnings("resource")
			//			System.out.println("File: " + f.getName());
			JarFile jar = new JarFile(f);
			JarOutputStream jos = null;
			jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				if (e.getName().endsWith(".class")) {
					{
						if (ANALYZE_ONLY)
							analyzeClass(jar.getInputStream(e));
						else
							try {
								JarEntry outEntry = new JarEntry(e.getName());
								jos.putNextEntry(outEntry);
								byte[] clazz = instrumentClass(f.getAbsolutePath(), jar.getInputStream(e), true);
								if (clazz == null) {
									System.out.println("Failed to instrument " + e.getName() + " in " + f.getName());
									InputStream is = jar.getInputStream(e);
									byte[] buffer = new byte[1024];
									while (true) {
										int count = is.read(buffer);
										if (count == -1)
											break;
										jos.write(buffer, 0, count);
									}
								} else {
									jos.write(clazz);
								}
								jos.closeEntry();
							} catch (ZipException ex) {
								ex.printStackTrace();
								continue;
							}

					}

				} else {
					JarEntry outEntry = new JarEntry(e.getName());
					if (e.isDirectory()) {
						try{
						jos.putNextEntry(outEntry);
						jos.closeEntry();
						}
						catch(ZipException exxx)
						{
							System.out.println("Ignoring exception: " + exxx);
						}
					} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
						// don't copy this
					} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
						Scanner s = new Scanner(jar.getInputStream(e));
						jos.putNextEntry(outEntry);

						String curPair = "";
						while (s.hasNextLine()) {
							String line = s.nextLine();
							if (line.equals("")) {
								curPair += "\n";
								if (!curPair.contains("SHA1-Digest:"))
									jos.write(curPair.getBytes());
								curPair = "";
							} else {
								curPair += line + "\n";
							}
						}
						s.close();
						//						jos.write("\n".getBytes());
						jos.closeEntry();
					} else {
						try {
							jos.putNextEntry(outEntry);
							InputStream is = jar.getInputStream(e);
							byte[] buffer = new byte[1024];
							while (true) {
								int count = is.read(buffer);
								if (count == -1)
									break;
								jos.write(buffer, 0, count);
							}
							jos.closeEntry();
						} catch (ZipException ex) {
							ex.printStackTrace();
							System.out.println("Ignoring above warning from improper source zip...");
						}
					}

				}

			}
			if (jos != null) {
				jos.close();

			}
			jar.close();
		} catch (Exception e) {
			System.err.println("Unable to process jar: " + f.getAbsolutePath());
			e.printStackTrace();
			//			logger.log(Level.SEVERE, "Unable to process jar: " + f.getAbsolutePath(), e);
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				System.err.println("Unable to copy file: " + f.getAbsolutePath());
				ex.printStackTrace();
				//				System.exit(-1);
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
			//			System.exit(-1);
		}

	}

	private static void processZip(File f, File outputDir) {
		try {
			//			@SuppressWarnings("resource")
			ZipFile zip = new ZipFile(f);
			ZipOutputStream zos = null;
			zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				if (e.getName().endsWith(".class")) {
					{
						if (ANALYZE_ONLY)
							analyzeClass(zip.getInputStream(e));
						else {
							ZipEntry outEntry = new ZipEntry(e.getName());
							zos.putNextEntry(outEntry);

							byte[] clazz = instrumentClass(f.getAbsolutePath(), zip.getInputStream(e), true);
							if (clazz == null) {
								InputStream is = zip.getInputStream(e);
								byte[] buffer = new byte[1024];
								while (true) {
									int count = is.read(buffer);
									if (count == -1)
										break;
									zos.write(buffer, 0, count);
								}
							} else
								zos.write(clazz);
							zos.closeEntry();
						}
					}

				} else if (e.getName().endsWith(".jar")) {
					ZipEntry outEntry = new ZipEntry(e.getName());
					//						jos.putNextEntry(outEntry);
					//						try {
					//							processJar(jar.getInputStream(e), jos);
					//							jos.closeEntry();
					//						} catch (FileNotFoundException e1) {
					//							// TODO Auto-generated catch block
					//							e1.printStackTrace();
					//						}

					File tmp = new File("/tmp/classfile");
					if (tmp.exists())
						tmp.delete();
					FileOutputStream fos = new FileOutputStream(tmp);
					byte buf[] = new byte[1024];
					int len;
					InputStream is = zip.getInputStream(e);
					while ((len = is.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					is.close();
					fos.close();
					//						System.out.println("Done reading");
					File tmp2 = new File("tmp2");
					if(!tmp2.exists())
						tmp2.mkdir();
					processJar(tmp, new File("tmp2"));

					zos.putNextEntry(outEntry);
					is = new FileInputStream("tmp2/classfile");
					byte[] buffer = new byte[1024];
					while (true) {
						int count = is.read(buffer);
						if (count == -1)
							break;
						zos.write(buffer, 0, count);
					}
					is.close();
					zos.closeEntry();
					//						jos.closeEntry();
				} else {
					ZipEntry outEntry = new ZipEntry(e.getName());
					if (e.isDirectory()) {
						try{
						zos.putNextEntry(outEntry);
						zos.closeEntry();
						}
						catch(ZipException exxxx)
						{
							System.out.println("Ignoring exception: " + exxxx.getMessage());
						}
					} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
						// don't copy this
					} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
						Scanner s = new Scanner(zip.getInputStream(e));
						zos.putNextEntry(outEntry);

						String curPair = "";
						while (s.hasNextLine()) {
							String line = s.nextLine();
							if (line.equals("")) {
								curPair += "\n";
								if (!curPair.contains("SHA1-Digest:"))
									zos.write(curPair.getBytes());
								curPair = "";
							} else {
								curPair += line + "\n";
							}
						}
						s.close();
						zos.write("\n".getBytes());
						zos.closeEntry();
					} else {
						zos.putNextEntry(outEntry);
						InputStream is = zip.getInputStream(e);
						byte[] buffer = new byte[1024];
						while (true) {
							int count = is.read(buffer);
							if (count == -1)
								break;
							zos.write(buffer, 0, count);
						}
						zos.closeEntry();
					}
				}

			}
			zos.close();
			zip.close();
		} catch (Exception e) {
			System.err.println("Unable to process zip: " + f.getAbsolutePath());
			e.printStackTrace();
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				System.err.println("Unable to copy zip: " + f.getAbsolutePath());
				ex.printStackTrace();
				//				System.exit(-1);
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
		}

	}

	public static boolean shouldCallUninstAlways(String owner, String name, String desc) {
		if (name.equals("writeArray") && owner.equals("java/io/ObjectOutputStream"))
			return true;
		return false;
	}

	public static boolean isIgnoredMethod(String owner, String name, String desc) {
		if (name.equals("wait") && desc.equals("(J)V"))
			return true;
		if (name.equals("wait") && desc.equals("(JI)V"))
			return true;
		return false;
	}

}