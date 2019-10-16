package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

public class BoxedPrimitiveStoreWithObjTags {

	public static WeakIdentityHashMap<Object, Taint> tags = new WeakIdentityHashMap<>();

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
		if (z.valueOf && tags.containsKey(z))
			ret.taint = tags.get(z);
		return ret;
	}

	public static TaintedCharWithObjTag charValue(Character z) {
		TaintedCharWithObjTag ret = new TaintedCharWithObjTag();
		ret.val = z.charValue();
		if (z.valueOf && tags.containsKey(z))
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

	@SuppressWarnings("unused")
	public static Boolean getTaintedBoxedPrimitive(Boolean z, ControlTaintTagStack controlTags) {
		if(z == null) {
			return null;
		}
		Taint<?> tag = z.valueOf && tags.containsKey(z) ? tags.get(z) : null;
		tag = Taint.combineTags(tag, controlTags);
		if(tag != null && !tag.isEmpty()) {
			z = new Boolean(z.booleanValue()); // Make a new one before tainting just in case
			z.valueOf = true;
			tags.put(z, tag);
		}
		return z;
	}

	@SuppressWarnings("unused")
	public static Byte getTaintedBoxedPrimitive(Byte b, ControlTaintTagStack controlTags) {
		if(b == null) {
			return null;
		}
		Taint<?> tag = b.valueOf && tags.containsKey(b) ? tags.get(b) : null;
		tag = Taint.combineTags(tag, controlTags);
		if(tag != null && !tag.isEmpty()) {
			b = new Byte(b.byteValue()); // Make a new one before tainting just in case
			b.valueOf = true;
			tags.put(b, tag);
		}
		return b;
	}

	@SuppressWarnings("unused")
	public static Character getTaintedBoxedPrimitive(Character c, ControlTaintTagStack controlTags) {
		if(c == null) {
			return null;
		}
		Taint<?> tag = c.valueOf && tags.containsKey(c) ? tags.get(c) : null;
		tag = Taint.combineTags(tag, controlTags);
		if(tag != null && !tag.isEmpty()) {
			c = new Character(c.charValue()); // Make a new one before tainting just in case
			c.valueOf = true;
			tags.put(c, tag);
		}
		return c;
	}

	@SuppressWarnings("unused")
	public static Short getTaintedBoxedPrimitive(Short s, ControlTaintTagStack controlTags) {
		if(s == null) {
			return null;
		}
		Taint<?> tag = s.valueOf && tags.containsKey(s) ? tags.get(s) : null;
		tag = Taint.combineTags(tag, controlTags);
		if(tag != null && !tag.isEmpty()) {
			s = new Short(s.shortValue()); // Make a new one before tainting just in case
			s.valueOf = true;
			tags.put(s, tag);
		}
		return s;
	}
}
