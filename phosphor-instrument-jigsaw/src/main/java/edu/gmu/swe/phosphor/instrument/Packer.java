package edu.gmu.swe.phosphor.instrument;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Packer {
    private final Instrumentation instrumentation;
    private final Patcher patcher;

    public Packer(ResourcePool in, Instrumentation instrumentation) {
        if (in == null || instrumentation == null) {
            throw new NullPointerException();
        }
        this.instrumentation = instrumentation;
        this.patcher = instrumentation.createPatcher(path -> Packer.findEntry(in, path));
    }

    public ResourcePoolEntry pack(ResourcePoolEntry entry, ResourcePoolBuilder out) {
        try {
            // Pack classes into the java.bas
            Set<String> packages = packClasses(out);
            // Transform java.base's module-info.class file
            try (InputStream in = entry.content()) {
                return entry.copyWithContent(transformBaseModuleInfo(in, packages));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> packClasses(ResourcePoolBuilder out) throws IOException {
        // Pack the JARs and directories into the resource pool
        // Return the set of packages for packed classes
        Set<String> packages = new HashSet<>();
        for (File element : instrumentation.getElementsToPack()) {
            if (element.isDirectory()) {
                packDirectory(out, element, packages);
            } else {
                packJar(out, element, packages);
            }
        }
        return packages;
    }

    private void packClass(ResourcePoolBuilder out, String name, File classFile, Set<String> packages) {
        try {
            if (instrumentation.shouldPack(name)) {
                byte[] content = patcher.patch(name, Files.readAllBytes(classFile.toPath()));
                out.add(ResourcePoolEntry.create("/java.base/" + name, content));
                packages.add(name.substring(0, name.lastIndexOf('/')));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to pack: " + name, e);
        }
    }

    private void packDirectory(ResourcePoolBuilder out, File directory, Set<String> packages) throws IOException {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            walk.filter(Files::isRegularFile)
                    .forEach(p -> packClass(
                            out,
                            directory.toPath().relativize(p).toFile().getPath(),
                            p.toAbsolutePath().toFile(),
                            packages));
        }
    }

    private void packJar(ResourcePoolBuilder out, File element, Set<String> packages) throws IOException {
        try (ZipFile zip = new ZipFile(element)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (instrumentation.shouldPack(entry.getName())) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        byte[] content = patcher.patch(entry.getName(), InstrumentUtil.readAllBytes(is));
                        out.add(ResourcePoolEntry.create("/java.base/" + entry.getName(), content));
                    }
                    packages.add(entry.getName().substring(0, entry.getName().lastIndexOf('/')));
                }
            }
        }
    }

    private byte[] transformBaseModuleInfo(InputStream in, Set<String> packages) {
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
