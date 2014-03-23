package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.BasicValue;

public class BasicTaintedValue extends BasicValue {

	boolean isTainted = false;

	public static final BasicTaintedValue UNINITIALIZED_VALUE() {
		return new BasicTaintedValue(null, false);
	}

	public static final BasicTaintedValue INT_VALUE() {
		return new BasicTaintedValue(Type.INT_TYPE, false);
	}

	public static final BasicTaintedValue FLOAT_VALUE() {
		return new BasicTaintedValue(Type.FLOAT_TYPE, false);
	}

	public static final BasicTaintedValue LONG_VALUE() {
		return new BasicTaintedValue(Type.LONG_TYPE, false);
	}

	public static final BasicTaintedValue DOUBLE_VALUE() {
		return new BasicTaintedValue(Type.DOUBLE_TYPE, false);
	}

	public static final BasicTaintedValue REFERENCE_VALUE() {
		return new BasicTaintedValue(Type.getObjectType("java/lang/Object"), false);
	}

	public static final BasicTaintedValue RETURNADDRESS_VALUE() {
		return new BasicTaintedValue(Type.VOID_TYPE, false);
	}

	public static final BasicTaintedValue TAINTED_UNINITIALIZED_VALUE() {
		return new BasicTaintedValue(null, true);
	}

	public static final BasicTaintedValue TAINTED_INT_VALUE() {
		return new BasicTaintedValue(Type.INT_TYPE, true);
	}

	public static final BasicTaintedValue TAINTED_FLOAT_VALUE() {
		return new BasicTaintedValue(Type.FLOAT_TYPE, true);
	}

	public static final BasicTaintedValue TAINTED_LONG_VALUE() {
		return new BasicTaintedValue(Type.LONG_TYPE, true);
	}

	public static final BasicTaintedValue TAINTED_DOUBLE_VALUE() {
		return new BasicTaintedValue(Type.DOUBLE_TYPE, true);
	}

	public static final BasicTaintedValue TAINTED_REFERENCE_VALUE() {
		return new BasicTaintedValue(Type.getObjectType("java/lang/Object"), true);
	}

	public static final BasicTaintedValue TAINTED_RETURNADDRESS_VALUE() {
		return new BasicTaintedValue(Type.VOID_TYPE, true);
	}

	public BasicTaintedValue(Type type, boolean tainted) {
		super(type);
		this.isTainted = tainted;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTainted ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicTaintedValue other = (BasicTaintedValue) obj;
		if (isTainted != other.isTainted)
			return false;
		return true;
	}

	@Override
	public String toString() {
		String taintVal = (isTainted ? "**" : "");
		if (this.getType() == null) {
			return taintVal + ".";
		} else if (this.getType().equals(Type.VOID_TYPE)) {
			return taintVal + "A";
		} else if (this.getType().equals(Type.getObjectType("java/lang/Object"))) {
			return taintVal + "R";
		} else {
			return taintVal + getType().getDescriptor();
		}
	}
}
