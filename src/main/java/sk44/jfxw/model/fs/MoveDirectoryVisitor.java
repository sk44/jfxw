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
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.AllArgsConstructor;
import sk44.jfxw.model.message.Message;

/**
 * FileVisitor implementation for moving directory.
 *
 * @author sk
 */
@AllArgsConstructor
public class MoveDirectoryVisitor extends SimpleFileVisitor<Path> {

    private final Path sourceDir;
    private final Path destDir;
    private final OverwriteFileConfirmer confirmer;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetDir = destDir.resolve(sourceDir.relativize(dir));
        if (PathHelper.isParentDir(sourceDir, targetDir)) {
            Message.warn("cannot move. " + sourceDir + " is a parent of " + targetDir);
            return FileVisitResult.TERMINATE;
        }
        if (PathHelper.createDirectoryIfNotExists(targetDir)) {
            Message.info(targetDir.toString() + " created.");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path newPath = destDir.resolve(sourceDir.relativize(file));
        // TODO リネームして移動のサポート
        if (Files.exists(newPath) == false
            || confirmer.confirm(newPath.toString() + " is already exists. overwrite it?")) {

            Files.move(file, newPath, StandardCopyOption.REPLACE_EXISTING);
            // TODO
            Message.info("moved " + file.toString() + " to " + newPath.toString());
        }
        return FileVisitResult.CONTINUE;
    }

}
