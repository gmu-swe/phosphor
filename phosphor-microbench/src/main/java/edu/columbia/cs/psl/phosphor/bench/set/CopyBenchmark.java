package edu.columbia.cs.psl.phosphor.bench.set;

import edu.columbia.cs.psl.phosphor.struct.BitSet;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import org.openjdk.jmh.annotations.*;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class CopyBenchmark {

    // The percentage of the number of unique elements that are present in each set
    @Param({".1", ".2", ".3"})
    private static double percentPresent;
    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();
    // The number of different possible unique elements
    @Param({"10000"})
    private int uniqueElementsSize;
    // Sets being tested
    private BitSet bitSet;
    private PowerSetTree.SetNode setNode;
    private HashSet<Object> hashSet;

    @Setup(Level.Trial)
    public void initSets() {
        bitSet = new BitSet(uniqueElementsSize);
        setNode = setTree.emptySet();
        hashSet = new HashSet<>();
        int setSize = (int) (uniqueElementsSize * percentPresent);
        for(int i : ThreadLocalRandom.current().ints(0, uniqueElementsSize).distinct().limit(setSize).toArray()) {
            bitSet.add(i);
            setNode = setNode.add(i);
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
}