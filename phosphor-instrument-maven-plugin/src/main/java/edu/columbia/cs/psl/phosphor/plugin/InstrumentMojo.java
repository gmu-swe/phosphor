package edu.columbia.cs.psl.phosphor.plugin;

import edu.columbia.cs.psl.phosphor.driver.DeletingFileVisitor;
import edu.columbia.cs.psl.phosphor.driver.InstrumentUtil;
import edu.columbia.cs.psl.phosphor.driver.Instrumentation;
import edu.columbia.cs.psl.phosphor.driver.Instrumenter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates an instrumented Java installation (i.e., Java Development Kit or Java Runtime Environment).
 * <p>
 * If {@link InstrumentMojo#outputDirectory} does not exists, a new instrumented Java installation is created.
 * If {@link InstrumentMojo#outputDirectory} exists and is not a Java installation previously created by
 * this plugin, this plugin will throw a {@link MojoExecutionException}.
 * If {@link InstrumentMojo#outputDirectory} exists and is a Java installation previously created by this plugin,
 * this plugin checks whether the Java installation needs to be recreated.
 * If the {@link InstrumentMojo#forceCreation} is {@code false} and the existing Java installation was created from
 * the same {@link InstrumentMojo#javaHome uninstrumented Java installation} using the same
 * {@link InstrumentMojo#instrumentationType} and {@link InstrumentMojo#options}, then the Java installation does
 * not need to be recreated and the plugin terminates.
 * Otherwise, this plugin will delete the existing Java installation and any
 * {@link InstrumentMojo#linkedCaches linked files or directories}.
 * Then, this plugin will create a new instrumented Java installation.
 * <p>
 * The instrumented Java installation is created by instrumenting the Java installation located in the directory
 * {@link InstrumentMojo#javaHome} or the Java installation used to run the Maven process if
 * {@link InstrumentMojo#javaHome} was not specified.
 * An instance of the specified {@link InstrumentMojo#instrumentationType instrumentation class} is created and
 * configured using the specified {@link InstrumentMojo#options}.
 * This instance determines the type of instrumentation applied.
 *
 * @see Instrumentation
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES)
public class InstrumentMojo extends AbstractMojo {
    /**
     * Directory where the Java installation to be instrumented is located.
     * If not specified, then the Java installation used to run the Maven process will be used.
     */
    @Parameter(property = "phosphor.javaHome")
    private File javaHome = new File(System.getProperty("java.home"));
    /**
     * Directory to which the instrumented Java installation should be written.
     */
    @Parameter(property = "phosphor.outputDirectory", defaultValue = "${project.build.directory}/phosphor/java/")
    private File outputDirectory;
    /**
     * List of directories and files that should be deleted if an existing instrumented Java installation is deleted.
     * These can be used to specify caches of dynamically instrumented classes.
     */
    @Parameter(property = "phosphor.linkedCaches")
    private List<File> linkedCaches = new LinkedList<>();
    /**
     * True if a new instrumented Java installation should be created even if a Java installation instrumented by this
     * plugin exists and was created using the same settings that would be used by this plugin.
     */
    @Parameter(property = "phosphor.forceCreation", readonly = true, defaultValue = "false")
    private boolean forceCreation;
    /**
     * Fully qualified name of the implementation of {@link Instrumentation} that should be used to instrument the
     * Java installation.
     * This plugin must be configured to add the the artifact that contains this implementation as a dependency using
     * the "dependencies" Maven tag for plugins.
     *
     * @see <a href="https://maven.apache.org/guides/mini/guide-configuring-plugins.html#using-the-dependencies-tag">
     *     Using the Dependencies Tag
     *   </a>
     * @see Class#forName(String className)
     */
    @Parameter(
            property = "phosphor.instrumentationType",
            defaultValue = "edu.columbia.cs.psl.phosphor.driver.PhosphorInstrumentation")
    private String instrumentationType;
    /**
     * Options passed to {@link Instrumentation#configure}.
     */
    @Parameter(property = "phosphor.options")
    private Properties options = new Properties();
    /**
     * A comma-separated list of Java modules to include in the instrumented Java installation.
     * Used only for instrumenting Java 9+ installations.
     */
    @Parameter(property = "phosphor.modules", defaultValue = "ALL-MODULE-PATH")
    private String modules;
    /**
     * True is information about instrumentation progress should be logged.
     */
    @Parameter(property = "phosphor.verbose", defaultValue = "false")
    private boolean verbose;

    /**
     * Creates an instrumented Java installation.
     *
     * @throws MojoExecutionException if an instrumented Java installation could not be created
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (!InstrumentUtil.isJavaHome(javaHome)) {
            throw new MojoExecutionException("Expected Java installation at: " + javaHome);
        }
        Instrumentation instance = createInstrumentation();
        byte[] checksum = computeChecksum(instance);
        String info = String.format("%s%n%s", javaHome.getAbsolutePath(), instrumentationType);
        MatchInfo match = new MatchInfo(outputDirectory);
        if (InstrumentUtil.isJavaHome(outputDirectory) && match.exists()) {
            if (!forceCreation && match.check(checksum, info, options)) {
                getLog().info("Existing instrumented Java installation with correct settings found: "
                        + outputDirectory);
                getLog().info("Skipping creation.");
            } else {
                if (!forceCreation) {
                    getLog().info("Existing Java installation did not have correct settings.");
                }
                deleteExisting();
                instrument(instance, checksum, info, match);
            }
        } else if (outputDirectory.exists()) {
            String message = "Failed to create instrumented Java installation."
                    + " %s already exists and is not an instrumented Java installation.";
            throw new MojoExecutionException(String.format(message, outputDirectory));
        } else {
            instrument(instance, checksum, info, match);
        }
    }

    private Instrumentation createInstrumentation() throws MojoExecutionException {
        try {
            return Instrumentation.create(instrumentationType, javaHome, options);
        } catch (ClassCastException | IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException(
                    "Error occurred while creating instrumentation instance: " + instrumentationType, e);
        }
    }

    private void deleteExisting() throws MojoExecutionException {
        try {
            Files.walkFileTree(outputDirectory.toPath(), new DeletingFileVisitor());
            getLog().info("Deleted existing Java installation: " + outputDirectory);
            for (File file : linkedCaches) {
                if (file.exists()) {
                    Files.walkFileTree(file.toPath(), new DeletingFileVisitor());
                    getLog().info("Deleted linked cache:" + file);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to delete existing instrumented Java installation and caches", e);
        }
    }

    private void instrument(Instrumentation instance, byte[] checksum, String info, MatchInfo match)
            throws MojoExecutionException {
        getLog().info("Creating Java installation: " + outputDirectory);
        try {
            long elapsedTime =
                    Instrumenter.instrument(javaHome, outputDirectory, options, instance, verbose, modules);
            getLog().info(String.format("Finished creating instrumented Java installation after %d ms", elapsedTime));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create instrumented Java instrumentation.", e);
        }
        match.write(checksum, info, options);
    }

    private static byte[] computeChecksum(Instrumentation instrumentation) throws MojoExecutionException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (File f : instrumentation.getClassPathElements()) {
                if (f.isFile()) {
                    buffer.write(InstrumentUtil.readAllBytes(f));
                }
            }
            return InstrumentUtil.checksum(buffer.toByteArray());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to compute instrumentation checksum", e);
        }
    }

    private static final class MatchInfo {
        /**
         * File used to store the options used to instrument a location.
         * <p>
         * Non-null.
         */
        private final File optionsFile;
        /**
         * File used to store the checksum for the class path of the instrumentation used to instrument a
         * location.
         * <p>
         * Non-null.
         */
        private final File checksumFile;
        /**
         * File used to store the path of the source location instrumented and the fully qualified name of the
         * implementation of {@link Instrumentation} used to instrument a location.
         */
        private final File infoFile;

        public MatchInfo(File directory) {
            File parent = new File(directory, "phosphor-instrument-match");
            this.optionsFile = new File(parent, "options.properties");
            this.checksumFile = new File(parent, "class-path.md5");
            this.infoFile = new File(parent, "info.txt");
        }

        public boolean exists() {
            return optionsFile.isFile() && checksumFile.isFile() && infoFile.isFile();
        }

        private boolean check(byte[] checksum, String info, Properties options) throws MojoExecutionException {
            try (FileReader reader = new FileReader(optionsFile)) {
                Properties foundOptions = new Properties();
                foundOptions.load(reader);
                return foundOptions.equals(options)
                        && Arrays.equals(checksum, InstrumentUtil.readAllBytes(checksumFile))
                        && new String(InstrumentUtil.readAllBytes(infoFile)).equals(info);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read match info", e);
            }
        }

        public void write(byte[] checksum, String info, Properties options) throws MojoExecutionException {
            try {
                InstrumentUtil.ensureDirectory(optionsFile.getParentFile());
                Files.write(checksumFile.toPath(), checksum);
                Files.write(infoFile.toPath(), info.getBytes());
                try (FileWriter writer = new FileWriter(optionsFile)) {
                    options.store(writer, null);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write match info", e);
            }
        }
    }
}
