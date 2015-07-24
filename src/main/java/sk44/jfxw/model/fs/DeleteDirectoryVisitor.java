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
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class DeleteDirectoryVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        Files.delete(dir);
        Message.info(dir.toString() + " deleted.");
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        Message.info(file.toString() + " deleted.");
        return FileVisitResult.CONTINUE;
    }

}
