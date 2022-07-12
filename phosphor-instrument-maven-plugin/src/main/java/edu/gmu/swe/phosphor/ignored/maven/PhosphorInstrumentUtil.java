package edu.gmu.swe.phosphor.ignored.maven;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.Option;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.Options;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Utility class that provides methods to assist with Phosphor instrumentation.
 */
public class PhosphorInstrumentUtil {

    /**
     * MD5 MessageDigest instance used for generating checksums.
     */
    private static MessageDigest md5Inst;

    static {
        try {
            md5Inst = MessageDigest.getInstance("MD5");
        } catch(Exception e) {
            System.err.println("Failed to create MD5 MessageDigest");
            e.printStackTrace();
        }
    }

    private PhosphorInstrumentUtil() {

    }

    /**
     * Creates a standardized copy of the specified properties where each Phosphor option without an argument is mapped
     * to either true or not present in the properties, each Phosphor option with an argument is either mapped to a
     * non-null, non-empty string or not present in properties, and no other keys are present in the properties.
     *
     * @param isRuntimeInst true if the options should be standardized against the options available during dynamic
     *                      instrumentation, otherwise standardizes against the options available during static
     *                      instrumentation
     * @param properties    un-standardized properties to be standardized
     * @return a standardized copy of properties
     */
    public static Properties canonicalizeProperties(Properties properties, boolean isRuntimeInst) {
        Set<String> propNames = properties.stringPropertyNames();
        Properties canonicalProps = new Properties();
        Map<String, Option> phosphorOptionMap = createPhosphorOptionMap(isRuntimeInst);
        for(String propName : propNames) {
            if(phosphorOptionMap.containsKey(propName)) {
                Option option = phosphorOptionMap.get(propName);
                if(option.hasArg()) {
                    if(properties.getProperty(propName) != null && properties.getProperty(propName).length() > 0) {
                        canonicalProps.setProperty(option.getOpt(), properties.getProperty(propName));
                    }
                } else {
                    if(properties.getProperty(propName).length() == 0 || "true".equals(properties.getProperty(propName).toLowerCase())) {
                        canonicalProps.setProperty(option.getOpt(), "true");
                    }
                }
            } else {
                System.err.println("Unknown Phosphor option: " + propName);
            }
        }
        return canonicalProps;
    }

    /**
     * @param isRuntimeInst true if a map of options available during dynamic instrumentation should be returned,
     *                      otherwise a map of option available during static instrumentation should be returned
     * @return a mapping from the names of configuration options available in Phosphor to an instance of
     * org.apache.commons.cli.Option that represents that configuration option
     */
    public static Map<String, Option> createPhosphorOptionMap(boolean isRuntimeInst) {
        Map<String, Option> phosphorOptionMap = new HashMap<String, Option>();
        Options options = PhosphorOption.createOptions(isRuntimeInst);
        for(Option option : options.getOptions()) {
            phosphorOptionMap.put(option.getOpt(), option);
            if(option.hasLongOpt()) {
                phosphorOptionMap.put(option.getLongOpt(), option);
            }
        }
        return phosphorOptionMap;
    }

    /**
     * @return a File object pointing to the JAR file for Phosphor
     */
    public static File getPhosphorJarFile() {
        try {
            return new File(Instrumenter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch(URISyntaxException e) {
            throw new AssertionError();
        }
    }

    /**
     * @return the checksum of the JAR file for Phosphor
     */
    public static byte[] generateChecksumForPhosphorJar() throws IOException {
        byte[] jarBytes = Files.readAllBytes(PhosphorInstrumentUtil.getPhosphorJarFile().toPath());
        return md5Inst.digest(jarBytes);
    }

    /**
     * Ensures that the specified directory exists and is empty.
     *
     * @param dir the directory to be created or cleaned
     * @throws IOException if the specified directory could not be created or cleaned
     */
    public static void createOrCleanDirectory(File dir) throws IOException {
        if(dir.isDirectory()) {
            FileUtils.cleanDirectory(dir);
        } else {
            if(dir.isFile()) {
                if(!dir.delete()) {
                    throw new IOException("Failed to delete: " + dir);
                }
            }
            if(!dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + dir);
            }
        }
    }

    /**
     * @param properties canonicalized properties that specify the Phosphor configuration options that should set in the
     *                   created argument
     * @return a String formatted for {@link edu.columbia.cs.psl.phosphor.PreMain#premain(String, Instrumentation) premain's}
     * String argument
     */
    public static String createPhosphorAgentArgument(Properties properties) {
        if(properties.isEmpty()) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder().append("=");
            Set<String> propNames = properties.stringPropertyNames();
            boolean first = true;
            for(String propName : propNames) {
                if(first) {
                    first = false;
                } else {
                    builder.append(',');
                }
                builder.append(propName);
                if(!"true".equals(properties.getProperty(propName))) {
                    builder.append('=').append(properties.getProperty(propName));
                }
            }
            return builder.toString();
        }
    }

    /**
     * @param jvmDir     the source directory where the JDK or JRE is installed
     * @param instJVMDir the target directory where the Phosphor-instrumented JVM should be created
     * @param properties canonicalized properties that specify the Phosphor configuration options that should set in the
     *                   created arguments
     * @return an array formatted for {@link Instrumenter#main(String[])} Instrumenter.main's} String[] argument
     */
    public static String[] createPhosphorMainArguments(File jvmDir, File instJVMDir, Properties properties) {
        SinglyLinkedList<String> arguments = new SinglyLinkedList<>();
        Set<String> propNames = properties.stringPropertyNames();
        for(String propName : propNames) {
            arguments.addLast("-" + propName);
            if(!"true".equals(properties.getProperty(propName))) {
                arguments.addLast(properties.getProperty(propName));
            }
        }
        arguments.addLast(jvmDir.getAbsolutePath());
        arguments.addLast(instJVMDir.getAbsolutePath());
        return arguments.toArray(new String[0]);
    }
}
