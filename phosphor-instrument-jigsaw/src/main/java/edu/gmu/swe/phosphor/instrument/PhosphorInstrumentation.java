package edu.gmu.swe.phosphor.instrument;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;

/**
 * Instances of this class are created via reflection.
 */
@SuppressWarnings("unused")
public class PhosphorInstrumentation implements Instrumentation {
    private PreMain.PCLoggingTransformer transformer;
    private Set<File> classPathElements;

    @Override
    public void configure(File source, Properties options) throws IOException {
        options.put("java8", String.valueOf(!InstrumentUtil.isModularJvm(source)));
        String[] arguments = OptionsUtil.createPhosphorMainArguments(options);
        setUpClassLoader(source);
        PhosphorOption.configure(false, arguments);
        Configuration.init();
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        transformer = new PreMain.PCLoggingTransformer();
        classPathElements = new HashSet<>();
        classPathElements.add(InstrumentUtil.getClassPathElement(PreMain.class));
        OptionsUtil.getConfigurationClasses(options).stream()
                .map(InstrumentUtil::getClassPathElement)
                .forEach(classPathElements::add);
    }

    @Override
    public java.util.Set<File> getClassPathElements() {
        return classPathElements;
    }

    @Override
    public byte[] apply(byte[] classFileBuffer) {
        return transformer.transform(null, null, null, null, classFileBuffer, false);
    }

    @Override
    public Patcher createPatcher(Function<String, byte[]> entryLocator) {
        String path = "/java.base/jdk/internal/misc/Unsafe.class";
        PhosphorPatcher patcher = new PhosphorPatcher(entryLocator.apply(path));
        return patcher::patch;
    }

    @Override
    public boolean shouldPack(String classFileName) {
        return !classFileName.startsWith("edu/columbia/cs/psl/jigsaw/phosphor/instrumenter")
                && !classFileName.endsWith("module-info.class")
                && !classFileName.startsWith("org/")
                && !classFileName.startsWith("edu/columbia/cs/psl/phosphor/runtime/jdk/unsupported")
                && classFileName.endsWith(".class");
    }

    @Override
    public Set<File> getElementsToPack() {
        return classPathElements;
    }

    private static void setUpClassLoader(File input) throws IOException {
        List<URL> urls = new ArrayList<>();
        if (input.isDirectory()) {
            Files.walkFileTree(input.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".jar")) {
                        urls.add(file.toUri().toURL());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else if (input.getName().endsWith(".jar")) {
            urls.add(input.toURI().toURL());
        }
        urls.add(input.toURI().toURL());
        PreMain.bigLoader = new URLClassLoader(urls.toArray(new URL[0]), Instrumenter.class.getClassLoader());
    }
}
