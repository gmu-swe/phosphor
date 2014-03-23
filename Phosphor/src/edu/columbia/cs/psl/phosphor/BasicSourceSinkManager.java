package edu.columbia.cs.psl.phosphor;

import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.struct.CallGraph;
import edu.columbia.cs.psl.phosphor.struct.MiniClassNode;

public class BasicSourceSinkManager extends SourceSinkManager{
	static HashSet<String> sinks = new HashSet<String>();
	static
	{
		sinks.add("java/io/InputStream.read()I");
	}
	CallGraph g;
	public BasicSourceSinkManager(CallGraph g) {
		this.g = g;
	}
	static BasicSourceSinkManager instance;
	public static BasicSourceSinkManager getInstance(CallGraph g)
	{
		if(instance == null)
		{
			instance = new BasicSourceSinkManager(g);
		}
		return instance;
	}
	boolean c1IsSuperforC2(String c1, String c2)
	{
		if(c1.equals(c2))
			return true;
		MiniClassNode cn = g.getClassNode(c2);
		if(cn.interfaces != null)
		for(String s : cn.interfaces)
		{
			if(c1IsSuperforC2(c1, s))
				return true;
		}
		
		if(cn.superName == null || cn.superName.equals("java/lang/Object"))
			return false;
		return c1IsSuperforC2(c1, cn.superName);
	}
	@Override
	public boolean isSource(String str) {
		if(str.startsWith("["))
			return false;
		try{
		String[] inD = str.split("\\.");
		for(String s : sinks)
		{
			String d[] = s.split("\\.");
			if(d[1].equals(inD[1]))//desc is same
			{
				if(c1IsSuperforC2(d[0], inD[0]))
					return true;
			}
		}
		return false;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}

}
