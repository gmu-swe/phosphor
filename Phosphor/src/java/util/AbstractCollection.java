package java.util;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;

public abstract class AbstractCollection<E> implements Collection<E> {

	protected AbstractCollection() {
	}

	public Iterator iterator() {
		return null;
	}

	public int size() {
		return 0;
	}

	public boolean isEmpty() {
		return false;
	}

	public boolean contains(Object o) {
		return false;
	}

	public Object[] toArray() {
		return null;
	}

	public <T> T[] toArray(T[] a) {
		return null;
	}

	public boolean add(E o) {
		return false;
	}

	public boolean remove(Object o) {
		return false;
	}

	public boolean containsAll(Collection c) {
		return false;
	}

	public boolean addAll(Collection<? extends E> c) {
		return false;
	}

	public boolean removeAll(Collection c) {
		return false;
	}

	public boolean retainAll(Collection c) {
		return false;
	}

	public void clear() {
	}

	public String toString() {
		return null;
	}

	public TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret) {
		return null;
	}

	public TaintedBooleanWithIntTag add$$PHOSPHORTAGGED(Object o, TaintedBooleanWithIntTag ret) {
		return null;
	}

	public TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(Object o, ControlTaintTagStack t, TaintedBooleanWithObjTag ret) {
		return null;
	}

	public TaintedBooleanWithIntTag add$$PHOSPHORTAGGED(Object o, ControlTaintTagStack t, TaintedBooleanWithIntTag ret) {
		return null;
	}
}
