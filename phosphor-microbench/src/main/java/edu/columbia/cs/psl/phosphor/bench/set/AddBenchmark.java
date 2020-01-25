package edu.columbia.cs.psl.phosphor.bench.set;

import edu.columbia.cs.psl.phosphor.struct.BitSet;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import org.openjdk.jmh.annotations.*;
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
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class AddBenchmark {

    // The number of sets of each type
    private static final int NUM_SETS = 30;
    // The percentage of the number of unique elements that are present in each set
    @Param({".1", ".2", ".3"})
    private static double percentPresent;
    // Singleton used to create empty SetNodes
    private final PowerSetTree setTree = PowerSetTree.getInstance();
    // The number of different possible unique elements
    @Param({"10000"})
    private int uniqueElementsSize;
    // Items to be added to the set
    private LinkedList<Integer> itemStack;
    // Sets being tested
    private BitSet[] bitSets = new BitSet[NUM_SETS];
    private PowerSetTree.SetNode[] setNodes = new PowerSetTree.SetNode[NUM_SETS];
    @SuppressWarnings("unchecked")
    private HashSet<Object>[] hashSets = new HashSet[NUM_SETS];

    @Setup(Level.Invocation)
    public void initSets() {
        int setSize = (int) (uniqueElementsSize * percentPresent);
        for(int i = 0; i < NUM_SETS; i++) {
            // Clear the sets
            bitSets[i] = new BitSet(uniqueElementsSize);
            setNodes[i] = setTree.emptySet();
            hashSets[i] = new HashSet<>();
            // Add setSize unique elements to each set
            for(int el : ThreadLocalRandom.current().ints(0, uniqueElementsSize).distinct().limit(setSize).toArray()) {
                bitSets[i].add(el);
                setNodes[i] = setNodes[i].add(el);
                hashSets[i].add(el);
            }
        }
        // Create a supply of items to be added to the sets
        itemStack = ThreadLocalRandom.current().ints(0, uniqueElementsSize).limit(setSize).boxed().collect(Collectors.toCollection(LinkedList::new));
    }

    @TearDown(Level.Invocation)
    public void clearSetsForGC() {
        for(int i = 0; i < NUM_SETS; i++) {
            bitSets[i] = null;
            setNodes[i] = null;
            hashSets[i] = null;
        }
    }

    @Benchmark
    @OperationsPerInvocation(30)
    public BitSet[] bitSetAddTest() {
        for(int i = 0; i < NUM_SETS; i++) {
            bitSets[i].add(itemStack.pop());
        }
        return bitSets;
    }

    @Benchmark
    @OperationsPerInvocation(30)
    public PowerSetTree.SetNode[] setNodeAddTest() {
        for(int i = 0; i < NUM_SETS; i++) {
            setNodes[i] = setNodes[i].add(itemStack.pop());
        }
        return setNodes;
    }

    @Benchmark
    @OperationsPerInvocation(30)
    public HashSet<Object>[] hashSetAddTest() {
        for(int i = 0; i < NUM_SETS; i++) {
            hashSets[i].add(itemStack.pop());
        }
        return hashSets;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include("edu\\.columbia\\.cs\\.psl\\.phosphor\\.bench\\.set\\..*Benchmark")
                .verbosity(VerboseMode.NORMAL)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(opt).run();
    }
}