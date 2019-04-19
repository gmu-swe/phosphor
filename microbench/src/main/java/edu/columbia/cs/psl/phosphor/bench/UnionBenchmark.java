package edu.columbia.cs.psl.phosphor.bench;

import edu.columbia.cs.psl.phosphor.struct.SimpleHashSet;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class UnionBenchmark {

    // The number of different possible unique elements
    @Param({"10", "100", "1000", "10000"})
    private static int uniqueElementsSize;

    // The percentage of the number of unique elements that are present in each set
    @Param({"0.05", ".1", ".2", ".3"})
    private static double percentPresent;

    @State(Scope.Thread)
    public static class SimpleSetState {
        SimpleHashSet<Object> set;

        @Setup(Level.Invocation)
        public void doSetup() {
            int setSize = (int)(uniqueElementsSize * percentPresent);
            set = new SimpleHashSet<>(setSize);
            ThreadLocalRandom.current().ints(0, uniqueElementsSize).distinct().limit(setSize).forEach(set::add);
        }
    }

    @Benchmark
    public SimpleHashSet<Object> simpleSetUnionTest(SimpleSetState state1, SimpleSetState state2) {
        state1.set.addAll(state2.set);
        return state1.set;
    }

    @State(Scope.Thread)
    public static class BitSetState {
        BitSet set;

        @Setup(Level.Invocation)
        public void doSetup() {
            int setSize = (int)(uniqueElementsSize * percentPresent);
            set = new BitSet(uniqueElementsSize);
            ThreadLocalRandom.current().ints(0, uniqueElementsSize).distinct().limit(setSize).forEach(set::add);
        }
    }

    @Benchmark
    public BitSet bitSetUnionTest(BitSetState state1, BitSetState state2) {
        state1.set.union(state2.set);
        return state1.set;
    }
}

