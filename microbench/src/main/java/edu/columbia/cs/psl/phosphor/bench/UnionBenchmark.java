package edu.columbia.cs.psl.phosphor.bench;

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
public class UnionBenchmark {

    // The number of different possible unique elements
    @Param({"1000", "10000"})
    private int uniqueElementsSize;

    // The percentage of the number of unique elements that are present in each set
    @Param({"0.05", "0.1", "0.2"})
    private static double percentPresent;

    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();

    // Sets being tested
    private BitSet[] bitSets = new BitSet[2];
    private PowerSetTree.SetNode[] setNodes = new PowerSetTree.SetNode[2];
    @SuppressWarnings("unchecked")
    private HashSet<Object>[] hashSets = new HashSet[2];
    @SuppressWarnings("unchecked")
    private SimpleHashSet<Object>[] simpleSets = new SimpleHashSet[2];

    @Setup(Level.Invocation)
    public void initSets() {
        int setSize = (int)(uniqueElementsSize*percentPresent);
        for(int i = 0; i < 2; i++) {
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            hashSets[i] = new HashSet<>();
            simpleSets[i] = new SimpleHashSet<>();
        }
        int i = 0;
        for(int el : ThreadLocalRandom.current().ints(0, uniqueElementsSize).limit(setSize*2).distinct().toArray()) {
            bitSets[i%2].add(el);
            setNodes[i%2].add(el);
            simpleSets[i%2].add(el);
            hashSets[i%2].add(el);
            i++;
        }
    }

    @TearDown(Level.Invocation)
    public void clearSetsForGC() {
        for(int i = 0; i < 2; i++) {
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            hashSets[i] = new HashSet<>();
            simpleSets[i] = new SimpleHashSet<>();
        }
    }

    @Benchmark
    public BitSet bitSetUnionTest() {
        bitSets[0].union(bitSets[1]);
        return bitSets[0];
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

    @Benchmark
    public SimpleHashSet<Object> simpleHashSetUnionTest() {
        simpleSets[0].addAll(simpleSets[1]);
        return simpleSets[0];
    }
}