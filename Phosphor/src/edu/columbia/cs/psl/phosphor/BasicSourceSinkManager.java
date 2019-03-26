package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

public class BasicSourceSinkManager extends SourceSinkManager {

	public static ConcurrentHashMap<String, Object> sourceLabels = new ConcurrentHashMap<>();

	// Maps class names to a set of all the methods listed as sources for the class
	private static ConcurrentHashMap<String, SimpleHashSet<String>> sources = new ConcurrentHashMap<>();
	// Maps class names to a set of all the methods listed as sinks for the class
	private static ConcurrentHashMap<String, SimpleHashSet<String>> sinks = new ConcurrentHashMap<>();
	// Maps class names to a set of all the  methods listed as taintThrough methods for the class
	private static ConcurrentHashMap<String, SimpleHashSet<String>> taintThrough = new ConcurrentHashMap<>();

	// Maps class names to a set of all methods listed as sources for the class or one of its supertypes or superinterfaces
	private static ConcurrentHashMap<String, SimpleHashSet<String>> inheritedSources = new ConcurrentHashMap<>();
	// Maps class names to a set of all methods listed as sinks for the class or one of its supertypes or superinterfaces
	private static ConcurrentHashMap<String, SimpleHashSet<String>> inheritedSinks = new ConcurrentHashMap<>();
	// Maps class names to a set of all methods listed as taintThrough methods for the class or one of its supertypes or superinterfaces
	private static ConcurrentHashMap<String, SimpleHashSet<String>> inheritedTaintThrough = new ConcurrentHashMap<>();

	/* Reads source, sink and taintThrough methods from their files into their respective maps. */
	static {
		readTaintMethods(Instrumenter.sourcesFile, sources, "source", true);
		readTaintMethods(Instrumenter.sinksFile, sinks, "sink", false);
		readTaintMethods(Instrumenter.taintThroughFile, taintThrough, "taintThrough", false);
	}

	/* Private constructor ensures that only one instance of BasicSourceSinkManager is ever created. */
	private BasicSourceSinkManager() {
	}

	/* Provides access to the single instance of BasicSourceSinkManager */
	public static BasicSourceSinkManager getInstance() {
		return BasicSourceSinkManagerSingleton.INSTANCE;
	}

	/* Inner class used to provide access to single instance of class and ensure that only a single instance of
	 * BasicSourceSinkManager is ever created. */
	private static class BasicSourceSinkManagerSingleton {
		private static final BasicSourceSinkManager INSTANCE = new BasicSourceSinkManager();
	}

	/* Replaces the set of base sink methods with sink methods read from the specified iterable. Calls retransform for any
	 * class that has already checked whether it is a sink. */
	public static synchronized void replaceSinks(Iterable<String> src) {
		StringBuilder builder = new StringBuilder();
		for(String s : src) {
			builder.append(s).append("\n");
		}
		replaceSinks(new ByteArrayInputStream(builder.toString().getBytes()));
	}

	/* Replaces the set of base sink methods with sink methods read from the specified stream. Calls retransform for any
	 * class with a method whose status as a sink or non-sink has changed. */
	public static synchronized void replaceSinks(InputStream src) {
		// Update the set of base sinks
		sinks.clear();
		readTaintMethods(src, sinks, "sink", false);
		// Store the previous map of inherited or derived sink methods
		ConcurrentHashMap<String, SimpleHashSet<String>> prevInheritedSinks = inheritedSinks;
		// Clear the map of inherited or derived sink methods
		inheritedSinks = new ConcurrentHashMap<>();
		// Retransform any class that has a method that changed from being a sink to a non-sink or vice versa
		for(String className : prevInheritedSinks.keySet()) {
			SimpleHashSet<String> sinkMethods = getAutoTaintMethods(className, sinks, inheritedSinks);
			if(!sinkMethods.equals(prevInheritedSinks.get(className))) {
				// Set of sink methods for this class changed
				try {
					PreMain.getInstrumentation().retransformClasses(Class.forName(className.replace("/", ".")));
				} catch(ClassNotFoundException | UnmodifiableClassException e) {
					//
				} catch (Throwable t) {
					// Make sure that any other type of exception is printed
					t.printStackTrace();
					throw t;
				}
			}
		}
	}

	/* Returns the set of methods that are a particular type of auto taint method (i.e. source, sink or taintThrough) for
	 * the class or interface with the specified slash-separated string class name. A method is considered to be an auto
	 * taint method if the method is present in the set of original auto taint methods for either the specified class or a supertype of the
	 * specified class. Previously determined auto taint methods are stored in inheritedMethods. */
	private static synchronized SimpleHashSet<String> getAutoTaintMethods(String className, ConcurrentHashMap<String, SimpleHashSet<String>> originalMethods,
												  ConcurrentHashMap<String, SimpleHashSet<String>> inheritedMethods) {
		if(inheritedMethods.containsKey(className)) {
			// The auto taint methods for this class have already been determined.
			return inheritedMethods.get(className);
		} else {
			// Recursively build the set of auto taint methods for this class
			SimpleHashSet<String> set = new SimpleHashSet<>();
			if(originalMethods.containsKey(className)) {
				// Add any methods from this class that are directly listed as auto taint methods
				set.addAll(originalMethods.get(className));
			}
			ClassNode cn = Instrumenter.getClassNode(className);
			if(cn != null) {
				if (cn.interfaces != null) {
					// Add all auto taint methods from interfaces implemented by this class
					for (Object inter : cn.interfaces) {
						set.addAll(getAutoTaintMethods((String) inter, originalMethods, inheritedMethods));
					}
				}
				if (cn.superName != null && !cn.superName.equals("java/lang/Object")) {
					// Add all auto taint methods from the superclass of this class
					set.addAll(getAutoTaintMethods(cn.superName, originalMethods, inheritedMethods));
				}
			}
			inheritedMethods.put(className, set);
			return set;
		}
	}

