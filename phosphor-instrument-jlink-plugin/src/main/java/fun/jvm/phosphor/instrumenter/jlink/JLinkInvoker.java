package fun.jvm.phosphor.instrumenter.jlink;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Set;

public class JLinkInvoker {

    public static void invokeJLink(File jvmDir, File instJVMDir, Properties properties) {

        String jlinkBin = jvmDir + File.separator + "bin" + File.separator + "jlink";
        String classpath = System.getProperty("java.class.path");
        File jlinkFile = getPhosphorJLinkJarFile();

        ProcessBuilder pb = new ProcessBuilder(jlinkBin, "-J-javaagent:" + jlinkFile,
                "-J--module-path=" + jlinkFile,
                "-J--add-modules=fun.jvm.phosphor.instrumenter.jlink",
                "--output=" + instJVMDir,
                "--phosphor-transformer=transform" + createPhosphorJLinkPluginArgument(properties),
                "--add-modules=java.base,jdk.jdwp.agent,java.instrument,jdk.unsupported"
        );
        try {
            System.out.println(pb.command());
            Process p = pb.inheritIO().start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return a File object pointing to the JAR file for Phosphor-jlink bridge
     */
    public static File getPhosphorJLinkJarFile() {
        try {
            return new File(JLinkInvoker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError();
        }
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
                builder.append(':');
                builder.append(propName);
                builder.append('=').append(properties.getProperty(propName));
            }
            return builder.toString();
        }
    }
}
