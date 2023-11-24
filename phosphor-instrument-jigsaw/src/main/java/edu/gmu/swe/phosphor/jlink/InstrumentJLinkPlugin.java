package edu.gmu.swe.phosphor.jlink;

import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ModuleHashesAttribute;
import org.objectweb.asm.commons.ModuleResolutionAttribute;
import org.objectweb.asm.commons.ModuleTargetAttribute;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstrumentJLinkPlugin implements Plugin {
    private ClassFileTransformer transformer;
    private File coreJar;

    @Override
    public String getName() {
        return "phosphor-instrument";
    }

    @Override
    public Category getType() {
        return Category.FILTER;
    }

    @Override
    public String getDescription() {
        return "Applies instrumentation to the runtime image and packs core classes into the java.base module";
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        coreJar = new File(config.get("core"));
        try {
            transformer = (ClassFileTransformer) Class.forName(config.get("transformer"))
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to create transformer of type: " + config.get("transformer"), e);
        }
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        in.transformAndCopy(e -> transform(e, out), out);
        return out.build();
    }

    private ResourcePoolEntry transform(ResourcePoolEntry entry, ResourcePoolBuilder out) {
        if (entry.type().equals(ResourcePoolEntry.Type.CLASS_OR_RESOURCE)) {
            if (entry.path().endsWith("module-info.class") && entry.path().startsWith("/java.base")) {
                // Transform java.base's module-info.class file and pack core classes into java.base
                return entry.copyWithContent(transformBaseModuleInfo(entry.content(), out));
            } else if (entry.path().endsWith(".class")) {
                try {
                    byte[] instrumented = transformer.transform(null, null, null,
                            null, entry.contentBytes());
                    return instrumented == null ? entry : entry.copyWithContent(instrumented);
                } catch (IllegalClassFormatException e) {
                    return entry;
                }
            }
        }
        return entry;
    }

    private Set<String> packCore(ResourcePoolBuilder out) throws IOException {
        Set<String> packages = new HashSet<>();
        try (ZipFile zip = new ZipFile(coreJar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        out.add(ResourcePoolEntry.create("/java.base/" + entry.getName(), is.readAllBytes()));
                    }
                    packages.add(entry.getName().substring(0, entry.getName().lastIndexOf('/')));
                }
            }
        }
        return packages;
    }

    private byte[] transformBaseModuleInfo(InputStream in, ResourcePoolBuilder out) {
        try {
            Set<String> packages = packCore(out);
            ClassNode classNode = new ClassNode();
            ClassReader cr = new ClassReader(in);
            Attribute[] attributes = new Attribute[]{new ModuleTargetAttribute(),
                    new ModuleResolutionAttribute(),
                    new ModuleHashesAttribute()};
            cr.accept(classNode, attributes, 0);
            // Add exports
            for (String packageName : packages) {
                classNode.module.exports.add(new ModuleExportNode(packageName, 0, null));
            }
            // Add packages
            classNode.module.packages.addAll(packages);
            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            return cw.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}