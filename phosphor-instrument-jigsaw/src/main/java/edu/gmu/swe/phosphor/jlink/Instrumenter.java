package edu.gmu.swe.phosphor.jlink;

import org.jacoco.core.internal.InputStreams;
import org.jacoco.core.internal.instr.SignatureRemover;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public final class Instrumenter {
    private final SignatureRemover signatureRemover = new SignatureRemover();
    private final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
    private final Instrumentation instrumentation;

    public Instrumenter(Instrumentation instrumentation) {
        if (instrumentation == null) {
            throw new NullPointerException();
        }
        this.instrumentation = instrumentation;
    }

    public void process(File source, File target) throws IOException, InterruptedException, ExecutionException {
        if (!source.exists()) {
            throw new IllegalArgumentException("Source file not found: " + source);
        } else if (!source.isDirectory() && !isClass(source.getName()) && !isArchive(source.getName())) {
            throw new IllegalArgumentException("Unknown source file type: " + source);
        }
        Queue<Future<Void>> futures = new LinkedList<>();
        processFile(futures, source, target);
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

    private void instrumentClass(File source, File target) {
        try (InputStream input = Files.newInputStream(source.toPath());
                OutputStream output = Files.newOutputStream(target.toPath())) {
            instrumentClass(input, output);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    private void instrumentClass(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = InstrumentUtil.readAllBytes(input);
        byte[] result = instrumentation.apply(buffer);
        output.write(result == null ? buffer : result);
    }

    private void processFile(Collection<Future<Void>> futures, File source, File target)
            throws IOException, InterruptedException {
        if (source.isDirectory()) {
            InstrumentUtil.ensureDirectory(target);
            for (File child : Objects.requireNonNull(source.listFiles())) {
                processFile(futures, child, new File(target, child.getName()));
            }
        } else if (isClass(source.getName())) {
            futures.add(executor.submit(() -> instrumentClass(source, target), null));
        } else if (isArchive(source.getName())) {
            processZip(Files.newInputStream(source.toPath()), Files.newOutputStream(target.toPath()));
        } else {
            if (copy(source, target)) {
                if (source.canExecute() && !target.setExecutable(true)) {
                    errors.add(new IOException("Failed to set permissions for: " + target));
                }
                if (source.canRead() && !target.setReadable(true)) {
                    errors.add(new IOException("Failed to set permissions for: " + target));
                }
                if (source.canWrite() && !target.setWritable(true)) {
                    errors.add(new IOException("Failed to set permissions for: " + target));
                }
            }
        }
    }

    private void processZip(InputStream in, OutputStream out) throws IOException, InterruptedException {
        try {
            List<Future<ZipResult>> futures = new LinkedList<>();
            try (ZipInputStream zin = new ZipInputStream(in)) {
                for (ZipEntry entry; (entry = zin.getNextEntry()) != null; ) {
                    ZipEntry finalEntry = entry;
                    if (entry.isDirectory()) {
                        futures.add(executor.submit(() -> new ZipResult(finalEntry, null)));
                    } else if (!signatureRemover.removeEntry(entry.getName())) {
                        byte[] buffer = InputStreams.readFully(zin);
                        futures.add(executor.submit(() -> new ZipResult(finalEntry, buffer)));
                    }
                }
            }
            writeZipResults(out, futures);
        } catch (IOException e) {
            errors.add(e);
            InstrumentUtil.copy(in, out);
        }
    }

    private void writeZipResults(OutputStream out, List<Future<ZipResult>> futures)
            throws IOException, InterruptedException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Future<ZipResult> future : futures) {
                try {
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
                } catch (ExecutionException | ZipException e) {
                    errors.add(e);
                }
            }
            zos.finish();
        }
    }

    private boolean copy(File source, File target) {
        try (InputStream in = Files.newInputStream(source.toPath());
                OutputStream out = Files.newOutputStream(target.toPath())) {
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

        public ZipResult(ZipEntry entry, byte[] buffer) throws IOException, InterruptedException {
            this.entry = entry;
            byte[] tempBuffer = new byte[0];
            if (buffer != null) {
                tempBuffer = buffer;
                try {
                    ByteArrayInputStream in = new ByteArrayInputStream(buffer);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (entry.getName().endsWith(".class")) {
                        instrumentClass(in, out);
                    } else if (entry.getName().endsWith(".jar")) {
                        processZip(in, out);
                    } else if (!signatureRemover.filterEntry(entry.getName(), in, out)) {
                        InstrumentUtil.copy(in, out);
                    }
                    tempBuffer = out.toByteArray();
                } catch (IOException | RuntimeException e) {
                    errors.add(e);
                }
            }
            this.buffer = tempBuffer;
        }
    }
}