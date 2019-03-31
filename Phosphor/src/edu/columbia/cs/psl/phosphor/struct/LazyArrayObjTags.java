package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;


public abstract class LazyArrayObjTags implements Cloneable, Serializable {

	private static final long serialVersionUID = -2635717960621951243L;
	public Taint[] taints;

	public LazyArrayObjTags(Taint[] taints) {
		this.taints = taints;
	}

	public LazyArrayObjTags() {
	}

	public Taint lengthTaint;

	public Taint getLengthTaint() {
		return lengthTaint;
	}


	public abstract int getLength();
	public void setTaints(Taint tag) {
		if(taints == null)
			taints = new Taint[getLength()];
		for(int i = 0; i < taints.length; i++)
		{
			taints[i]=tag;
		}
	}

	public abstract Object getVal();

	protected void checkAIOOB(Taint idxTaint, int idx, ControlTaintTagStack ctrl) {
		if (idx >= getLength()) {
			ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException("" + idx);
			MultiTainter.taintedObject(ex, Taint.combineTags(idxTaint, ctrl));
			throw ex;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LazyArrayObjTags that = (LazyArrayObjTags) o;
		return Objects.equals(this.getVal(), that.getVal());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.getVal());
	}
}
