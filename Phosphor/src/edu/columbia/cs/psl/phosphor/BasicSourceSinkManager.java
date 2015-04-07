package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.CallGraph;
import edu.columbia.cs.psl.phosphor.struct.MiniClassNode;

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
			try {
				if(Instrumenter.sourcesFile != null)
				{
					System.out.println("Using taint sources file: " + Instrumenter.sourcesFile);
					s = new Scanner(new File(Instrumenter.sourcesFile));

					int i = 0;
					while (s.hasNextLine())
					{
						String line = s.nextLine();
						if(!line.startsWith("#"))
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
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		{
			Scanner s;
			try {
				if(Instrumenter.sinksFile != null)
				{
					System.out.println("Using taint sinks file: " + Instrumenter.sinksFile);
					s = new Scanner(new File(Instrumenter.sinksFile));

					while (s.hasNextLine()) {
						String line = s.nextLine();
						if (!line.startsWith("#"))
							sinks.add(line);
					}
					s.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
			System.out.println("Loaded " + sinks.size() + " sinks and " + sources.size() + " sources");
		}
	}
	CallGraph g;

	public BasicSourceSinkManager(CallGraph g) {
		this.g = g;
	}

	static BasicSourceSinkManager instance;

	public static BasicSourceSinkManager getInstance(CallGraph g) {
		if (instance == null) {
			instance = new BasicSourceSinkManager(g);
		}
		return instance;
	}

	boolean c1IsSuperforC2(String c1, String c2) {
		if (c1.equals(c2))
			return true;
		MiniClassNode cn = g.getClassNode(c2);
		if (cn.interfaces != null)
			for (String s : cn.interfaces) {
				if (c1IsSuperforC2(c1, s))
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
				if (d[1].equals(inD[1]))//desc is same
				{
					if (c1IsSuperforC2(d[0], inD[0]))
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

				if (d[1].equals(inD[1]))//desc is same
				{
					if (c1IsSuperforC2(d[0], inD[0]))
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