	@Override
	public boolean isSourceOrSinkOrTaintThrough(Class<?> clazz) {
		if(clazz.getName() == null) {
			return false;
		}
		String className = clazz.getName().replace(".", "/");
		// This class has a sink, source or taintThrough method
		return !getAutoTaintMethods(className, sinks, inheritedSinks).isEmpty() ||
				!getAutoTaintMethods(className, sources, inheritedSources).isEmpty() ||
				!getAutoTaintMethods(className, taintThrough, inheritedTaintThrough).isEmpty();
	}

	@Override
	public Object getLabel(String str) {
		return sourceLabels.get(str);
	}

	/* Adds method names from the specified input stream into the set of string stored for their class in the specified map.
	 * If isSource, then sourceLabels are also created for each method name added. */
	private static void readTaintMethods(InputStream src, ConcurrentHashMap<String, SimpleHashSet<String>> map, String type, boolean isSource) {
		Scanner s = null;
		String lastLine = null;
		try {
			if(src != null) {
				s = new Scanner(src);
				int i = 0;
				while (s.hasNextLine()) {
					String line = s.nextLine();
					lastLine = line;
					if (!line.startsWith("#") && !line.isEmpty()) {
						String[] parsed = line.split("\\.");
						if(!map.containsKey(parsed[0])) {
							map.put(parsed[0], new SimpleHashSet<String>());
						}
						map.get(parsed[0]).add(parsed[1]);
						if(isSource) {
							if(Configuration.MULTI_TAINTING) {
                                sourceLabels.put(line, line);
                            } else {
								if(i > 32) {
								    i = 0;
                                }
								sourceLabels.put(line, 1 << i);
							}
							i++;
						}
					}
				}
			}
		} catch (Throwable e) {
			System.err.printf("Unable to parse %s file: %s\n", type, src);
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

	/* Returns the string class name of the supertype of the class or interface with specified string class name from which
	 * its method with the specified method name derived its status as an auto taint (i.e. source, sink or taintThrough)
	 * method. */
	private static synchronized String findSuperTypeAutoTaintProvider(String className, String methodName, ConcurrentHashMap<String,
			SimpleHashSet<String>> originalMethods, ConcurrentHashMap<String, SimpleHashSet<String>> inheritedMethods) {
		LinkedList<String> queue = new LinkedList<>();
		queue.add(className);
		while(!queue.isEmpty()) {
			String curClassName = queue.pop();
			// Check that the current class actually has an inherited auto taint method with the target method name
			if(inheritedMethods.containsKey(curClassName) && inheritedMethods.get(curClassName).contains(methodName)) {
				if(originalMethods.containsKey(curClassName) && originalMethods.get(curClassName).contains(methodName)) {
					return curClassName;
				}
				ClassNode cn = Instrumenter.getClassNode(curClassName);
				if(cn != null) {
					if (cn.interfaces != null) {
						// Enqueue interfaces implemented by the current class
						for (Object inter : cn.interfaces) {
							queue.add((String) inter);
						}
					}
					if (cn.superName != null && !cn.superName.equals("java/lang/Object")) {
						// Enqueue the superclass of the current class
						queue.add(cn.superName);
					}
				}
			}
		}
		// The specified method for the specified class is not the particular type of auto taint method corresponding to
		// the specified maps
		return null;
	}

	@Override
	public boolean isTaintThrough(String str) {
		if (str.startsWith("[")) {
			return false;
		} else {
			String[] parsed = str.split("\\.");
			// Check if the set of taintThrough methods for the class name contains the method name
			return getAutoTaintMethods(parsed[0], taintThrough, inheritedTaintThrough).contains(parsed[1]);
		}
	}

	@Override
	public boolean isSource(String str) {
		if (str.startsWith("[")) {
			return false;
		} else {
			String[] parsed = str.split("\\.");
			// Check if the set of source methods for the class name contains the method name
			if(getAutoTaintMethods(parsed[0], sources, inheritedSources).contains(parsed[1])) {
				String originalSource = findSuperTypeAutoTaintProvider(parsed[0], parsed[1], sources, inheritedSources);
				if(!sourceLabels.containsKey(str)) {
					sourceLabels.put(str, sourceLabels.get(String.format("%s.%s", originalSource, parsed[1])));
				}
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isSink(String str) {
		if (str.startsWith("[")) {
			return false;
		} else {
			String[] parsed = str.split("\\.");
			// Check if the set of sink methods for the class name contains the method name
			return getAutoTaintMethods(parsed[0], sinks, inheritedSinks).contains(parsed[1]);
		}
	}

	/* Returns the name of sink method from which the specified method inherited its sink property or null if the specified
	 * method is not a sink. */
	public String getBaseSink(String str) {
		String[] parsed = str.split("\\.");
		String baseSink = findSuperTypeAutoTaintProvider(parsed[0], parsed[1], sinks, inheritedSinks);
		return baseSink == null ? null : String.format("%s.%s", baseSink, parsed[1]);
	}
}
