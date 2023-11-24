package edu.gmu.swe.phosphor.jlink;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class InstrumentDriver {
    private InstrumentDriver() {
        throw new AssertionError("Tried to instantiate static driver class: " + getClass());
    }

    /**
     * Usage: java -cp &lt;CLASS_PATH&gt; edu.gmu.swe.phosphor.jlink.InstrumentDriver
     * [OPTIONS]
     * &lt;INPUT_DIRECTORY&gt;
     * &lt;OUTPUT_DIRECTORY&gt;
     * java -cp [CLASS-PATH] -jar phosphor.jar [OPTIONS] [input] [output]
     * output_directory transformer_class core_jar [java_home]
     * <p>
     * output_directory: directory to which the instrumented JVM should be written.
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File inputDirectory = new File(args[0]);
        File outputDirectory = new File(args[1]);
        Class<?> transformerClass = Class.forName(args[1]);
        File coreJar = new File(args[2]);
        File javaHome = args.length == 3 ? new File(System.getProperty("java.home")) : new File(args[3]);
        System.out.printf("Instrumenting %s to %s%n", javaHome, outputDirectory);
        long elapsedTime = instrument(inputDirectory, outputDirectory, null);
        System.out.printf("Finished generation after %dms%n", elapsedTime);
    }

    public static long instrument(File inputDirectory, File outputDirectory, Instrumentation instrumentation)
            throws IOException {
        if (outputDirectory.exists()) {
            throw new IllegalArgumentException("Output directory exists.");
        }
        if (!inputDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid input directory: " + inputDirectory);
        }
        long startTime = System.currentTimeMillis();
        try {
            if (InstrumentUtil.isModularJvm(inputDirectory)) {
                // TODO
                // Arrays.asList("--add-modules", "ALL-MODULE-PATH")
            } else {
                new Instrumenter(instrumentation).process(inputDirectory, outputDirectory);
            }
            return System.currentTimeMillis() - startTime;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IOException("Failed to generate instrumented JVM", e);
        }
    }
}
