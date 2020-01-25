package edu.gmu.swe.phosphor.ignored.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Deletes a file tree.
 */
public class DeletingFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * Deletes the specified file.
     * @param file {@inheritDoc}
     * @param attrs {@inheritDoc}
     * @return FileVisitResult#CONTINUE
     * @throws IOException if an I/O error occurs
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Deletes the specified directory.
     * @param dir {@inheritDoc}
     * @param e {@inheritDoc}
     * @return FileVisitResult#CONTINUE
     * @throws IOException if an I/O error occurred with traversing dir
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
        if(e == null) {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        } else {
            throw e;
        }
    }
}