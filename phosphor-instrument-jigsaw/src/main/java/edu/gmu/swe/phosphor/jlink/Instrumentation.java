package edu.gmu.swe.phosphor.jlink;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Implementors are expected to have a zero-argument, public constructor.
 */
public interface Instrumentation {
    /**
     * Configures this instance using the specified options.
     * @param inputDirectory the directory to be instrumented
     * @param outputDirectory the directory to which the instrumented output should be written
     * @param options the key-value pairs that should be used to configure this instance
     * @throws IOException if an I/O error occurs
     */
    void configure(File inputDirectory, File outputDirectory, Properties options)
            throws IOException, ReflectiveOperationException;

    /**
     * Returns an array of class path elements needed to use this class.
     * The returned array should be non-null.
     * All elements of the returned array should be non-null.
     * @return class path elements needed to use this class
     */
    File[] getClassPathElements();

    byte[] apply(byte[] classFileBuffer);
}
