package edu.columbia.cs.psl.phosphor.driver;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.IOException;

public final class PhosphorInstrumenter {
    private PhosphorInstrumenter() {
        throw new AssertionError("Tried to instantiate static utility class: " + getClass());
    }

    public static void main(String[] args) throws IOException {
        PhosphorInstrumentation instrumentation = new PhosphorInstrumentation();
        CommandLine line = PhosphorOption.configure(false, args);
        if(line == null) {
            // The "help" option was specified
            return;
        }
        File source = new File(args[0]);
        File destination = new File(args[1]);
        instrumentation.initialize(line);
        boolean verbose = !Configuration.QUIET_MODE;
        String modules = line.hasOption("jvmModules") ? line.getOptionValue("jvmModules") : "ALL-MODULE-PATH";
        System.out.printf("Instrumenting %s to %s%n", source, destination);
        long elapsedTime = Instrumenter.instrument(source, destination, PhosphorOption.toProperties(line),
                instrumentation, verbose, modules);
        System.out.printf("Finished instrumentation after %dms%n", elapsedTime);
    }
}
