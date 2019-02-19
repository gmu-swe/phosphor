package edu.columbia.cs.psl.phosphor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import org.objectweb.asm.tree.ClassNode;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;

public class BasicSourceSinkManager extends SourceSinkManager {
	public static HashSet<String> sinks = new HashSet<String>();
	public static HashSet<String> sources = new HashSet<String>();
	public static HashMap<String, Object> sourceLabels = new HashMap<String, Object>();
	public static HashSet<String> taintThrough = new HashSet<String>();
	
	@Override
	public Object getLabel(String str) {
		return sourceLabels.get(str);
	}
	static {
		if(Instrumenter.sourcesFile == null && Instrumenter.sinksFile == null && !TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
			System.err.println("No taint sources or sinks specified. To specify, add option -taintSources file and/or -taintSinks file where file is a file listing taint sources/sinks. See files taint-sinks and taint-samples in source for examples. Lines beginning with # are ignored.");
		}

		{
			Scanner s;
			String lastLine = null;
			try {
				if(Instrumenter.sourcesFile != null)
				{
					if(PreMain.DEBUG)
						System.out.println("Using taint sources file");
					s = new Scanner(Instrumenter.sourcesFile);

					int i = 0;
					while (s.hasNextLine())
					{
						String line = s.nextLine();
						lastLine = line;
						if(!line.startsWith("#") && !line.isEmpty())
						{
							sources.add(line);
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
					s.close();
				}
				
			} catch (Throwable e) {
				System.err.println("Unable to parse sources file");
				if (lastLine != null)
					System.err.println("Last line read: '" + lastLine + "'");
				throw new RuntimeException(e);
			} 
		}
		{
			Scanner s;
			String lastLine = null;
			try {
				if(Instrumenter.sinksFile != null)
				{
					if(PreMain.DEBUG)
						System.out.println("Using taint sinks file");
					s = new Scanner(Instrumenter.sinksFile);

					while (s.hasNextLine()) {
						String line = s.nextLine();
						lastLine = line;
						if (!line.startsWith("#") && !line.isEmpty())
							sinks.add(line);
					}
					s.close();
				}
			} catch (Throwable e) {
				System.err.println("Unable to parse sink file: " + Instrumenter.sourcesFile);
				if (lastLine != null)
					System.err.println("Last line read: '" + lastLine + "'");
				throw new RuntimeException(e);
			} 
		}
		{
			Scanner s;
			String lastLine = null;
			try {
				if(Instrumenter.taintThroughFile != null)
				{
					if(PreMain.DEBUG)
						System.out.println("Using taint through file");
					s = new Scanner(Instrumenter.taintThroughFile);

					while (s.hasNextLine()) {
						String line = s.nextLine();
						lastLine = line;
						if (!line.startsWith("#") && !line.isEmpty())
							taintThrough.add(line);
					}
					s.close();
				}
			} catch (Throwable e) {
				System.err.println("Unable to parse taintThrough file: " + Instrumenter.taintThroughFile);
				if (lastLine != null)
					System.err.println("Last line read: '" + lastLine + "'");
				throw new RuntimeException(e);
			} 
		}
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
			System.out.println("Loaded " + sinks.size() + " sinks, " + sources.size() + " sources and " + taintThrough.size()+ " taint through methods");
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
