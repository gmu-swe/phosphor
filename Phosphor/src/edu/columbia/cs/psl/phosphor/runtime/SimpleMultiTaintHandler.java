package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;

public class SimpleMultiTaintHandler {
	static ArrayList<Tag> tags = new ArrayList<SimpleMultiTaintHandler.Tag>();
	static{
		tags.add(new Tag());
	}
	public static int combineTags(int t1, int t2){
		if(t1 == 0 && t2 == 0)
			return 0;
		else if(t1 == 0)
		{
			return t2;
		}
		else if(t2 == 0)
		{
			return t1;
		}
		else
		{
			Tag newT = new Tag();
			synchronized (tags) {
				newT.dependentOn.add(tags.get(t1));
				newT.dependentOn.add(tags.get(t2));
				tags.add(newT);
				return tags.size()-1;
			}
		}
	}
	static class Tag{
		LinkedList<Tag> dependentOn = new LinkedList<SimpleMultiTaintHandler.Tag>();
		String label;
	}
}
