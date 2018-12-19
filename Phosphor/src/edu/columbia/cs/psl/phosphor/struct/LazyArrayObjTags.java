package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;

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
}
