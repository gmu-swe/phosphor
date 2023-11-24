package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.OptionsUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class JLinkInvoker {
    public static final String MODULES_PROPERTY = "jvmModules";
    public static final String PACK_KEY = "phosphor.pack";

    public static void invokeJLink(File jvmDir, File instJVMDir, Properties properties) {
        String jlinkBin = jvmDir + File.separator + "bin" + File.separator + "jlink";
        File jlinkFile = getClassPathElement(JLinkInvoker.class);
        String modulesToAdd =
                properties.getProperty(MODULES_PROPERTY, "java.base,jdk.jdwp.agent,java.instrument,jdk.unsupported");
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
                "--add-modules=" + modulesToAdd);
        try {
            System.out.println(String.join(" ", pb.command()));
            Process p = pb.inheritIO().start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String buildClassPath(Properties properties) {
        return OptionsUtil.getConfigurationClasses(properties).stream()
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
            return new File(
                    clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError();
        }
    }
}
