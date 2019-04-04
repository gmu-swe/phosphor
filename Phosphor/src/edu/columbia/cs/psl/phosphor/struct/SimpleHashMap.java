package edu.columbia.cs.psl.phosphor.struct;

/*
 * Adapted from: https://github.com/robertovormittag/algorithms/blob/master/src/main/java/net/robertovormittag/idealab/structures/hash/SimpleHashSet.java
 * from: package net.robertovormittag.idealab.structures.hash;
 */

import edu.columbia.cs.psl.phosphor.Configuration;

import java.io.Serializable;


public class SimpleHashMap<K, V> implements Serializable {


    public static int DEFAULT_HASHMAP_SIZE = 16;

    private static class Entry<K, V> implements Serializable {
        K key;
        V value;
        Entry<K, V> next;

        public Entry<K, V> copy() {
            Entry<K, V> ret = new Entry<>();
            ret.key = key;
            ret.value = value;
            ret.next = (next == null) ? null : next.copy();
            return ret;
        }
    }

    private Entry<K, V>[] buckets;

    private int size;

    public int size() {
        return size;
    }

    public SimpleHashMap() {
        this(DEFAULT_HASHMAP_SIZE);
    }

    @SuppressWarnings("unchecked")
    public SimpleHashMap(int capacity) {
        buckets = new Entry[capacity];
        size = 0;
    }

    public SimpleHashMap<K,V> copy() {
        SimpleHashMap<K,V> ret = new SimpleHashMap<>(buckets.length);
        for(int i = 0; i < buckets.length; i++) {
            if(buckets[i] != null)
                ret.buckets[i] = buckets[i].copy();
        }
        ret.size = size;
        return ret;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private int hashFunction(int hashCode) {
        int index = hashCode;
        if (index < 0) { index = -index; }
        return index % buckets.length;
    }

    public boolean containsKey(K key) {
        if(key == null) {
            return false;
        }
        int index;
        if(Configuration.IMPLICIT_TRACKING && key instanceof String) {
            synchronized (this) {
                index = hashFunction(((String)key).hashCode());
            }
        } else {
            index = hashFunction(key.hashCode());
        }
        for(Entry<K, V> current = buckets[index]; current != null; current = current.next) {
            // Check if the current node contains the key
            if(current.key.equals(key)) {
                return true;
            }
        }
        // The key was not found
        return false;
    }

    public V get(K key) {
        if(key == null) {
            return null;
        }
        int index;
        if(Configuration.IMPLICIT_TRACKING && key instanceof String) {
            synchronized (this) {
                index = hashFunction(((String)key).hashCode());
            }
        } else {
            index = hashFunction(key.hashCode());
        }
        for(Entry<K, V> current = buckets[index]; current != null; current = current.next) {
            // Check if the current node contains the key
            if(current.key.equals(key)) {
                return current.value;
            }
        }
        // The key was not found
        return null;
    }

    public boolean put(K key, V value) {
        int index;
        if(Configuration.IMPLICIT_TRACKING && key instanceof String) {
            synchronized (this) {
                index = hashFunction(((String)key).hashCode());
            }
        } else {
            index = hashFunction(key.hashCode());
        }

        //log.info(element.toString() + " hashCode=" + element.hashCode() + " index=" + index);
        for(Entry<K, V> current = buckets[index]; current != null; current = current.next) {
            // The key is already in the map
            if(current.key.equals(key)) {
                return false;
            }
        }
        // Add new entry
        Entry<K, V> entry = new Entry<>();
        entry.key = key;
        entry.value = value;
        // Current entry is null when the bucket is empty
        // If it is not null it becomes next Entry
        entry.next = buckets[index];
        buckets[index] = entry;
        size++;
        return true;
    }

    public boolean remove(K key) {
        int index;
        if(Configuration.IMPLICIT_TRACKING && key instanceof String) {
            synchronized (this) {
                index = hashFunction(((String)key).hashCode());
            }
        } else {
            index = hashFunction(key.hashCode());
        }

        //log.info(element.toString() + " hashCode=" + element.hashCode() + " index=" + index);
        Entry<K,V> previous = null;
        for(Entry<K, V> current = buckets[index]; current != null; current = current.next) {
            // Found the key, remove it
            if(current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return true;
            }
            previous = current;
        }
        // The key was not found
        return false;
    }
}
