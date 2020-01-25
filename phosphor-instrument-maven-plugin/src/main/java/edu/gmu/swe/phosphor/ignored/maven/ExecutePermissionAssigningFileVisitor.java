package edu.gmu.swe.phosphor.ignored.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Traverses a file tree. Assigns execute permission to visited files.
 */
public class ExecutePermissionAssigningFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * Assigns execute permission to the specified file.
     * @param file {@inheritDoc}
     * @param attrs {@inheritDoc}
     * @return FileVisitResult#CONTINUE
     * @throws IOException if assigning execute permission fails
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(!file.toFile().setExecutable(true)) {
            throw new IOException("Failed to assign execute permission to: " + file);
        }
        return FileVisitResult.CONTINUE;
    }
}
