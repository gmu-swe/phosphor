package edu.columbia.cs.psl.phosphor.runtime;

import java.util.WeakHashMap;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;

public class BoxedPrimitiveStoreWithIntTags {
	public static WeakHashMap<Object, Integer> tags = new WeakHashMap<Object, Integer>();

	public static TaintedBooleanWithIntTag booleanValue(Boolean z) {
		TaintedBooleanWithIntTag ret = new TaintedBooleanWithIntTag();
		ret.val = z;
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedByteWithIntTag byteValue(Byte z) {
		TaintedByteWithIntTag ret = new TaintedByteWithIntTag();
		ret.val = z;
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedShortWithIntTag shortValue(Short z) {
		TaintedShortWithIntTag ret = new TaintedShortWithIntTag();
		ret.val = z;
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedCharWithIntTag charValue(Character z) {
		TaintedCharWithIntTag ret = new TaintedCharWithIntTag();
		ret.val = z.charValue();
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static Boolean valueOf(int tag, boolean z) {
		if (tag > 0) {
			Boolean r = new Boolean(z);
			tags.put(r, tag);
			r.valueOf = true;
			return r;
		}
		return Boolean.valueOf(z);
	}

	public static Byte valueOf(int tag, byte z) {
		if (tag > 0) {
			Byte r = new Byte(z);
			tags.put(r, tag);
			r.valueOf = true;
			return r;
		}
		return Byte.valueOf(z);
	}

	public static Character valueOf(int tag, char z) {
		if (tag > 0) {
			Character r = new Character(z);
			tags.put(r, tag);
			r.valueOf = true;
			return r;
		}
		return Character.valueOf(z);
	}

	public static Short valueOf(int tag, short z) {
		if (tag > 0) {
			Short r = new Short(z);
			tags.put(r, tag);
			r.valueOf = true;
			return r;
		}
		return Short.valueOf(z);
	}
}
