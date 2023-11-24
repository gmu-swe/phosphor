package edu.gmu.swe.phosphor.instrument;

import edu.gmu.swe.phosphor.jlink.DeletingFileVisitor;
import edu.gmu.swe.phosphor.jlink.InstrumentDriver;
import edu.gmu.swe.phosphor.jlink.InstrumentUtil;
import edu.gmu.swe.phosphor.jlink.Instrumentation;
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
 * Creates an instrumented Java installation.
 * <p>
 * If {@link InstrumentingMojo#outputDirectory} does not exists, a new instrumented Java installation is created.
 * If {@link InstrumentingMojo#outputDirectory} exists and is not a Java installation previously created by
 * this plugin, this plugin will throw a {@link MojoExecutionException}.
 * If {@link InstrumentingMojo#outputDirectory} exists and is a Java installation previously created by this plugin,
 * this plugin checks whether the Java installation needs to be recreated.
 * If the {@link InstrumentingMojo#forceCreation} is {@code false} and the existing Java installation was created from
 * the same {@link InstrumentingMojo#javaHome uninstrumented Java installation} using the same
 * {@link InstrumentingMojo#instrumentationType} and {@link InstrumentingMojo#options}, then the Java installation does
 * not need to be recreated and the plugin terminates.
 * Otherwise, this plugin will delete the existing Java installation and any
 * {@link InstrumentingMojo#linkedCaches linked files or directories}.
 * Then, this plugin will create a new instrumented Java installation.
 * <p>
 * The instrumented Java installation is created by instrumenting the Java installation located in the directory
 * {@link InstrumentingMojo#javaHome} or the Java installation used to run the Maven process if
 * {@link InstrumentingMojo#javaHome} was not specified.
 * An instance of the specified {@link InstrumentingMojo#instrumentationType instrumentation class} is created and
 * configured using the specified {@link InstrumentingMojo#options}.
 * This instance determines the type of instrumentation applied.
 *
 * @see edu.gmu.swe.phosphor.jlink.Instrumentation
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES)
public class InstrumentingMojo extends AbstractMojo {
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
            defaultValue = "edu.gmu.swe.phosphor.jlink.PhosphorInstrumentation")
    private String instrumentationType;
    /**
     * Options passed to {@link Instrumentation#configure}.
     */
    @Parameter(property = "phosphor.options")
    private Properties options = new Properties();
    /**
     * File used to store the options used by this plugin to create the instrumented Java installation
     */
    private File optionsFile;
    /**
     * File used to store the checksum for the instrumentation used by this plugin to create the instrumented Java
     * installation
     */
    private File checksumFile;
    /**
     * File used to store the path of the uninstrumented Java installation and fully qualified name of the
     * implementation of {@link Instrumentation} used by this plugin to create the instrumented Java installation
     */
    private File infoFile;

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
        optionsFile = new File(outputDirectory, "phosphor-instrument" + File.separator + "option.properties");
        checksumFile = new File(optionsFile.getParent(), "checksum.md5");
        infoFile = new File(optionsFile.getParent(), "info.txt");
        Instrumentation instance = createInstrumentation();
        byte[] checksum = computeChecksum(instance);
        String info = String.format("%s%n%s", javaHome.getAbsolutePath(), instrumentationType);
        if (InstrumentUtil.isJavaHome(outputDirectory)
                && checksumFile.isFile()
                && optionsFile.isFile()
                && infoFile.isFile()) {
            if (!forceCreation && checkMatchFiles(checksum, info)) {
                getLog().info("Existing instrumented Java installation with correct settings found: "
                        + outputDirectory);
                getLog().info("Skipping creation.");
            } else {
                if (!forceCreation) {
                    getLog().info("Existing Java installation did not have correct settings.");
                }
                getLog().info("Recreating Java installation : " + outputDirectory);
                deleteExistingJdkAndLinkedCaches();
                createInstrumentedJdk(instance, checksum, info);
            }
        } else if (outputDirectory.exists()) {
            String message = "Failed to create instrumented Java installation."
                    + " %s already exists and is not an instrumented Java installation.";
            throw new MojoExecutionException(String.format(message, outputDirectory));
        } else {
            getLog().info("Creating Java installation : " + outputDirectory);
            createInstrumentedJdk(instance, checksum, info);
        }
    }

    private Instrumentation createInstrumentation() throws MojoExecutionException {
        try {
            Class<?> clazz = Class.forName(instrumentationType, true, getClass().getClassLoader());
            Instrumentation instance = (Instrumentation) clazz.getConstructor().newInstance();
            instance.configure(javaHome, outputDirectory, options);
            return instance;
        } catch (ClassCastException | IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException(
                    "Error occurred while creating instrumentation instance: " + instrumentationType, e);
        }
    }

    private boolean checkMatchFiles(byte[] checksum, String info) throws MojoExecutionException {
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

    private void deleteExistingJdkAndLinkedCaches() throws MojoExecutionException {
        try {
            Files.walkFileTree(outputDirectory.toPath(), new DeletingFileVisitor());
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

    private void createInstrumentedJdk(Instrumentation instance, byte[] checksum, String info)
            throws MojoExecutionException {
        try {
            InstrumentDriver.instrument(javaHome, outputDirectory, instance);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to instrument Java instrumentation.", e);
        }
        writeMatchFiles(checksum, info);
    }

    private void writeMatchFiles(byte[] checksum, String info) throws MojoExecutionException {
        try {
            InstrumentUtil.ensureDirectory(optionsFile.getParentFile());
            Files.write(checksumFile.toPath(), checksum);
            Files.write(infoFile.toPath(), info.getBytes());
            try (FileWriter writer = new FileWriter(optionsFile)) {
                options.store(writer, null);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write match files", e);
        }
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
}
