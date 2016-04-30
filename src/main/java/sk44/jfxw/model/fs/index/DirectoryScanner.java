/*
 *
 *
 *
 */
package sk44.jfxw.model.fs.index;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;

/**
 *
 * @author sk
 */
@AllArgsConstructor
public class DirectoryScanner extends SimpleFileVisitor<Path> {

    private final Consumer<Path> consumer;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        consumer.accept(dir.toAbsolutePath());
        return FileVisitResult.CONTINUE;
    }

}
