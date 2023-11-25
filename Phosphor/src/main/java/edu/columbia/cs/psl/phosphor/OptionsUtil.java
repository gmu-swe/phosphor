package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.*;

public class OptionsUtil {
    /**
     * Creates a standardized copy of the specified properties where each Phosphor option without an argument is mapped
     * to either true or not present in the properties, each Phosphor option with an argument is either mapped to a
     * non-null, non-empty string or not present in properties, and no other keys are present in the properties.
     *
     * @param properties properties to be standardized
     * @return a standardized copy of properties
     */
    private static Properties standardize(Properties properties) {
        Properties result = new Properties();
        Map<String, Option> phosphorOptionMap = createOptionMap();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (phosphorOptionMap.containsKey(key)) {
                Option option = phosphorOptionMap.get(key);
                if (option.hasArg() && value != null && !value.isEmpty()) {
                    result.setProperty(option.getOpt(), value);
                } else if (!option.hasArg() && (value == null || value.isEmpty() || "true".equalsIgnoreCase(value))) {
                    result.setProperty(option.getOpt(), "true");
                }
            } else {
                throw new IllegalArgumentException("Unknown option: " + key);
            }
        }
        return result;
    }

    /**
     * @return a mapping from the names of configuration options available in Phosphor to an instance of
     * org.apache.commons.cli.Option that represents that configuration option
     */
    private static Map<String, Option> createOptionMap() {
        Map<String, Option> map = new HashMap<>();
        Options options = PhosphorOption.createOptions(false);
        for (Option option : options.getOptions()) {
            map.put(option.getOpt(), option);
            if (option.hasLongOpt()) {
                map.put(option.getLongOpt(), option);
            }
        }
        return map;
    }

    public static String[] createPhosphorMainArguments(Properties properties) {
        properties = standardize(properties);
        SinglyLinkedList<String> arguments = new SinglyLinkedList<>();
        Set<String> propNames = properties.stringPropertyNames();
        for (String propName : propNames) {
            arguments.addLast("-" + propName);
            if (!"true".equals(properties.getProperty(propName))) {
                arguments.addLast(properties.getProperty(propName));
            }
        }
        arguments.addLast("temp/");
        arguments.addLast("temp2/");
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
