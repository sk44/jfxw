/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.AllArgsConstructor;

/**
 *
 * @author sk
 */
@AllArgsConstructor
public class DeleteDirectoryVisitor extends SimpleFileVisitor<Path> {

    private final FileSystem fileSystem;

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        fileSystem.deleteDir(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        fileSystem.deleteFile(file);
        return FileVisitResult.CONTINUE;
    }

}
