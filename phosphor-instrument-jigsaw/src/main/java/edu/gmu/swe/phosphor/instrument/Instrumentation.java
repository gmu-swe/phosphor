package edu.gmu.swe.phosphor.instrument;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * Implementors are expected to have a zero-argument, public constructor.
 */
public interface Instrumentation {
    /**
     * Configures this instance using the specified options.
     *
     * @param source  the location to be instrumented
     * @param options the key-value pairs that should be used to configure this instance
     * @throws IOException if an I/O error occurs
     */
    void configure(File source, Properties options) throws IOException, ReflectiveOperationException;

    /**
     * Returns an array of class path elements needed to use this class.
     * The returned array should be non-null.
     * All elements of the returned array should be non-null.
     *
     * @return class path elements needed to use this class
     */
    Set<File> getClassPathElements();

    byte[] apply(byte[] classFileBuffer);

    boolean shouldPack(String classFileName);

    Set<File> getElementsToPack();

    Patcher createPatcher(Function<String, byte[]> entryLocator);

    static Instrumentation create(String className, File source, Properties options)
            throws ReflectiveOperationException, IOException {
        Class<?> clazz = Class.forName(className, true, Instrumentation.class.getClassLoader());
        Instrumentation instance = (Instrumentation) clazz.getConstructor().newInstance();
        instance.configure(source, options);
        return instance;
    }
}
