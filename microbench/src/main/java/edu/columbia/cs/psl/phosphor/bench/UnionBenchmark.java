package edu.columbia.cs.psl.phosphor.bench;

import edu.columbia.cs.psl.phosphor.struct.BitSet;
import edu.columbia.cs.psl.phosphor.struct.IntPowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;

import org.openjdk.jmh.annotations.*;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class UnionBenchmark {

    // The number of different possible unique elements
    @Param({"10000"})
    private int uniqueElementsSize;

    // The percentage of the number of unique elements that are present in each set
    @Param({".1", ".2", ".3"})
    private static double percentPresent;

    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();
    // Singleton used to create empty SetNodes
    private final IntPowerSetTree intSetTree = IntPowerSetTree.getInstance();

    // Sets being tested
    private BitSet[] bitSets = new BitSet[2];
    private PowerSetTree.SetNode[] setNodes = new PowerSetTree.SetNode[2];
    private IntPowerSetTree.SetNode[] intSetNodes = new IntPowerSetTree.SetNode[2];
    @SuppressWarnings("unchecked")
    private HashSet<Object>[] hashSets = new HashSet[2];

    @Setup(Level.Invocation)
    public void initSets() {
        int setSize = (int)(uniqueElementsSize*percentPresent);
        for(int i = 0; i < 2; i++) {
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            intSetNodes[i] = intSetTree.emptySet();
            hashSets[i] = new HashSet<>();
        }
        int i = 0;
        for(int el : ThreadLocalRandom.current().ints(0, uniqueElementsSize).distinct().limit(setSize*2).toArray()) {
            bitSets[i%2].add(el);
            setNodes[i%2] = setNodes[i%2].add(el);
            intSetNodes[i%2] = intSetNodes[i%2].add(el);
            hashSets[i%2].add(el);
            i++;
        }
    }

    @TearDown(Level.Invocation)
    public void clearSetsForGC() {
        for(int i = 0; i < 2; i++) {
            bitSets[i] = null;
            setNodes[i] = null;
            intSetNodes[i] = null;
            hashSets[i] = null;
        }
    }

    @Benchmark
    public BitSet bitSetUnionTest() {
        bitSets[0].union(bitSets[1]);
        return bitSets[0];
    }

    @Benchmark
    public IntPowerSetTree.SetNode intSetNodeUnionTest() {
        return intSetNodes[0].union(intSetNodes[1]);
    }

    @Benchmark
    public PowerSetTree.SetNode setNodeUnionTest() {
        return setNodes[0].union(setNodes[1]);
    }

    @Benchmark
    public HashSet<Object> hashSetUnionTest() {
        hashSets[0].addAll(hashSets[1]);
        return hashSets[0];
    }
}