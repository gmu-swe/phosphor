package edu.columbia.cs.psl.phosphor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TransformationCache {


    private static final MessageDigest md5inst = initializeMessageDigest();
    private final File cacheDirectory;

    private TransformationCache(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public void store(String className, byte[] classFileBuffer, byte[] instrumentedBytes) {
        if (instrumentedBytes == null) {
            throw new NullPointerException();
        }
        try {
            if (md5inst != null) {
                String cacheKey = className.replace("/", ".");
                File file = new File(cacheDirectory, cacheKey + ".class");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(instrumentedBytes);
                }
                byte[] checksum;
                synchronized (md5inst) {
                    checksum = md5inst.digest(classFileBuffer);
                }
                file = new File(cacheDirectory, cacheKey + ".md5sum");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(checksum);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write a file to the transformation cache for class: " + className);
        }
    }

    public byte[] load(String className, byte[] classFileBuffer) {
        if (md5inst != null) {
            String cacheKey = className.replace("/", ".");
            File file = new File(cacheDirectory, cacheKey + ".md5sum");
            if (file.exists()) {
                try {
                    byte[] cachedDigest = new byte[1024];
                    int read;
                    try (FileInputStream fis = new FileInputStream(file)) {
                        if ((read = fis.read(cachedDigest)) <= 0) {
                            return null;
                        }
                    }
                    byte[] checksum;
                    synchronized (md5inst) {
                        checksum = md5inst.digest(classFileBuffer);
                    }
                    if (checksum.length != read) {
                        return null;
                    }
                    for (int i = 0; i < checksum.length; i++) {
                        if (checksum[i] != cachedDigest[i]) {
                            return null;
                        }
                    }
                    return Files.readAllBytes(new File(cacheDirectory, cacheKey + ".class").toPath());
                } catch (Throwable t) {
                    return null;
                }
            }
        }
        return null;
    }

    static TransformationCache getInstance(String cacheDirectoryPath) {
        if (cacheDirectoryPath == null) {
            return null;
        }
        File cacheDirectory = new File(cacheDirectoryPath);
        if (!cacheDirectory.exists()) {
            if (!cacheDirectory.mkdir()) {
                System.err.printf("Failed to create transformation cache directory: %s. " +
                                "Transformations are not being cached.%n",
                        cacheDirectoryPath);
                return null;
            }
        } else if (!cacheDirectory.isDirectory()) {
            System.err.printf("Cache directory already exists but is not a directory: %s. " +
                            "Transformations are not being cached.%n",
                    cacheDirectoryPath);
            return null;
        }
        if (!cacheDirectory.canWrite()) {
            System.err.printf("Cache directory is not writable: %s. " +
                            "Transformations are not being cached.%n",
                    cacheDirectoryPath);
            return null;
        }
        return new TransformationCache(cacheDirectory);
    }

    private static MessageDigest initializeMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
