package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.*;

public class OptionsUtil {
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
        for (String propName : propNames) {
            if (phosphorOptionMap.containsKey(propName)) {
                Option option = phosphorOptionMap.get(propName);
                if (option.hasArg()) {
                    if (properties.getProperty(propName) != null
                            && !properties.getProperty(propName).isEmpty()) {
                        canonicalProps.setProperty(option.getOpt(), properties.getProperty(propName));
                    }
                } else {
                    if (properties.getProperty(propName).isEmpty()
                            || "true".equalsIgnoreCase(properties.getProperty(propName))) {
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
    private static Map<String, Option> createPhosphorOptionMap(boolean isRuntimeInst) {
        Map<String, Option> phosphorOptionMap = new HashMap<>();
        Options options = PhosphorOption.createOptions(isRuntimeInst);
        for (Option option : options.getOptions()) {
            phosphorOptionMap.put(option.getOpt(), option);
            if (option.hasLongOpt()) {
                phosphorOptionMap.put(option.getLongOpt(), option);
            }
        }
        return phosphorOptionMap;
    }

    public static String[] createPhosphorMainArguments(
            File inputDirectory, File outputDirectory, Properties properties) {
        properties = canonicalizeProperties(properties, false);
        SinglyLinkedList<String> arguments = new SinglyLinkedList<>();
        Set<String> propNames = properties.stringPropertyNames();
        for (String propName : propNames) {
            arguments.addLast("-" + propName);
            if (!"true".equals(properties.getProperty(propName))) {
                arguments.addLast(properties.getProperty(propName));
            }
        }
        arguments.addLast(inputDirectory.getAbsolutePath());
        arguments.addLast(outputDirectory.getAbsolutePath());
        return arguments.toArray(new String[0]);
    }

    public static Set<Class<?>> getConfigurationClasses(Properties properties) {
        Set<Class<?>> classes = new HashSet<>();
        Options options = PhosphorOption.createOptions(false);
        for (Option option : options.getOptions()) {
            if (option.getType().equals(Class.class)) {
                String key = option.getOpt();
                if (properties.containsKey(key)) {
                    try {
                        Class<?> clazz = Class.forName(properties.getProperty(key));
                        classes.add(clazz);
                    } catch (ReflectiveOperationException e) {
                        String message =
                                String.format("Failed to create %s class: %s", key, properties.getProperty(key));
                        throw new IllegalArgumentException(message, e);
                    }
                }
            }
        }
        return classes;
    }
}
