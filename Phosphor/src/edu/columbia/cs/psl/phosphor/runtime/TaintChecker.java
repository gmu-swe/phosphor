package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class TaintChecker {
	public static void checkTaint(int tag)
	{
		if(tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}
	public static void checkTaint(Taint tag)
	{
		if(tag != null)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}
	public static void checkTaint(Object obj) {
		if(obj == null)
			return;
		if (obj instanceof TaintedWithIntTag) {
			if (((TaintedWithIntTag) obj).getPHOSPHOR_TAG() != 0)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithIntTag) obj).getPHOSPHOR_TAG());
		}
		else if (obj instanceof TaintedWithObjTag) {
			if (((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
		}

		else if(obj instanceof int[])
		{
			for(int i : ((int[])obj))
			{
				if(i > 0)
					throw new IllegalAccessError("Argument carries taints - example: " +i);
			}
		}
		else if(obj instanceof MultiDTaintedArrayWithIntTag)
		{
			LazyArrayIntTags tags = ((MultiDTaintedArrayWithIntTag) obj).taint;
			if (tags.taints != null)
				for (int i : tags.taints) {
					if (i > 0)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		}
		else if(obj instanceof MultiDTaintedArrayWithObjTag)
		{
			LazyArrayObjTags tags = ((MultiDTaintedArrayWithObjTag) obj).taint;
			if (tags.taints != null)
				for (Object i : tags.taints) {
					if (i != null)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		}
		else if(obj instanceof Object[])
		{
			for(Object o : ((Object[]) obj))
				checkTaint(o);
		}
		else if(obj instanceof ControlTaintTagStack)
		{
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if(ctrl.taint != null && !ctrl.isEmpty())
			{
				throw new IllegalAccessError("Current control flow carries taints:  " + ctrl.taint);
			}
		}
		else if(obj instanceof Taint)
		{
			throw new IllegalAccessError("Argument carries taints:  " + obj);
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
		if (obj instanceof TaintedWithIntTag) {
			((TaintedWithIntTag) obj).setPHOSPHOR_TAG(tag);
		} else if (obj instanceof TaintedPrimitiveArrayWithIntTag){
			((TaintedPrimitiveArrayWithIntTag)obj).setTaints(tag);
		}else if (obj instanceof MultiDTaintedArrayWithIntTag) {
			LazyArrayIntTags _taints = ((MultiDTaintedArrayWithIntTag) obj).taint;
			if(_taints.taints == null)
				_taints.taints = new int[((MultiDTaintedArrayWithIntTag) obj).getLength()];
			for (int i = 0; i < _taints.taints.length; i++)
				_taints.taints[i] = tag;
		} else if (obj.getClass().isArray()) {
			
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					setTaints(o, tag);
			
		}
		if(obj instanceof Iterable)
		{
			for(Object o : ((Iterable)obj))
				setTaints(o, tag);
		}
	}
	public static void setTaints(String str, Object tag) {
		if(str == null)
			return;
		if(tag == null)
		{
			str.valuePHOSPHOR_TAG = new LazyArrayObjTags();
			return;
		}
		if(str.valuePHOSPHOR_TAG == null)
			str.valuePHOSPHOR_TAG = new LazyArrayObjTags(new Taint[str.length()]);
		if(str.valuePHOSPHOR_TAG.taints == null)
			str.valuePHOSPHOR_TAG.taints = new Taint[str.length()];
		for (int i = 0; i < str.length(); i++) {
			if (tag != null) {
				if(str.valuePHOSPHOR_TAG.taints[i] != null)
				{
					str.valuePHOSPHOR_TAG.taints[i].addDependency(((Taint) tag));
				} else {
					str.valuePHOSPHOR_TAG.taints[i] = ((Taint) tag).copy();
					str.valuePHOSPHOR_TAG.taints[i].lbl = ((Taint) tag).lbl;
				}
			}
			else if(str.valuePHOSPHOR_TAG.taints[i] == null)
				str.valuePHOSPHOR_TAG.taints[i] = null;
		}
	}
	public static void setTaints(Object obj, Taint tag) {
		if(obj == null)
			return;
		if (obj instanceof TaintedWithObjTag) {
			((TaintedWithObjTag) obj).setPHOSPHOR_TAG(tag);
		} else if (obj instanceof TaintedPrimitiveArrayWithObjTag){
			((TaintedPrimitiveArrayWithObjTag)obj).setTaints(tag);
		}else if (obj instanceof MultiDTaintedArrayWithObjTag) {
			LazyArrayObjTags taints = ((MultiDTaintedArrayWithObjTag) obj).taint;
			if(taints.taints == null)
				taints.taints = new Taint[Array.getLength(((MultiDTaintedArrayWithObjTag)obj).getVal())];
			for (int i = 0; i < taints.taints.length; i++)
				taints.taints[i] = tag;
		} else if (obj.getClass().isArray()) {
			
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					setTaints(o, tag);
			
		}
		if(obj instanceof Iterable)
		{
			for(Object o : ((Iterable)obj))
				setTaints(o, tag);
		}
	}

	public static void setTaints(int len, LazyArrayIntTags tags, int tag) {
		if(len == 0)
			return;
		tags.taints = new int[len];
		for (int i = 0; i < len; i++)
			tags.taints[i] = tag;
	}
	public static void setTaints(Taint[] array, Taint tag) {
		if(array == null)
			return;
		for (int i = 0; i < array.length; i++)
			array[i] = tag;
	}
}
