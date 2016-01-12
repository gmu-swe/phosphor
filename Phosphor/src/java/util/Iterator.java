package java.util;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithIntTag;

public interface Iterator<E> {
	public boolean hasNext();
	public E next();
	public void remove();
	
	public TaintedBooleanWithIntTag hasNext$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag ret);
	public E next$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag ret);

	
}
