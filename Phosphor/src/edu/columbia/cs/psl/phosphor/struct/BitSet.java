package edu.columbia.cs.psl.phosphor.struct;

public class BitSet {

    // The number of bits that can be packed into a long.
    private final static int BITS_PER_PACKET = 1 << 6;
    // Array of bit-packed longs. Each bit in a packet represents a particular element in the set. If a bit is 1 then
    // the element represented by that bit is present in the set, otherwise the element in is absent.
    // The if the bit at the position representing a particular element is 1 then that
    private long[] packets;

    /* Creates a new set that can have up to the specified maximum number of elements. */
    public BitSet(int maxElements) {
        this.packets = new long[maxElements/BITS_PER_PACKET + (maxElements%BITS_PER_PACKET == 0 ? 0 : 1)];
    }

    /* Creates a new set with the specified packets. */
    public BitSet(long[] packets) {
        this.packets = packets;
    }

    /* Creates a new set that is a copy of the specified set. */
    public BitSet(BitSet set) {
        this.packets = set.packets.clone();
    }

    /* Returns this instances packets. */
    public long[] getPackets() {
        return packets;
    }

    /* Returns a copy of this set. */
    public BitSet copy() {
        return new BitSet(this);
    }

    /* Adds the element represented by the bit at the specified index to the set.*/
    public void add(int bitIndex) {
        // The index of the packet the specified bit index is located in
        int packetIndex = bitIndex / BITS_PER_PACKET;
        // The mask to isolate the specified bit in the packet it is located in
        long mask = 1L << bitIndex;
        packets[packetIndex] |= mask;
    }

    /* Returns whether or not the element represented by the bit at the specified index is in the set. */
    public boolean contains(int bitIndex) {
        // The index of the packet the specified bit index is located in
        int packetIndex = bitIndex / BITS_PER_PACKET;
        // The mask to isolate the specified bit in the packet it is located in
        long mask = 1L << bitIndex;
        return (packets[packetIndex] & mask) != 0;
    }

    /* Adds all of the elements in the specified other set to this set. */
    public void union(BitSet other) {
        if(other != null && other.packets.length > this.packets.length) {
            long[] temp = this.packets;
            this.packets = other.packets.clone();
            for(int i = 0; i < temp.length; i++) {
                this.packets[i] |= temp[i];
            }
        } else if(other != null) {
            for(int i = 0; i < other.packets.length; i++) {
                this.packets[i] |= other.packets[i];
            }
        }
    }

    /* Returns true is none of the bits in the set are set to 1. */
    public boolean isEmpty() {
        for(long packet : packets) {
            if(packet != 0) {
                return false;
            }
        }
        return true;
    }

    /* Returns whether this set is a superset of the specified other set. */
    public boolean isSuperset(BitSet other) {
        if(other == null || this.packets.length < other.packets.length) {
            return true;
        } else  {
            for(int i = 0; i < other.packets.length; i++) {
                if((this.packets[i] | other.packets[i]) != this.packets[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /* Returns a list containing the bit indices in this set that are set to one. */
    public SimpleLinkedList<Integer> toList() {
        SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
        for(int i = 0; i < packets.length; i++) {
            int packetOffset = i * BITS_PER_PACKET;
            int shifts = 0;
            for(long packetValue = packets[i]; packetValue != 0; packetValue = packetValue >>> 1) {
                if((packetValue & 1) != 0) {
                    list.enqueue(packetOffset + shifts);
                }
                shifts++;
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(!(obj instanceof BitSet)) {
            return false;
        } else {
            BitSet bitSet = (BitSet)obj;
            if(bitSet.packets.length != packets.length) {
                return false;
            }
            for(int i = 0; i < packets.length; i++) {
                if(bitSet.packets[i] != packets[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (long packet : packets) {
            int packetHash = (int)(packet ^ (packet >>> 32));
            result = 31 * result + packetHash;
        }
        return result;
    }

    /* Returns a new BitSet that represents the union of the specified sets. */
    public static BitSet union(BitSet set1, BitSet set2) {
        if(set1 == null) {
            return set2;
        } else if(set2 == null) {
            return set1;
        } else if(set1.packets.length > set2.packets.length) {
            BitSet result = new BitSet(set1);
            result.union(set2);
            return result;
        } else {
            BitSet result = new BitSet(set2);
            result.union(set1);
            return result;
        }
    }
}
