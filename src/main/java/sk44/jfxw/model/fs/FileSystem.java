/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import lombok.NonNull;
import sk44.jfxw.model.EventSource;
import sk44.jfxw.model.message.Message;

/**
 * ファイルシステム操作及びイベント通知を提供する。
 *
 * @author sk
 */
public class FileSystem {

    private static void assertTrue(boolean test) {
        if (test == false) {
            throw new IllegalArgumentException();
        }
    }

    private final EventSource<Consumer<Path>> pathCreated = new EventSource<>();
    private final EventSource<Consumer<Path>> fileDeleted = new EventSource<>();
    private final EventSource<Consumer<Path>> directoryDeleted = new EventSource<>();

    public void addPathCreated(Consumer<Path> listener) {
        pathCreated.addListener(listener);
    }

    public void addFileDeleted(Consumer<Path> listener) {
        fileDeleted.addListener(listener);
    }

    public void addDirectoryDeleted(Consumer<Path> listener) {
        directoryDeleted.addListener(listener);
    }

    public void deletePath(Path toDelete) {
        if (Files.isDirectory(toDelete)) {
            IOExceptions.unchecked(() -> Files.walkFileTree(toDelete, new DeleteDirectoryVisitor(this)));
        } else {
            deleteFile(toDelete);
        }

    }

    void deleteFile(Path toDelete) {
        IOExceptions.unchecked(() -> Files.delete(toDelete));
        Message.debug(toDelete.toString() + " deleted.");
        raiseFileRemoved(toDelete);
    }

    void deleteDir(Path toDelete) {
        if (Files.isDirectory(toDelete) == false) {
            throw new IllegalArgumentException(toDelete + " is not a directory.");
        }
        IOExceptions.unchecked(() -> Files.delete(toDelete));
        Message.debug("directory " + toDelete.toString() + " deleted.");
        raiseDirectoryRemoved(toDelete);
    }

    public boolean createDirectoryIfNotExists(Path dir) {
        // 同名のファイルが存在する場合もある
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            return false;
        }
        IOExceptions.unchecked(() -> Files.createDirectory(dir));
        railsePathCreated(dir);
        return true;
    }

    public boolean extractArchive(Path archiveFile, Path destDir) {
        try (java.nio.file.FileSystem fs = FileSystems.newFileSystem(archiveFile, ClassLoader.getSystemClassLoader())) {
            for (Path rootPath : fs.getRootDirectories()) {
                Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // TODO 展開処理
                        Message.info(file.toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            return true;
        } catch (IOException ex) {
            Message.error(ex);
            return false;
        }
    }

    public boolean movePath(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {

        if (Files.isDirectory(source)) {
            IOExceptions.unchecked(() -> {
                createDirectoryIfNotExists(dest);
                Files.walkFileTree(source, new MoveDirectoryVisitor(source,
                    dest, confirmer, this));
                Files.walkFileTree(source, new DeleteDirectoryVisitor(this));
                Message.info("moved: \n\t" + source.toString() + "\n\tto: \n\t" + dest.toString());
            });
            return true;
        }
        return moveFile(source, dest, confirmer);
    }

    boolean moveFile(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {
        assertTrue(Files.isDirectory(source) == false);
        assertTrue(Files.isDirectory(dest) == false);
        if (Files.exists(dest) == false
            || (confirmer != null && confirmer.confirm(dest.toString() + " is already exists. overwrite this?"))) {

            IOExceptions.unchecked(() -> Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING));
            raiseFileRemoved(source);
            railsePathCreated(dest);
            Message.info("moved: \n\t" + source.toString() + "\n\tto: \n\t" + dest.toString());
            return true;
        }
        return false;
    }

    private void railsePathCreated(Path created) {
        pathCreated.raiseEvent(listener -> listener.accept(created));
    }

    private void raiseFileRemoved(Path removedFile) {
        fileDeleted.raiseEvent(listener -> listener.accept(removedFile));
    }

    private void raiseDirectoryRemoved(Path removedDir) {
        directoryDeleted.raiseEvent(listener -> listener.accept(removedDir));
    }
}
