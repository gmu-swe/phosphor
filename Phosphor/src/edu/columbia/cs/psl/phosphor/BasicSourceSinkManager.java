package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

public class BasicSourceSinkManager extends SourceSinkManager {

	public static SimpleHashSet<String> sinks = new SimpleHashSet<>();
	public static SimpleHashSet<String> sources = new SimpleHashSet<>();
	public static HashMap<String, Object> sourceLabels = new HashMap<>();
	public static SimpleHashSet<String> taintThrough = new SimpleHashSet<>();
	public static SimpleHashSet<String> applicableClasses = new SimpleHashSet<>();

	@Override
	public boolean isSourceOrSinkOrTaintThrough(Class<?> clazz) {
		if(applicableClasses == null || applicableClasses.size() == 0)
			return false;
		if (applicableClasses.contains(clazz.getName()))
			return true;
		if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class)
			if (isSourceOrSinkOrTaintThrough(clazz.getSuperclass()))
				return true;
		for (Class c : clazz.getInterfaces())
			if (isSourceOrSinkOrTaintThrough(c))
				return true;
		return false;
	}

	@Override
	public Object getLabel(String str) {
		return sourceLabels.get(str);
	}

	static {
		if(Instrumenter.sourcesFile == null && Instrumenter.sinksFile == null && !TaintTrackingClassVisitor.IS_RUNTIME_INST) {
			System.err.println("No taint sources or sinks specified. To specify, add option -taintSources file and/or -taintSinks file where file is a file listing taint sources/sinks. See files taint-sinks and taint-samples in source for examples. Lines beginning with # are ignored.");
		}
		readTaintMethods(Instrumenter.sourcesFile, sources, "source", true);
		readTaintMethods(Instrumenter.sinksFile, sinks, "sink", false);
		readTaintMethods(Instrumenter.taintThroughFile, taintThrough, "taintThrough", false);
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST) {
			System.out.printf("Loaded %d sinks, %d sources and %d taint through methods.\n", sinks.size(), sources.size(), taintThrough.size());
		}
		SourceSinkTransformer.isBasicSourceSinkManagerInit = true;
	}

	/* Adds method names from the specified source stream into the specified set. If isSource, then sourceLabels are also
	 * created for each method name added. */
	private static void readTaintMethods(InputStream src, SimpleHashSet<String> set, String type, boolean isSource) {
		Scanner s = null;
		String lastLine = null;
		try {
			if(src != null) {
				if(PreMain.DEBUG) {
					System.out.printf("Using %s file \n", type);
				}
				s = new Scanner(src);
				int i = 0;
				while (s.hasNextLine()) {
					String line = s.nextLine();
					lastLine = line;
					if (!line.startsWith("#") && !line.isEmpty()) {
						String[] parsed = line.split("\\.");
						applicableClasses.add(parsed[0].replaceAll("/", "."));
						set.add(line);
						if(isSource) {
							if(Configuration.MULTI_TAINTING)
								sourceLabels.put(line, line);
							else
							{
								if(i > 32)
									i = 0;
								sourceLabels.put(line, 1 << i);
							}
							i++;
						}
					}
				}
				s.close();
			}
		} catch (Throwable e) {
			System.out.printf("Unable to parse %s file: %s\n", type, src);
			if (lastLine != null) {
				System.err.printf("Last line read: '%s'\n", lastLine);
			}
			throw new RuntimeException(e);
		} finally {
			if(s != null) {
				s.close();
			}
		}
	}
	
	public BasicSourceSinkManager() {
	}

	static BasicSourceSinkManager instance;

	public static BasicSourceSinkManager getInstance() {
		if (instance == null) {
			instance = new BasicSourceSinkManager();
		}
		return instance;
	}

	/* Returns whether the class or interface with specified string name c1 is either the same as, or is an ancestor
	 * of the class or interface with specified string name c2. Performs a breadth first search of Instrumenter.classes
	 * to determine is a class hierarchy path exists. */
	public static boolean isSuperType(String c1, String c2) {
		LinkedList<String> queue = new LinkedList<>();
		queue.add(c2);
		while(!queue.isEmpty()) {
			String className = queue.pop();
			if(className.equals(c1)) {
				return true;
			}
			ClassNode cn = Instrumenter.classes.get(className);
			if(cn != null) {
				if (cn.interfaces != null) {
					for (Object s : cn.interfaces) {
						queue.add((String) s);
					}
				}
				if (cn.superName != null && !cn.superName.equals("java/lang/Object")) {
					queue.add(cn.superName);
				}
			}
		}
		return false;
	}

	@Override
	public boolean isTaintThrough(String str) {
		if (str.startsWith("["))
			return false;
		try {
			String[] inD = str.split("\\.");
			for (String s : taintThrough) {
				String d[] = s.split("\\.");

				if (d[1].equals(inD[1]) && isSuperType(d[0], inD[0]))//desc is same
				{
					return true;
				}
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean isSource(String str) {
		if (str.startsWith("["))
			return false;
		try {
			String[] inD = str.split("\\.");
			for (String s : sources) {
				String d[] = s.split("\\.");
				if (d[1].equals(inD[1]) && isSuperType(d[0], inD[0]))//desc is same
				{
					System.out.printf("Source: %s.%s vs %s.%s\n", d[0], d[1], inD[0], inD[1]);
					if(!sourceLabels.containsKey(str))
						sourceLabels.put(str, sourceLabels.get(s));
				    return true;
				}
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean isSink(String str) {
		if (str.startsWith("["))
			return false;
		try {
			String[] inD = str.split("\\.");
			for (String s : sinks) {
				String d[] = s.split("\\.");

				if (d[1].equals(inD[1]) && isSuperType(d[0], inD[0]))//desc is same
				{
					return true;
				}
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
}
