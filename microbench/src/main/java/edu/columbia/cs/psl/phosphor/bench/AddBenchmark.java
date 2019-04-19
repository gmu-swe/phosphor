package edu.columbia.cs.psl.phosphor.bench;

import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, batchSize = 5000)
@Measurement(iterations = 5, batchSize = 5000)
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Benchmark)
@Threads(1)
public class AddBenchmark {

    // The number of different possible unique elements
    @Param({"1000"})
    private int uniqueElementsSize;

    // Items to be added to the set
    private LinkedList<Integer> itemStack;
    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();

    // The number of sets of each type
    private static final int NUM_SETS = 30;
    // The batch size i.e. the number of items added to each set of the type being tested per iteration
    private static final int BATCH_SIZE = 5000;
    // Sets being tested
    private BitSet[] bitSets = new BitSet[NUM_SETS];
    private PowerSetTree.SetNode[] setNodes = new PowerSetTree.SetNode[NUM_SETS];
    @SuppressWarnings("unchecked")
    private HashSet<Object>[] hashSets = new HashSet[NUM_SETS];
    @SuppressWarnings("unchecked")
    private SimpleHashSet<Object>[] simpleSets = new SimpleHashSet[NUM_SETS];

    @Setup(Level.Iteration)
    public void initSets() {
        for(int i = 0; i < NUM_SETS; i++) {
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            hashSets[i] = new HashSet<>();
            simpleSets[i] = new SimpleHashSet<>();
        }
        itemStack = ThreadLocalRandom.current().ints(BATCH_SIZE*NUM_SETS, 0, uniqueElementsSize).boxed().collect(Collectors.toCollection(LinkedList::new));
    }

    @TearDown(Level.Iteration)
    public void clearSetsForGC() {
        for(int i = 0; i < NUM_SETS; i++) {
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            hashSets[i] = new HashSet<>();
            simpleSets[i] = new SimpleHashSet<>();
        }
    }

    @Benchmark
    public BitSet[] bitSetAddTest(Blackhole hole) {
        for(int i = 0; i < NUM_SETS; i++) {
            bitSets[i].add(itemStack.pop());
            hole.consume(bitSets[i]);
        }
        return bitSets;
    }

    @Benchmark
    public PowerSetTree.SetNode[] setNodeAddTest(Blackhole hole) {
        for(int i = 0; i < NUM_SETS; i++) {
            setNodes[i] = setNodes[i].singletonUnion(itemStack.pop());
            hole.consume(setNodes[i]);
        }
        return setNodes;
    }

    @Benchmark
    public HashSet<Object>[] HashSetAddTest(Blackhole hole) {
        for(int i = 0; i < NUM_SETS; i++) {
            hashSets[i].add(itemStack.pop());
            hole.consume(hashSets[i]);
        }
       return hashSets;
    }

    @Benchmark
    public SimpleHashSet<Object>[] simpleHashSetAddTest(Blackhole hole) {
        for(int i = 0; i < NUM_SETS; i++) {
            simpleSets[i].add(itemStack.pop());
            hole.consume(simpleSets[i]);
        }
        return simpleSets;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                /*.include(".*Benchmark")*/
                .include("AddBenchmark")
                /*.addProfiler("gc")*/
                .addProfiler("stack", "lines=5")
                .verbosity(VerboseMode.NORMAL)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(opt).run();
    }
}