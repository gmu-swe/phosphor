package edu.columbia.cs.psl.phosphor.runtime;

import java.util.WeakHashMap;

import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.TaintedByte;
import edu.columbia.cs.psl.phosphor.struct.TaintedChar;
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;

public class BoxedPrimitiveStore {
	public static WeakHashMap<Object, Integer> tags = new WeakHashMap<Object, Integer>();

	public static TaintedBoolean booleanValue(Boolean z) {
		TaintedBoolean ret = new TaintedBoolean();
		ret.val = z.booleanValue();
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedByte byteValue(Byte z) {
		TaintedByte ret = new TaintedByte();
		ret.val = z.byteValue();
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedShort shortValue(Short z) {
		TaintedShort ret = new TaintedShort();
		ret.val = z.shortValue();
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedChar charValue(Character z) {
		TaintedChar ret = new TaintedChar();
		ret.val = z.charValue();
		if (tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static Boolean valueOf(int tag, boolean z) {
		if (tag > 0) {
			Boolean r = new Boolean(z);
			tags.put(r, tag);
			return r;
		}
		return Boolean.valueOf(z);
	}

	public static Byte valueOf(int tag, byte z) {
		if (tag > 0) {
			Byte r = new Byte(z);
			tags.put(r, tag);
			return r;
		}
		return Byte.valueOf(z);
	}

	public static Character valueOf(int tag, char z) {
		if (tag > 0) {
			Character r = new Character(z);
			tags.put(r, tag);
			return r;
		}
		return Character.valueOf(z);
	}

	public static Short valueOf(int tag, short z) {
		if (tag > 0) {
			Short r = new Short(z);
			tags.put(r, tag);
			return r;
		}
		return Short.valueOf(z);
	}
}
