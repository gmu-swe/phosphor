package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakIdentityHashMap<K, V> implements Serializable {

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final Map<IdentityWeakReference<K>, V> map;

    public WeakIdentityHashMap() {
        this.map = new HashMap<>();
    }

    public int size() {
        expungeStaleEntries();
        return map.size();
    }

    public boolean isEmpty() {
        expungeStaleEntries();
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        expungeStaleEntries();
        return map.containsKey(new IdentityWeakReference<>(key, queue));
    }

    public boolean containsValue(Object value) {
        expungeStaleEntries();
        return map.containsValue(value);
    }

    public V get(Object key) {
        expungeStaleEntries();
        return map.get(new IdentityWeakReference<>(key, queue));
    }

    public V put(K key, V value) {
        if(key == null) {
            throw new IllegalArgumentException("WeakIdentityHashMap cannot have null keys");
        }
        expungeStaleEntries();
        return map.put(new IdentityWeakReference<>(key, queue), value);
    }

    public V remove(Object key) {
        expungeStaleEntries();
        return map.remove(new IdentityWeakReference<>(key, queue));
    }

    public void clear() {
        expungeStaleEntries();
        map.clear();
    }

    public Set<K> keySet() {
        expungeStaleEntries();
        Set<K> ret = new HashSet<>();
        for(IdentityWeakReference<K> ref : map.keySet()) {
            K value = ref.get();
            if(value != null) {
                ret.add(value);
            }
        }
        return Collections.unmodifiableSet(ret);
    }

    public Collection<V> values() {
        expungeStaleEntries();
        return map.values();
    }

    private void expungeStaleEntries() {
        for(Reference ref; (ref = queue.poll()) != null;) {
            map.remove(ref);
        }
    }

    private static class IdentityWeakReference<T> extends WeakReference<T> {

        private final int hashCode;

        IdentityWeakReference(T referent, ReferenceQueue<? super T> queue) {
            super(referent, queue);
            hashCode = System.identityHashCode(referent);
        }

        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }
            WeakReference that = (WeakReference) o;
            T referent = this.get();
            return referent != null && referent == that.get();
        }
    }
}
