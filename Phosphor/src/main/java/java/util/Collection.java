package java.util;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;

import java.util.stream.Stream;

public interface Collection<E> extends Iterable<E> {
    int size();

    boolean isEmpty();

    boolean contains(Object o);

    Iterator<E> iterator();

    Object[] toArray();

    <T> T[] toArray(T[] a);

    boolean add(E e);

    Stream<E> stream();

    TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(E e, TaintedBooleanWithObjTag ret);

    TaintedBooleanWithObjTag add$$PHOSPHORTAGGED(E e, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret);

    boolean remove(Object o);

    boolean containsAll(Collection<?> c);

    boolean addAll(Collection<? extends E> c);

    boolean removeAll(Collection<?> c);

    boolean retainAll(Collection<?> c);

    void clear();

    boolean equals(Object o);

    int hashCode();
}
