package edu.gmu.swe.phosphor.instrument;

import java.io.File;
import java.io.IOException;

public final class PhosphorInstrumenter {
    private PhosphorInstrumenter() {
        throw new AssertionError("Tried to instantiate static utility class: " + getClass());
    }

    public static void main(String[] args) throws IOException {
        // TODO
        File source = new File(args[0]);
        File target = new File(args[1]);
        boolean verbose = Boolean.parseBoolean(args[2]);
        String modules = args[3];
        System.out.printf("Instrumenting %s to %s%n", source, target);
        long elapsedTime = Instrumenter.instrument(source, target, null, null, verbose, modules);
        System.out.printf("Finished generation after %dms%n", elapsedTime);
    }
}
