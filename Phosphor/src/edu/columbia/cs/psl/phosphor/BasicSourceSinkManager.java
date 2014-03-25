package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.CallGraph;
import edu.columbia.cs.psl.phosphor.struct.MiniClassNode;

public class BasicSourceSinkManager extends SourceSinkManager {
	static HashSet<String> sinks = new HashSet<String>();
	static HashSet<String> sources = new HashSet<String>();

	static {
		URL f = BasicSourceSinkManager.class.getResource("/taint-sources");
		//		if(f.exists())
		{
			Scanner s;
			try {
				s = new Scanner(f.openStream());
				while (s.hasNextLine())
					sources.add(s.nextLine());
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
				s = new Scanner(f.openStream());
				while (s.hasNextLine())
					sinks.add(s.nextLine());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!TaintTrackingClassVisitor.IS_RUNTIME_INST)
			System.out.println("Loaded " + sinks.size() + " sinks and " + sources.size() + " sources");
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
