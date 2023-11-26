package edu.columbia.cs.psl.phosphor.driver;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.agent.PhosphorAgent;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.CommandLine;

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
    private PCLoggingTransformer transformer;
    private Set<File> classPathElements;

    @Override
    public void configure(File source, Properties options) throws IOException {
        options.remove("help");
        CommandLine line = PhosphorOption.configure(options, source, new File("temp/"));
        assert line != null;
        initialize(line);
    }

    public void initialize(CommandLine line) throws IOException {
        File source = new File(line.getArgs()[0]);
        Set<Class<?>> configurationClasses = PhosphorOption.getClassOptionValues(line);
        if (InstrumentUtil.isJavaHome(source)) {
            Configuration.IS_JAVA_8 = !InstrumentUtil.isModularJvm(source);
        }
        setUpClassLoader(source);
        Configuration.init();
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        transformer = new PCLoggingTransformer();
        classPathElements = new HashSet<>();
        classPathElements.add(InstrumentUtil.getClassPathElement(PhosphorAgent.class));
        configurationClasses.stream()
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
        return !classFileName.endsWith("module-info.class")
                && !classFileName.startsWith("org/")
                && !classFileName.startsWith("edu/columbia/cs/psl/phosphor/agent/")
                && !classFileName.startsWith("edu/columbia/cs/psl/phosphor/driver/")
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
        Phosphor.bigLoader = new URLClassLoader(urls.toArray(new URL[0]), Instrumenter.class.getClassLoader());
    }
}
