/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FileVisitor implementation for calculating directory size.
 *
 * @author sk
 */
@RequiredArgsConstructor
public class CalculateDirectorySizeVisitor extends SimpleFileVisitor<Path> {

    private final Path targetDir;
    @Getter
    private long totalSize = 0;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (Files.isSymbolicLink(dir)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        if (Files.isSymbolicLink(file)) {
            return FileVisitResult.CONTINUE;
        }
        long size = Files.size(file);
        totalSize += size;

        return FileVisitResult.CONTINUE;
    }

}
