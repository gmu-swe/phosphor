package edu.columbia.cs.psl.phosphor.runtime;

import java.util.WeakHashMap;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;

public class BoxedPrimitiveStoreWithObjTags {
	public static WeakHashMap<Object, Taint> tags = new WeakHashMap<Object, Taint>();

	public static TaintedBooleanWithObjTag booleanValue(Boolean z) {
		TaintedBooleanWithObjTag ret = new TaintedBooleanWithObjTag();
		ret.val = z;
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedByteWithObjTag byteValue(Byte z) {
		TaintedByteWithObjTag ret = new TaintedByteWithObjTag();
		ret.val = z;
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedShortWithObjTag shortValue(Short z) {
		TaintedShortWithObjTag ret = new TaintedShortWithObjTag();
		ret.val = z;
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedCharWithObjTag charValue(Character z) {
		TaintedCharWithObjTag ret = new TaintedCharWithObjTag();
		ret.val = z.charValue();
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static Boolean valueOf(Taint tag, boolean z) {
		if (tag != null) {
			Boolean r = new Boolean(z);
			r.valueOf = true;
			tags.put(r, tag);
			return r;
		}
		return Boolean.valueOf(z);
	}

	public static Byte valueOf(Taint tag, byte z) {
		if (tag != null) {
			Byte r = new Byte(z);
			r.valueOf = true;
			tags.put(r, tag);
			return r;
		}
		return Byte.valueOf(z);
	}

	public static Character valueOf(Taint tag, char z) {
		if (tag != null) {
			Character r = new Character(z);
			r.valueOf = true;
			tags.put(r, tag);
			return r;
		}
		return Character.valueOf(z);
	}

	public static Short valueOf(Taint tag, short z) {
		if (tag != null) {
			Short r = new Short(z);
			r.valueOf = true;
			tags.put(r, tag);
			return r;
		}
		return Short.valueOf(z);
	}
}
