package edu.columbia.cs.psl.phosphor.runtime;

public class RuntimeBoxUnboxPropogator {
	public static String toString$$PHOSPHORTAGGED(int t, byte i) {
		if (t == 0)
			return Byte.toString(i);
		String ret = new String(Byte.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, char i) {
		if (t == 0)
			return Character.toString(i);
		String ret = new String(Character.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toUnsignedString(i);
		String ret = new String(Integer.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toOctalString(i);
		String ret = new String(Integer.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toHexString(i);
		String ret = new String(Integer.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, short i) {
		if (t == 0)
			return Short.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, boolean i) {
		if (t == 0)
			return Boolean.toString(i);
		String ret = new String(Boolean.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, float i) {
		if (t == 0)
			return Float.toString(i);
		String ret = new String(Float.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, float i) {
		if (t == 0)
			return Float.toHexString(i);
		String ret = new String(Float.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, double i) {
		if (t == 0)
			return Double.toString(i);
		String ret = new String(Double.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, double i) {
		if (t == 0)
			return Double.toHexString(i);
		String ret = new String(Double.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toString(i);
		String ret = new String(Long.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toBinaryString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toBinaryString(i);
		String ret = new String(Long.toBinaryString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toHexString(i);
		String ret = new String(Long.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toUnsignedString(i);
		String ret = new String(Long.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toOctalString(i);
		String ret = new String(Long.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(Taint t, byte i) {
		if (t == null)
			return Byte.toString(i);
		String ret = new String(Byte.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, char i) {
		if (t == null)
			return Character.toString(i);
		String ret = new String(Character.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toUnsignedString(i);
		String ret = new String(Integer.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toOctalString(i);
		String ret = new String(Integer.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toHexString(i);
		String ret = new String(Integer.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, short i) {
		if (t == null)
			return Short.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, boolean i) {
		if (t == null)
			return Boolean.toString(i);
		String ret = new String(Boolean.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, float i) {
		if (t == null)
			return Float.toString(i);
		String ret = new String(Float.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, float i) {
		if (t == null)
			return Float.toHexString(i);
		String ret = new String(Float.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, double i) {
		if (t == null)
			return Double.toString(i);
		String ret = new String(Double.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, double i) {
		if (t == null)
			return Double.toHexString(i);
		String ret = new String(Double.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toString(i);
		String ret = new String(Long.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toBinaryString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toBinaryString(i);
		String ret = new String(Long.toBinaryString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toHexString(i);
		String ret = new String(Long.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toUnsignedString(i);
		String ret = new String(Long.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toOctalString(i);
		String ret = new String(Long.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

}
