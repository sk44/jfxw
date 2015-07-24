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
 *
 * @author sk
 */
@AllArgsConstructor
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {

    @FunctionalInterface
    public interface OverwriteConfirmer {

        boolean confirm(String message);
    }

    private final Path sourceDir;
    private final Path destDir;
    private final OverwriteConfirmer confirmer;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetDir = destDir.resolve(sourceDir.relativize(dir));
        // TODO 循環すると死ぬ
        if (Files.exists(targetDir) == false || Files.isDirectory(targetDir) == false) {
            Files.createDirectory(targetDir);
            Message.info(targetDir.toString() + " created.");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path newPath = destDir.resolve(sourceDir.relativize(file));
        if (Files.exists(newPath) == false
            || confirmer.confirm(newPath.toString() + " is already exists. overwrite it?")) {

            Files.copy(file, newPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            Message.info("copied " + file.toString() + " to " + newPath.toString());
        }
        return FileVisitResult.CONTINUE;
    }

}
