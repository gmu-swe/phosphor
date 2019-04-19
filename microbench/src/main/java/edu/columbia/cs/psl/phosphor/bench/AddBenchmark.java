package edu.columbia.cs.psl.phosphor.bench;

import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class AddBenchmark {

    @Param({"10", "100", "1000", "10000"})
    private int size;

    /* Primitive items to be added to the set. */
    private int[] items;

    /* Pre-boxed item to be added to the set. */
    private Object[] boxedItems;

    @Setup(Level.Invocation)
    public void setUp() {
        items = ThreadLocalRandom.current().ints(size, 0, size).toArray();
        boxedItems = new Object[items.length];
        for(int i = 0; i < items.length; i++) {
            boxedItems[i] = items[i];
        }
    }

    @Benchmark
    public SimpleHashSet<Object> simpleHashSetAddTest() {
        SimpleHashSet<Object> set = new SimpleHashSet<>();
        for(Object item : boxedItems) {
            set.add(item);
        }
        return set;
    }

    @Benchmark
    public BitSet bitSetAddTest() {
        BitSet set = new BitSet(size);
        for(int item : items) {
            set.add(item);
        }
        return set;
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    public PowerSetTree.SetNode setNodeAddTest() {
        PowerSetTree.SetNode set = PowerSetTree.getInstance().emptySet();
        for(Object item : boxedItems) {
            set = set.singletonUnion(item);
        }
        return set;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                /*.include(".*Benchmark")*/
                .include("UnionBenchmark")
                .forks(3)
                .addProfiler(GCProfiler.class)
                .addProfiler(StackProfiler.class)
                .verbosity(VerboseMode.NORMAL)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(opt).run();
    }
}