package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.CallGraph;
import edu.columbia.cs.psl.phosphor.struct.MiniClassNode;

public class BasicSourceSinkManager extends SourceSinkManager {
	static HashSet<String> sinks = new HashSet<String>();
	static HashSet<String> sources = new HashSet<String>();

	static {
		if(System.getProperty("TAINT_SOURCES") == null && System. getProperty("TAINT_SINKS") == null)
		{
			System.err.println("No taint sources or sinks specified. To specify, add option -DTAINT_SOURCES=file and/or -DTAINT_SINKS=file where file is a file listing taint sources/sinks. See files taint-sinks and taint-samples in source for examples. Lines beginning with # are ignored.");
		}
		URL f = BasicSourceSinkManager.class.getResource("/taint-sources");
		//		if(f.exists())
		{
			Scanner s;
			try {
				if(System.getProperty("TAINT_SOURCES") != null)
				{
					System.out.println("Using taint sources file: " + System.getProperty("TAINT_SOURCES"));
					s = new Scanner(new File(System.getProperty("TAINT_SOURCES")));
				}
				else
					s = new Scanner(f.openStream());
				while (s.hasNextLine())
				{
					String line = s.nextLine();
					if(!line.startsWith("#"))
						sources.add(line);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		f = BasicSourceSinkManager.class.getResource("/taint-sinks");
		//		if(f.exists())
		{
			Scanner s;
			try {
				if(System.getProperty("TAINT_SINKS") != null)
				{
					System.out.println("Using taint sinks file: " + System.getProperty("TAINT_SINKS"));
					s = new Scanner(new File(System.getProperty("TAINT_SINKS")));
				}
				else
					s = new Scanner(f.openStream());
				while (s.hasNextLine()) {
					String line = s.nextLine();
					if (!line.startsWith("#"))
						sinks.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST)
		{
			if(Configuration.TAINT_TAG_TYPE != Type.INT)
				System.err.println("Warning: You specified to perform auto source/sink tainting, but want to use a non-integer taint. This is unsupported.");
			else
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
