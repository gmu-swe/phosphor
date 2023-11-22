package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

import java.io.File;
import java.util.*;


public class PhosphorJLinkPlugin implements Plugin {
    public static final String NAME = "phosphor-transformer";
    private Set<File> elementsToPack;

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public Category getType() {
        return Category.ADDER;
    }

    /**
     * @param properties canonicalized properties that specify the Phosphor configuration options that should set in the
     *                   created arguments
     * @return an array formatted for {@link Instrumenter#main(String[])} Instrumenter.main's} String[] argument
     */
    public static String[] createPhosphorMainArguments(Map<String, String> properties) {
        LinkedList<String> arguments = new LinkedList<>();
        Set<String> propNames = properties.keySet();
        for (String propName : propNames) {
            if (propName.equals("phosphor-transformer")) {
                continue;
            }
            arguments.addLast("-" + propName);
            if (!"true".equals(properties.get(propName))) {
                arguments.addLast(properties.get(propName));
            }
        }
        arguments.addLast("ignored");//for input dir
        arguments.addLast("ignored");//for output dir
        return arguments.toArray(new String[0]);
    }

    @Override
    public void configure(Map<String, String> config) {
        Configuration.IS_JAVA_8 = false;
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        TaintUtils.VERIFY_CLASS_GENERATION = true;
        PhosphorOption.configure(false, createPhosphorMainArguments(config));
        File phosphorJar = JLinkInvoker.getClassPathElement(Instrumenter.class);
        System.out.println("Embedding Phosphor from: " + phosphorJar);
        String pack = System.getProperty(JLinkInvoker.PACK_KEY);
        this.elementsToPack = new HashSet<>();
        elementsToPack.add(phosphorJar);
        if (pack != null && !pack.isEmpty()) {
            Arrays.stream(pack.split(File.pathSeparator))
                    .map(File::new)
                    .forEach(elementsToPack::add);
        }
        //TODO process args
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        TaintUtils.VERIFY_CLASS_GENERATION = true;
        PreMain.RUNTIME_INST = false;
        PhosphorPacker packer = new PhosphorPacker(in, elementsToPack);
        in.transformAndCopy((resourcePoolEntry) -> {
            if (resourcePoolEntry.type().equals(ResourcePoolEntry.Type.CLASS_OR_RESOURCE)) {
                if (resourcePoolEntry.path().endsWith(".class")) {
                    if (resourcePoolEntry.path().endsWith("module-info.class")) {
                        //if this is the module for java-base, hack it to export phosphor, and pack phosphor into the module
                        if (resourcePoolEntry.path().startsWith("/java.base")) {
                            //This is the java.base module-info.class file. Transform it, and then add in phosphor
                            resourcePoolEntry = packer.pack(resourcePoolEntry, out);
                        }
                    } else {
                        byte[] newContent = Instrumenter.instrumentClass(resourcePoolEntry.path(),
                                resourcePoolEntry.content(), true);
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
}


