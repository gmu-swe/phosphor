package edu.columbia.cs.psl.phosphor.driver;

import org.jacoco.core.internal.InputStreams;
import org.jacoco.core.internal.instr.SignatureRemover;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class Instrumenter {
    private final SignatureRemover signatureRemover = new SignatureRemover();
    private final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
    private final Instrumentation instrumentation;
    private final AtomicInteger count = new AtomicInteger(0);
    private final boolean verbose;
    private final Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();

    public Instrumenter(Instrumentation instrumentation, boolean verbose) {
        if (instrumentation == null) {
            throw new NullPointerException();
        }
        this.instrumentation = instrumentation;
        this.verbose = verbose;
    }

    public void process(File source, File destination) throws IOException, InterruptedException, ExecutionException {
        if (!source.exists()) {
            throw new IllegalArgumentException("Source file not found: " + source);
        } else if (!source.isDirectory() && !isClass(source.getName()) && !isArchive(source.getName())) {
            throw new IllegalArgumentException("Unknown source file type: " + source);
        }
        collectFiles(source, destination);
        while (!futures.isEmpty()) {
            futures.poll().get();
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
            if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                break;
            }
        }
        if (!errors.isEmpty()) {
            for (Throwable error : errors) {
                error.printStackTrace();
            }
        }
    }

    private void instrumentClass(File source, File destination) {
        try (InputStream input = Files.newInputStream(source.toPath());
                OutputStream output = Files.newOutputStream(destination.toPath())) {
            instrumentClass(InstrumentUtil.readAllBytes(input), output);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    private void instrumentClass(byte[] classFileBuffer, OutputStream output) throws IOException {
        byte[] result = instrumentation.apply(classFileBuffer);
        output.write(result == null ? classFileBuffer : result);
        int n;
        if ((n = count.incrementAndGet()) % 1000 == 0 && verbose) {
            System.out.println("Processed: " + n);
        }
    }

    private void collectFiles(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            InstrumentUtil.ensureDirectory(destination);
            for (File child : Objects.requireNonNull(source.listFiles())) {
                collectFiles(child, new File(destination, child.getName()));
            }
        } else if (isClass(source.getName())) {
            futures.add(executor.submit(() -> instrumentClass(source, destination), null));
        } else if (isArchive(source.getName())) {
            byte[] buffer = InstrumentUtil.readAllBytes(source);
            OutputStream out = Files.newOutputStream(destination.toPath());
            futures.add(executor.submit(() -> processZip(buffer, out), null));
        } else {
            if (copy(source, destination)) {
                if (source.canExecute() && !destination.setExecutable(true)) {
                    errors.add(new IOException("Failed to set execute permission for: " + destination));
                }
                if (source.canRead() && !destination.setReadable(true)) {
                    errors.add(new IOException("Failed to set read permission for: " + destination));
                }
                if (source.canWrite() && !destination.setWritable(true)) {
                    errors.add(new IOException("Failed to set write permission for: " + destination));
                }
            }
        }
    }

    private void finishArchive(List<Future<ZipResult>> entries, OutputStream out) {
        for (Future<ZipResult> future : entries) {
            // Submit a new task to finish this archive to prevent blocking
            if (!future.isDone()) {
                executor.submit(() -> finishArchive(entries, out));
                return;
            }
        }
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Future<ZipResult> future : entries) {
                ZipResult result = future.get();
                ZipEntry entry = result.entry;
                ZipEntry outEntry = new ZipEntry(entry.getName());
                outEntry.setMethod(entry.getMethod());
                if (entry.getMethod() == ZipEntry.STORED) {
                    // Uncompressed entries require entry size and CRC
                    outEntry.setSize(result.buffer.length);
                    outEntry.setCompressedSize(result.buffer.length);
                    CRC32 crc = new CRC32();
                    crc.update(result.buffer, 0, result.buffer.length);
                    outEntry.setCrc(crc.getValue());
                }
                zos.putNextEntry(outEntry);
                zos.write(result.buffer);
                zos.closeEntry();
            }
            zos.finish();
        } catch (ExecutionException | InterruptedException | IOException e) {
            errors.add(e);
        }
    }

    private List<Future<ZipResult>> submitZipEntries(byte[] in) throws IOException {
        List<Future<ZipResult>> entries = new LinkedList<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(in))) {
            for (ZipEntry entry; (entry = zin.getNextEntry()) != null; ) {
                ZipEntry finalEntry = entry;
                if (entry.isDirectory()) {
                    entries.add(executor.submit(() -> new ZipResult(finalEntry, null)));
                } else if (!signatureRemover.removeEntry(entry.getName())) {
                    byte[] buffer = InputStreams.readFully(zin);
                    entries.add(executor.submit(() -> new ZipResult(finalEntry, buffer)));
                }
            }
        }
        futures.addAll(entries);
        return entries;
    }

    private void processZip(byte[] in, OutputStream out) {
        try {
            List<Future<ZipResult>> entries = submitZipEntries(in);
            finishArchive(entries, out);
        } catch (IOException e) {
            errors.add(e);
        }
    }

    private boolean copy(File source, File destination) {
        try (InputStream in = Files.newInputStream(source.toPath());
                OutputStream out = Files.newOutputStream(destination.toPath())) {
            InstrumentUtil.copy(in, out);
            return true;
        } catch (IOException e) {
            errors.add(e);
            return false;
        }
    }

    private static boolean isArchive(String name) {
        return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".zip") || name.endsWith(".jmod");
    }

    private static boolean isClass(String name) {
        return name.endsWith(".class");
    }

    private class ZipResult {
        private final ZipEntry entry;
        private final byte[] buffer;

        public ZipResult(ZipEntry entry, byte[] buffer) {
            this.entry = entry;
            byte[] tempBuffer = new byte[0];
            if (buffer != null) {
                tempBuffer = buffer;
                try {
                    ByteArrayInputStream in = new ByteArrayInputStream(buffer);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (entry.getName().endsWith(".class")) {
                        instrumentClass(buffer, out);
                    } else if (entry.getName().endsWith(".jar")) {
                        processZip(buffer, out);
                    } else if (!signatureRemover.filterEntry(entry.getName(), in, out)) {
                        out.write(buffer);
                    }
                    tempBuffer = out.toByteArray();
                } catch (IOException | RuntimeException e) {
                    errors.add(e);
                }
            }
            this.buffer = tempBuffer;
        }
    }

    public static long instrument(
            File source,
            File destination,
            Properties options,
            Instrumentation instrumentation,
            boolean verbose,
            String modules)
            throws IOException {
        if (destination.exists()) {
            throw new IllegalArgumentException("Destination location for instrumentation already exists.");
        }
        if (!source.exists()) {
            throw new IllegalArgumentException("Source location not found: " + source);
        }
        long startTime = System.currentTimeMillis();
        try {
            if (InstrumentUtil.isModularJvm(source)) {
                JLinkInvoker.invoke(source, destination, instrumentation, options, modules);
            } else {
                new Instrumenter(instrumentation, verbose).process(source, destination);
            }
            return System.currentTimeMillis() - startTime;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IOException("Failed to instrument source location", e);
        }
    }
}