package java.util;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;

public interface Collection<E> extends Iterable<E> {
	int size();

	boolean isEmpty();

	boolean contains(Object o);

	Iterator<E> iterator();

	Object[] toArray();

	<T> T[] toArray(T[] a);

	boolean add(E e);

	TaintedBooleanWithIntTag add$$PHOSPHORTAGGED(E e, TaintedBooleanWithIntTag ret);

	TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(E e, TaintedBooleanWithObjTag ret);

	TaintedBooleanWithIntTag add$$PHOSPHORTAGGED(E e, ControlTaintTagStack ctrl, TaintedBooleanWithIntTag ret);

	TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(E e, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret);

	boolean remove(Object o);

	boolean containsAll(Collection<?> c);

	boolean addAll(Collection<? extends E> c);

	boolean removeAll(Collection<?> c);

	boolean retainAll(Collection<?> c);

	void clear();

	boolean equals(Object o);

	int hashCode();
}
