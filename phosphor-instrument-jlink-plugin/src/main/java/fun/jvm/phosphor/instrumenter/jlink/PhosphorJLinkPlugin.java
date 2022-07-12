package fun.jvm.phosphor.instrumenter.jlink;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.PreMain;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PhosphorJLinkPlugin implements Plugin {
    public static final String NAME = "phosphor-transformer";
    private static File phosphorJar;

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public Category getType() {
        return Category.ADDER;
    }

    /**
     * @param jvmDir     the source directory where the JDK or JRE is installed
     * @param instJVMDir the target directory where the Phosphor-instrumented JVM should be created
     * @param properties canonicalized properties that specify the Phosphor configuration options that should set in the
     *                   created arguments
     * @return an array formatted for {@link Instrumenter#main(String[])} Instrumenter.main's} String[] argument
     */
    public static String[] createPhosphorMainArguments(Map<String,String> properties) {
        LinkedList<String> arguments = new LinkedList<>();
        Set<String> propNames = properties.keySet();
        for(String propName : propNames) {
            if(propName.equals("phosphor-transformer")){
                continue;
            }
            arguments.addLast("-" + propName);
            if(!"true".equals(properties.get(propName))) {
                arguments.addLast(properties.get(propName));
            }
        }
        arguments.addLast("ignored");//for input dir
        arguments.addLast("ignored");//for output dir
        return arguments.toArray(new String[0]);
    }

    @Override
    public void configure(Map<String, String> config) {
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        TaintUtils.VERIFY_CLASS_GENERATION = true;
        PhosphorOption.configure(false, createPhosphorMainArguments(config));
        phosphorJar = getPhosphorJarFile();
        System.out.println("Embedding Phosphor from: " + phosphorJar);
        //TODO process args
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        TaintUtils.VERIFY_CLASS_GENERATION = true;
        PreMain.RUNTIME_INST = false;
        in.transformAndCopy((resourcePoolEntry) -> {
            if (resourcePoolEntry.type().equals(ResourcePoolEntry.Type.CLASS_OR_RESOURCE)) {
                if (resourcePoolEntry.path().endsWith(".class")) {
                    if (resourcePoolEntry.path().endsWith("module-info.class")) {
                        //if this is the module for java-base, hack it to export phosphor, and pack phosphor into the module
                        if (resourcePoolEntry.path().startsWith("/java.base")) {
                            //This is the java.base module-info.class file. Transform it, and then add in phosphor
                            try {
                                ZipFile phosphorZip = new ZipFile(phosphorJar);
                                Enumeration<? extends ZipEntry> phosphorEntries = phosphorZip.entries();
                                HashSet<String> packages = new HashSet<>();
                                while (phosphorEntries.hasMoreElements()) {
                                    ZipEntry pe = phosphorEntries.nextElement();
                                    if (pe.getName().startsWith("fun/jvm/phosphor/instrumenter/jlink")
                                            || pe.getName().endsWith("module-info.class")
                                            || pe.getName().startsWith("edu/columbia/cs/psl/phosphor/runtime/jdk/unsupported")) {
                                        continue;
                                    } else if (pe.getName().endsWith(".class")) {
                                        InputStream is = phosphorZip.getInputStream(pe);
                                        out.add(ResourcePoolEntry.create("/java.base/" + pe.getName(), is.readAllBytes()));
                                        is.close();

                                        //package name
                                        packages.add(pe.getName().substring(0, pe.getName().lastIndexOf('/')));
                                    }
                                }
                                phosphorZip.close();

                                byte[] newContent = Instrumenter.transformJavaBaseModuleInfo(resourcePoolEntry.content(), packages);
                                if (newContent != null) {
                                    resourcePoolEntry = resourcePoolEntry.copyWithContent(newContent);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if(resourcePoolEntry.path().startsWith("/jdk.unsupported")){
                            //This is the jdk.unsupported module-info.class file. Transform it, and then add in phosphor
                            try {
                                ZipFile phosphorZip = new ZipFile(phosphorJar);
                                Enumeration<? extends ZipEntry> phosphorEntries = phosphorZip.entries();
                                HashSet<String> packages = new HashSet<>();
                                while (phosphorEntries.hasMoreElements()) {
                                    ZipEntry pe = phosphorEntries.nextElement();
                                    if(pe.getName().startsWith("edu/columbia/cs/psl/phosphor/runtime/jdk/unsupported") && pe.getName().endsWith(".class")){
                                        InputStream is = phosphorZip.getInputStream(pe);
                                        out.add(ResourcePoolEntry.create("/jdk.unsupported/" + pe.getName(), is.readAllBytes()));
                                        is.close();

                                        //package name
                                        packages.add(pe.getName().substring(0, pe.getName().lastIndexOf('/')));
                                    }
                                }
                                phosphorZip.close();

                                byte[] newContent = Instrumenter.transformJDKUnsupportedModuleInfo(resourcePoolEntry.content(), packages);
                                if (newContent != null) {
                                    resourcePoolEntry = resourcePoolEntry.copyWithContent(newContent);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else {
                        byte[] newContent = Instrumenter.instrumentClass(resourcePoolEntry.path(), resourcePoolEntry.content(), true);
                        if (newContent != null) {
                            resourcePoolEntry = resourcePoolEntry.copyWithContent(newContent);
                        }
                    }
                }
            }
            return resourcePoolEntry;
        }, out);
        return out.build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Transforms the runtime image to be compatible with Phosphor";
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


}


