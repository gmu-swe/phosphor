package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public interface TaintedWithObjTag extends Tainted{
	public Taint getPHOSPHOR_TAG();
	public void setPHOSPHOR_TAG(Taint t);
}
