package edu.columbia.cs.psl.phosphor.struct;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

final class RankPool {
    /**
     * The maximum ratio of stored entries to storage capacity that does not lead to rehash.
     */
    private static final float LOAD_FACTOR = 0.75f;
    private static final RankedElement NULL_ELEMENT = new RankedElement(null, 0x80000000);
    /**
     * Ranks that are available for reuse.
     */
    private final IntSinglyLinkedList availableRanks = new IntSinglyLinkedList();
    private final ReferenceQueue<RankedElement> referenceQueue = new ReferenceQueue<>();
    private int size = 0;
    private Entry[] entries = new Entry[16];
    private int threshold;

    public RankPool() {
        computeThreshold();
    }

    public synchronized void reset() {
        reclaimRanks();
        if (size > 0) {
            for (int i = 0; i < entries.length; i++) {
                for (Entry entry = entries[i]; entry != null; entry = entry.next) {
                    availableRanks.push(entry.rank);
                }
                entries[i] = null;
            }
            size = 0;
        }
    }

    /**
     * Returns a {@link RankedElement} that has an element that is equal to the specified element (with respect to
     * the {@link Object#equals(Object)} method).
     */
    public synchronized RankedElement getRankedElement(Object element) {
        if (element == null) {
            return getNullRankedObject();
        }
        reclaimRanks();
        int index = getIndex(element.hashCode(), entries.length);
        for (Entry entry = entries[index]; entry != null; entry = entry.next) {
            RankedElement match = entry.getIfMatched(element);
            if (match != null) {
                return match;
            }
        }
        RankedElement result = new RankedElement(element, getNextRank());
        Entry entry = new Entry(result, referenceQueue);
        entry.next = entries[index];
        entries[index] = entry;
        if (++size > threshold) {
            rehash();
        }
        return result;
    }

    private void reclaimRanks() {
        for (Entry entry; (entry = (Entry) referenceQueue.poll()) != null; ) {
            removeEntry(entry);
        }
    }

    private int getNextRank() {
        reclaimRanks();
        if (!availableRanks.isEmpty()) {
            // Reuse an existing rank
            return availableRanks.pop();
        } else {
            // There are no available ranks to be reused; create a new one
            return NULL_ELEMENT.getRank() + size + 1;
        }
    }

    private void computeThreshold() {
        threshold = (int) (entries.length * LOAD_FACTOR);
    }

    void removeEntry(Entry target) {
        if (target != null) {
            Entry prev = null;
            int index = getIndex(target.hash, entries.length);
            for (Entry entry = entries[index]; entry != null; entry = entry.next) {
                if (target == entry) {
                    if (prev == null) {
                        entries[index] = entry.next;
                    } else {
                        prev.next = entry.next;
                    }
                    availableRanks.push(entry.rank);
                    size--;
                    return;
                }
                prev = entry;
            }
        }
    }

    private int getIndex(int hash, int capacity) {
        return (hash & 0x7FFFFFFF) % capacity;
    }

    private void rehash() {
        int newCapacity = entries.length << 1;
        Entry[] newEntries = new Entry[newCapacity];
        for (Entry entry : entries) {
            while (entry != null) {
                int index = getIndex(entry.hash, newCapacity);
                Entry next = entry.next;
                entry.next = newEntries[index];
                newEntries[index] = entry;
                entry = next;
            }
        }
        entries = newEntries;
        computeThreshold();
    }

    private static RankedElement getNullRankedObject() {
        return NULL_ELEMENT;
    }

    private static final class Entry extends WeakReference<RankedElement> {
        private final int hash;
        private final int rank;
        Entry next;

        Entry(RankedElement referent, ReferenceQueue<RankedElement> queue) {
            super(referent, queue);
            this.rank = referent.getRank();
            this.hash = referent.getElement().hashCode();
        }

        public RankedElement getIfMatched(Object element) {
            RankedElement referent = get();
            if (referent != null && element.equals(referent.getElement())) {
                return referent;
            }
            return null;
        }
    }
}
