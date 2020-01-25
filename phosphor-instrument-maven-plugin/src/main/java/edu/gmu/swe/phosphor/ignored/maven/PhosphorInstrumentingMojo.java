package edu.gmu.swe.phosphor.ignored.maven;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static edu.gmu.swe.phosphor.ignored.maven.PhosphorInstrumentUtil.createPhosphorMainArguments;
import static edu.gmu.swe.phosphor.ignored.maven.PhosphorInstrumentUtil.generateChecksumForPhosphorJar;

/**
 * Creates a Phosphor-instrumented JVM at a target location. If a directory already exists at the target location,
 * checks to see if it contains a phosphor-properties file and phosphor-checksum.md5sum file. If it does and the
 * phosphor-properties file's properties match the desired properties for the Phosphor-instrumented JVM to be created,
 * and the phosphor-checksum.md5sum file's checksum matches the checksum of the Phosphor JAR current loaded, this
 * directory is assumed to contain a properly instrumented JVM. Otherwise, deletes any existing files or directories at
 * the target location and creates a new Phosphor-instrumented JVM there.
 * <p>
 * If a new Phosphor-instrumented JVM is generated, deletes any directories listed as being used to cache files
 * dynamically instrumented by Phosphor when using the previous Phosphor-instrumented JVM.
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.COMPILE)
public class PhosphorInstrumentingMojo extends AbstractMojo {

    /**
     * Constant name of the file used to store information about the configuration options that were used by Phosphor
     * when instrumenting a JVM.
     */
    public static final String PROPERTIES_FILE_NAME = "phosphor-properties";

    /**
     * Constant name of the file used to store the checksum of the Phosphor JAR used to instrument a JVM.
     */
    public static final String CHECKSUM_FILE_NAME = "phosphor-checksum.md5sum";

    /**
     * Path to directory where the JDK or JRE installation to be instrumented is installed.
     */
    @Parameter(property = "sourceJVMDir", readonly = true)
    private String sourceJVMDir;

    /**
     * Path to a directory in which the target location for the instrumented JVM should be placed.
     */
    @Parameter(property = "targetBaseDir", defaultValue = "${project.build.directory}", readonly = true)
    private File targetBaseDir;

    /**
     * Name of the target location directory for the instrumented JVM.
     */
    @Parameter(property = "instrumentedJVMName", readonly = true, required = true)
    private String targetName;

    /**
     * Phosphor configuration options that should be set while instrumenting the JVM.
     */
    @Parameter(property = "options", readonly = true, required = true)
    private Properties options;

    /**
     * True if an existing instrumented JVM should be invalidated and rebuilt if it was instrumented with a Phosphor
     * JAR file whose checksum does not match the current Phosphor JAR file's checksum
     */
    @Parameter(property = "reinstrumentBasedOnChecksum", readonly = true, defaultValue = "true")
    private boolean reinstrumentBasedOnChecksum;

    /**
     * List of paths to directories that are used to cache files dynamically instrumented by Phosphor that
     * should be delete if a new Phosphor-instrumented JVM is generated
     */
    @Parameter(property = "associatedCaches", readonly = true)
    private List<String> associatedCaches;

    /**
     * Creates a Phosphor-instrumented JVM at a target location if one does not already exist with the correct
     * properties and checksum.
     *
     * @throws MojoFailureException if the Phosphor-instrumented JVM cannot be created at the target location
     */
    @Override
    public void execute() throws MojoFailureException {
        Properties canonicalOptions = PhosphorInstrumentUtil.canonicalizeProperties(options, false);
        File jvmDir = getJVMDir(sourceJVMDir);
        File instJVMDir = new File(targetBaseDir, targetName);
        byte[] checksum;
        try {
            checksum = generateChecksumForPhosphorJar();
        } catch(IOException e) {
            throw new MojoFailureException("Failed to generate checksum for Phosphor JAR");
        }
        if(!isExistingInstrumentedJVM(instJVMDir, canonicalOptions, checksum)) {
            getLog().info(String.format("Generating Phosphor-instrumented JVM %s with options: %s", instJVMDir, canonicalOptions));
            try {
                generateInstrumentedJVM(jvmDir, instJVMDir, canonicalOptions, checksum);
            } catch(IOException e) {
                throw new MojoFailureException("Failed to create instrumented JVM", e);
            }
            // Delete associated caches
            for(String associateCache : associatedCaches) {
                if(associateCache != null) {
                    deleteCache(new File(associateCache));
                }
            }
        } else {
            getLog().info(String.format("No generation necessary: existing Phosphor-instrumented JVM %s with correct " +
                    "properties(%s) and checksum found", instJVMDir, canonicalOptions));
        }
    }

    /**
     * Checks whether the specified file is a directory containing a phosphor-properties file whose properties match
     * the specified desired properties and a phosphor-checksum file whose bytes match the specified checksum
     * (if invalidating invalidateBasedOnChecksum is true).
     *
     * @param instJVMDir        path to be checked for an existing instrumented JVM
     * @param desiredProperties the properties that the existing instrumented JVM must have
     * @param checksum          the checksum that existing instrumented JVM must have associated with it
     * @return true if instJVMDir is a directory that contains a phosphor-properties file that matches desiredProperties
     * and a phosphor-checksum file whose bytes match the specified checksum (if invalidating
     * invalidateBasedOnChecksum is true).
     */
    private boolean isExistingInstrumentedJVM(File instJVMDir, Properties desiredProperties, byte[] checksum) {
        if(instJVMDir.isDirectory()) {
            File propFile = new File(instJVMDir, PROPERTIES_FILE_NAME);
            if(propFile.isFile()) {
                try {
                    Properties existingProperties = new Properties();
                    existingProperties.load(new FileReader(propFile));
                    if(!desiredProperties.equals(existingProperties)) {
                        return false;
                    }
                } catch(IOException e) {
                    return false;
                }
            }
            if(reinstrumentBasedOnChecksum) {
                try {
                    byte[] existingChecksum = Files.readAllBytes(new File(instJVMDir, CHECKSUM_FILE_NAME).toPath());
                    return Arrays.equals(checksum, existingChecksum);
                } catch(IOException e) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to delete the specified cache directory. Logs a warning if deletion is unsuccessful.
     *
     * @param cacheDir directory containing a cache files dynamically instrumented by Phosphor
     */
    private void deleteCache(File cacheDir) {
        if(cacheDir.exists()) {
            getLog().info("Deleting associated Phosphor cache directory: " + cacheDir);
            try {
                Files.walkFileTree(cacheDir.toPath(), new DeletingFileVisitor());
            } catch(IOException e) {
                getLog().warn("Failed to delete associated Phosphor cache directory: " + cacheDir);
                getLog().warn(e);
            }
        }
    }

    /**
     * Determines the correct path for the directory where the JDK or JRE installation to be instrumented is installed
     * as follows:
     * Use the value of baseJVM if it provided and is non-null and not the empty string. Else use the value of the
     * environmental variable INST_HOME if it is set to a non-null, non empty string value. Else use the value of
     * the environmental variable JAVA_HOME if it is set to a non-null, non empty string value. Otherwise no appropriate
     * path value was provided.
     *
     * @param sourceJVMDir if non-null and non-empty specified the location of the JDK or JRE installation to be
     *                     instrumented
     * @throws MojoFailureException if an appropriate JDK or JRE installation path was not provided or the provided
     *                              path does not point to a directory
     * @ return a file representing the path to the determined JDK or JRE installation directory
     */
    private static File getJVMDir(String sourceJVMDir) throws MojoFailureException {
        String path;
        String source;
        if(sourceJVMDir != null && !sourceJVMDir.isEmpty()) {
            path = sourceJVMDir;
            source = "sourceJVMDir property";
        } else if(System.getenv("INST_HOME") != null && !System.getenv("INST_HOME").isEmpty()) {
            path = System.getenv("INST_HOME");
            source = "INST_HOME environmental variable";
        } else if(System.getenv("JAVA_HOME") != null && !System.getenv("JAVA_HOME").isEmpty()) {
            path = System.getenv("JAVA_HOME");
            source = "JAVA_HOME environmental variable";
        } else {
            throw new MojoFailureException("Either baseJVM property or INST_HOME environmental variable or JAVA_HOME " +
                    "environmental variable must be set to a directory where the JDK or JRE is installed");
        }
        File jvmDir = new File(path);
        if(!jvmDir.isDirectory()) {
            throw new MojoFailureException(String.format("Value for %s (%s) must be set to a directory where the " +
                    "JDK or JRE is installed", source, path));
        }
        return jvmDir;
    }

    /**
     * Creates a Phosphor-instrumented JVM at the specified directory with the specified Phosphor configuration options.
     *
     * @param jvmDir     the source directory where the JDK or JRE is installed
     * @param instJVMDir the target directory where the Phosphor-instrumented JVM should be created
     * @param properties canonicalized properties that specify the Phosphor configuration options that should be used
     * @param checksum   the checksum of the Phosphor JAR that will be used to instrument the JVM
     * @throws IOException if an I/O error occurs
     */
    private static void generateInstrumentedJVM(File jvmDir, File instJVMDir, Properties properties, byte[] checksum) throws IOException {
        if(instJVMDir.exists()) {
            // Delete existing directory or file if necessary
            Files.walkFileTree(instJVMDir.toPath(), new DeletingFileVisitor());
        }
        if(!instJVMDir.mkdirs()) {
            throw new IOException("Failed to create target directory for Phosphor-instrumented JVM: " + instJVMDir);
        }
        Instrumenter.main(createPhosphorMainArguments(jvmDir, instJVMDir, properties));
        // Add phosphor-properties file to directory
        File propsFile = new File(instJVMDir, PROPERTIES_FILE_NAME);
        properties.store(new FileWriter(propsFile), null);
        // Add phosphor-checksum file to directory
        File checksumFile = new File(instJVMDir, CHECKSUM_FILE_NAME);
        Files.write(checksumFile.toPath(), checksum);
        // Set execute permissions
        Files.walkFileTree(new File(instJVMDir, "bin").toPath(), new ExecutePermissionAssigningFileVisitor());
        Files.walkFileTree(new File(instJVMDir, "lib").toPath(), new ExecutePermissionAssigningFileVisitor());
        if(new File(instJVMDir, "jre").exists()) {
            Files.walkFileTree(new File(instJVMDir, "jre" + File.separator + "bin").toPath(), new ExecutePermissionAssigningFileVisitor());
            Files.walkFileTree(new File(instJVMDir, "jre" + File.separator + "lib").toPath(), new ExecutePermissionAssigningFileVisitor());
        }
    }
}
