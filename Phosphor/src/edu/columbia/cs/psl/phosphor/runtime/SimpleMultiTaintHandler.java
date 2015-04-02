package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class SimpleMultiTaintHandler {

	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags)
	{
		if(o instanceof TaintedWithObjTag)
		{
			((TaintedWithObjTag) o).setPHOSPHOR_TAG(combineTags(((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));

		}
	}
	public static Object combineTags(Object t1, ControlTaintTagStack tags){
		if(t1 == null && tags.isEmpty())
			return null;
		else if(t1 == null)
		{
			return tags.taint;
		}
		else if(tags.isEmpty())
			return t1;
		return new Taint((Taint) t1, tags.taint);
	}
	public static Object combineTags(Object ot1, Object ot2)
	{
		if(ot1 == null && ot2 == null)
			return null;
		if(ot2 == null)
			return ot1;
		if(ot1 == null)
			return ot2;
		Taint t1 = (Taint) ot1;
		Taint t2 = (Taint) ot2;
		if(t1.lbl == null && t1.hasNoDependencies())
			return t2;
		if(t2.lbl == null && t2.hasNoDependencies())
			return t1;
		Taint r = new Taint(t1);
		r.addDependency(t2);
		return r;
	}

}
