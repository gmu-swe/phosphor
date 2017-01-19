package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.objectweb.asm.tree.ClassNode;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;

public class BasicSourceSinkManager extends SourceSinkManager {
	static HashSet<String> sinks = new HashSet<String>();
	static HashSet<String> sources = new HashSet<String>();
	static HashMap<String, Object> sourceLabels = new HashMap<String, Object>();
	
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
					System.out.println("Using taint sources file: " + Instrumenter.sourcesFile);
					s = new Scanner(new File(Instrumenter.sourcesFile));

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
				System.err.println("Unable to parse sources file: " + Instrumenter.sourcesFile);
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
					System.out.println("Using taint sinks file: " + Instrumenter.sinksFile);
					s = new Scanner(new File(Instrumenter.sinksFile));

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
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
			System.out.println("Loaded " + sinks.size() + " sinks and " + sources.size() + " sources");
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

	boolean c1IsSuperforC2(String c1, String c2) {
		if (c1.equals(c2))
			return true;
		ClassNode cn = Instrumenter.classes.get(c2);

		if (cn == null) {
			return false;
		}

		if (cn.interfaces != null)
			for (Object s : cn.interfaces) {
				if (c1IsSuperforC2(c1, (String) s))
					return true;
			}

		if (cn.superName == null || cn.superName.equals("java/lang/Object"))
			return false;
		return c1IsSuperforC2(c1, cn.superName);
	}

	@Override
	public boolean isSource(String str) {
		if (str.startsWith("["))
			return false;
		try {
			String[] inD = str.split("\\.");
			for (String s : sources) {
				String d[] = s.split("\\.");
				if (d[1].equals(inD[1]) && c1IsSuperforC2(d[0], inD[0]))//desc is same
				{
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

	public boolean isSink(String str) {
		if (str.startsWith("["))
			return false;
		try {
			String[] inD = str.split("\\.");
			for (String s : sinks) {
				String d[] = s.split("\\.");

				if (d[1].equals(inD[1]) && c1IsSuperforC2(d[0], inD[0]))//desc is same
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
