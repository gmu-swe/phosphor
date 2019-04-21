package edu.columbia.cs.psl.phosphor.bench;

import edu.columbia.cs.psl.phosphor.struct.BitSet;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;

import org.openjdk.jmh.annotations.*;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class CopyBenchmark {

    // The number of different possible unique elements
    @Param({"1000", "10000"})
    private int uniqueElementsSize;

    // The percentage of the number of unique elements that are present in each set
    @Param({"0.05", ".1", ".2"})
    private static double percentPresent;

    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();

    // Sets being tested
    private BitSet bitSet;
    private PowerSetTree.SetNode setNode;
    private HashSet<Object> hashSet;
    private SimpleHashSet<Object> simpleSet;

    @Setup(Level.Trial)
    public void initSets() {
        bitSet = new BitSet(uniqueElementsSize);
        setNode = setTree.emptySet();
        hashSet = new HashSet<>();
        simpleSet = new SimpleHashSet<>();
        int setSize = (int)(uniqueElementsSize*percentPresent);
        for(int i : ThreadLocalRandom.current().ints(0, uniqueElementsSize).limit(setSize).distinct().toArray()) {
            bitSet.add(i);
            setNode.add(i);
            simpleSet.add(i);
            hashSet.add(i);
        }
    }

    @Benchmark
    public BitSet bitSetCopyTest() {
        return bitSet.copy();
    }

    @Benchmark
    public PowerSetTree.SetNode setNodeCopyTest() {
        return setNode;
    }

    @Benchmark
    public HashSet<Object> hashSetCopyTest() {
        return new HashSet<>(hashSet);
    }

    @Benchmark
    public SimpleHashSet<Object> simpleHashSetCopyTest() {
        SimpleHashSet<Object> copy = new SimpleHashSet<>();
        copy.addAll(simpleSet);
        return copy;
    }
}