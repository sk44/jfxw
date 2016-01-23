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
import sk44.jfxw.model.message.Message;

/**
 * FileVisitor implementation for copying directory.
 *
 * @author sk
 */
@AllArgsConstructor
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {

    private final Path sourceDir;
    private final Path destDir;
    private final OverwriteFileConfirmer confirmer;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetDir = destDir.resolve(sourceDir.relativize(dir));
        if (PathHelper.isParentDir(sourceDir, targetDir)) {
            Message.warn("cannot copy. " + sourceDir + " is a parent of " + targetDir);
            return FileVisitResult.TERMINATE;
        }
        if (PathHelper.createDirectoryIfNotExists(targetDir)) {
            Message.debug(targetDir.toString() + " created.");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path newPath = destDir.resolve(sourceDir.relativize(file));
        PathHelper.copyFile(file, newPath, confirmer);
        return FileVisitResult.CONTINUE;
    }

}
