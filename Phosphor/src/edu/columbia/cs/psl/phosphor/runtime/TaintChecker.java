package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

import edu.columbia.cs.psl.phosphor.struct.Tainted;

public class TaintChecker {
	public static void checkTaint(int tag)
	{
		if(tag != 0)
			throw new IllegalAccessError("Argument carries taint");
	}
	public static void checkTaint(Object obj) {
		if(obj == null)
			return;
		if (obj instanceof Tainted) {
			if (((Tainted) obj).getINVIVO_PC_TAINT() != 0)
				throw new IllegalAccessError("Argument carries taint");
		}
		else if(obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive())
		{
			int[] tags = ArrayHelper.getTags(obj);
			for(int i : tags)
			{
				if(i > 0)
					throw new IllegalAccessError("Argument carries taints");
			}
		}
		else if(obj instanceof Object[])
		{
			for(Object o : ((Object[]) obj))
				checkTaint(o);
		}
	}

	public static boolean hasTaints(int[] tags) {
		if(tags == null)
			return false;
		for (int i : tags) {
			if (i != 0)
				return true;
		}
		return false;
	}

	public static void setTaints(Object obj, int tag) {
		if(obj == null)
			return;
		if (obj instanceof Tainted) {
			((Tainted) obj).setINVIVO_PC_TAINT(tag);
		} else if (obj.getClass().isArray()) {
			if (obj.getClass().getComponentType().isPrimitive()) {
				int[] taints = ArrayHelper.getTags(obj);
				for (int i = 0; i < taints.length; i++)
					taints[i] = tag;
			} else {
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					setTaints(o, tag);
			}
		}
	}

	public static void setTaints(int[] array, int tag) {
		if(array == null)
			return;
		for (int i = 0; i < array.length; i++)
			array[i] = tag;
	}
}
