package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.PhosphorPatcher;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class PhosphorPacker {
    private final PhosphorPatcher patcher;
    private final Set<File> elementsToPack;

    PhosphorPacker(ResourcePool pool, Set<File> elementsToPack) {
        this.elementsToPack = Collections.unmodifiableSet(new HashSet<>(elementsToPack));
        ResourcePoolEntry entry = pool.findEntry("/java.base/jdk/internal/misc/Unsafe.class")
                .orElseThrow(() -> new IllegalArgumentException("Unable to find entry for jdk/internal/misc/Unsafe"));
        try (InputStream in = entry.content()) {
            this.patcher = new PhosphorPatcher(in);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read entry for jdk/internal/misc/Unsafe", e);
        }
    }

    private boolean shouldInclude(String name) {
        return !name.startsWith("edu/columbia/cs/psl/jigsaw/phosphor/instrumenter")
                && !name.endsWith("module-info.class")
                && !name.startsWith("org/")
                && !name.startsWith("edu/columbia/cs/psl/phosphor/runtime/jdk/unsupported")
                && name.endsWith(".class");
    }

    ResourcePoolEntry pack(ResourcePoolEntry entry, ResourcePoolBuilder out) {
        // Transform java.base's module-info.class file and pack Phosphor classes into java.base
        try {
            Set<String> packages = packClasses(out);
            try (InputStream in = entry.content()) {
                return entry.copyWithContent(patcher.transformBaseModuleInfo(in, packages));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> packClasses(ResourcePoolBuilder out) throws IOException {
        // Pack the JARs and directories into the resource pool
        // Return the set of packages for packed classes
        Set<String> packages = new HashSet<>();
        for (File element : elementsToPack) {
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
            if (shouldInclude(name)) {
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
                    .forEach(p -> packClass(out, directory.toPath().relativize(p).toFile().getPath(),
                            p.toAbsolutePath().toFile(),
                            packages));
        }
    }

    private void packJar(ResourcePoolBuilder out, File element, Set<String> packages) throws IOException {
        try (ZipFile zip = new ZipFile(element)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (shouldInclude(entry.getName())) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        byte[] content = patcher.patch(entry.getName(), is.readAllBytes());
                        out.add(ResourcePoolEntry.create("/java.base/" + entry.getName(), content));
                    }
                    packages.add(entry.getName().substring(0, entry.getName().lastIndexOf('/')));
                }
            }
        }
    }
}
