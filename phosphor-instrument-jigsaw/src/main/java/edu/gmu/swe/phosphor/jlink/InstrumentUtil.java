package edu.gmu.swe.phosphor.jlink;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class InstrumentUtil {
    /**
     * MD5 MessageDigest instance.
     * <br>
     * Non-null.
     */
    private static final MessageDigest md5Inst;

    static {
        try {
            md5Inst = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private InstrumentUtil() {
        throw new AssertionError();
    }

    public static File javaHomeToBin(File javaHome) {
        return new File(javaHome, "bin");
    }

    public static File javaHomeToJavaExec(File javaHome) {
        return new File(javaHomeToBin(javaHome), "java");
    }

    public static File javaHomeToJLinkExec(File javaHome) {
        return new File(javaHomeToBin(javaHome), "jlink");
    }

    public static boolean isJavaHome(File directory) {
        return javaHomeToJavaExec(directory).isFile();
    }

    public static boolean isModularJvm(File javaHome) {
        return isJavaHome(javaHome) && javaHomeToJLinkExec(javaHome).isFile();
    }

    public static File getClassPathElement(Class<?> clazz) {
        return new File(
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    public static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return readAllBytes(in);
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        for (int len; (len = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, len);
        }
    }

    public static byte[] checksum(byte[] input) {
        return md5Inst.digest(input);
    }


    /**
     * Creates the specified directory if it does not already exist.
     *
     * @param dir the directory to create
     * @throws IOException if the specified directory did not already exist and was not successfully created
     */
    public static void ensureDirectory(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
    }
}