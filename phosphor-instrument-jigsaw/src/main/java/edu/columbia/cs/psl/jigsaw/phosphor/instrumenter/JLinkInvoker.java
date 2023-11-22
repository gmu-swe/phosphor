package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.Option;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class JLinkInvoker {
    public static final String MODULES_PROPERTY = "jvmModules";
    public static final String PACK_KEY = "phosphor.pack";

    public static void invokeJLink(File jvmDir, File instJVMDir, Properties properties) {
        String jlinkBin = jvmDir + File.separator + "bin" + File.separator + "jlink";
        File jlinkFile = getClassPathElement(JLinkInvoker.class);
        String modulesToAdd = properties.getProperty(MODULES_PROPERTY,
                "java.base,jdk.jdwp.agent,java.instrument,jdk.unsupported");
        String classPath = buildClassPath(properties);
        ProcessBuilder pb = new ProcessBuilder(
                jlinkBin,
                String.format("-J-D%s=%s", PACK_KEY, classPath),
                "-J-javaagent:" + jlinkFile,
                "-J--module-path=" + jlinkFile,
                "-J--add-modules=edu.columbia.cs.psl.jigsaw.phosphor.instrumenter",
                "-J--class-path=" + classPath,
                "--output=" + instJVMDir,
                "--phosphor-transformer=transform" + createPhosphorJLinkPluginArgument(properties),
                "--add-modules=" + modulesToAdd
        );
        try {
            System.out.println(String.join(" ", pb.command()));
            Process p = pb.inheritIO().start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String buildClassPath(Properties properties) {
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
                        String message = String.format("Failed to create %s class: %s", key, properties.getProperty(key));
                        throw new IllegalArgumentException(message, e);
                    }
                }
            }
        }
        if (Configuration.PRIOR_CLASS_VISITOR != null) {
            classes.add(Configuration.PRIOR_CLASS_VISITOR);
        }
        if (Configuration.POST_CLASS_VISITOR != null) {
            classes.add(Configuration.POST_CLASS_VISITOR);
        }
        if (Configuration.taintTagFactoryPackage != null) {
            classes.add(Configuration.taintTagFactory.getClass());
        }
        return classes.stream()
                .map(JLinkInvoker::getClassPathElement)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
    }

    /**
     * @param properties canonicalized properties that specify the Phosphor configuration options that should set in the
     *                   created argument
     * @return a String formatted for {@link PhosphorJLinkPlugin}'s arguments
     * String argument
     */
    public static String createPhosphorJLinkPluginArgument(Properties properties) {
        if (properties.isEmpty()) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder();
            Set<String> propNames = properties.stringPropertyNames();
            for (String propName : propNames) {
                if (propName.equals(MODULES_PROPERTY)) {
                    continue;
                }
                builder.append(':');
                builder.append(propName);
                builder.append('=').append(properties.getProperty(propName));
            }
            return builder.toString();
        }
    }

    public static File getClassPathElement(Class<?> clazz) {
        try {
            return new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError();
        }
    }
}
