package edu.columbia.cs.psl.phosphor.driver;

import edu.columbia.cs.psl.phosphor.agent.InstrumentUtil;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class ResourcePoolPacker extends Packer {
    private final ResourcePoolBuilder out;

    public ResourcePoolPacker(Instrumentation instrumentation, ResourcePool pool, ResourcePoolBuilder out) {
        super(instrumentation, path -> ResourcePoolPacker.findEntry(pool, path));
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
    }

    @Override
    public void pack(String name, byte[] content) {
        out.add(ResourcePoolEntry.create("/java.base/" + name, content));
    }

    public ResourcePoolEntry pack(ResourcePoolEntry entry) {
        try {
            // Pack classes into java.base
            Set<String> packages = pack();
            // Transform java.base's module-info.class file
            try (InputStream in = entry.content()) {
                return entry.copyWithContent(transformBaseModuleInfo(in, packages));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] transformBaseModuleInfo(InputStream in, Set<String> packages) {
        try {
            ClassNode classNode = new ClassNode();
            ClassReader cr = new ClassReader(in);
            Attribute[] attributes = new Attribute[] {
                new ModuleTargetAttribute(), new ModuleResolutionAttribute(), new ModuleHashesAttribute()
            };
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

    private static byte[] findEntry(ResourcePool pool, String path) {
        ResourcePoolEntry entry = pool.findEntry(path)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find entry for: " + path));
        try (InputStream in = entry.content()) {
            return InstrumentUtil.readAllBytes(in);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read entry for: " + path, e);
        }
    }
}
