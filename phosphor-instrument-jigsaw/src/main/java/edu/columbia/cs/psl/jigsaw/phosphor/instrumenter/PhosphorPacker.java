package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.PhosphorPatcher;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class PhosphorPacker {
    private final File phosphorJar;
    private final PhosphorPatcher patcher;

    PhosphorPacker(ResourcePool pool, File phosphorJar) {
        if (phosphorJar == null) {
            throw new NullPointerException();
        }
        this.phosphorJar = phosphorJar;
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
        // Pack the Phosphor JAR into the resource pool
        // Return the set of packages for packed classes
        Set<String> packages = new HashSet<>();
        try (ZipFile zip = new ZipFile(phosphorJar)) {
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
        return packages;
    }
}
