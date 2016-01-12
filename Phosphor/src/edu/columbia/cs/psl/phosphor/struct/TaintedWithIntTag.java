package edu.columbia.cs.psl.phosphor.struct;

public interface TaintedWithIntTag extends Tainted {
	public int getPHOSPHOR_TAG();
	public void setPHOSPHOR_TAG(int i);
	public TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(Object o1, TaintedReturnHolderWithIntTag ret);
	public TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag ret);
	public String toString$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag ret);
}
