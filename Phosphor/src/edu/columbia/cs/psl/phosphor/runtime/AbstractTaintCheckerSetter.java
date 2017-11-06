package edu.columbia.cs.psl.phosphor.runtime;

public abstract class AbstractTaintCheckerSetter {
	public abstract void checkTaint(int tag);
	public abstract void checkTaint(Taint tag);

	public abstract void checkTaint(Object obj);
	public abstract void setTaint(Object obj, int i);
	public abstract void setTaint(Object obj, Taint<?> t);
}
