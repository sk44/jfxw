/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    private final EventSource<Consumer<Path>> fileDeleted = new EventSource<>();
    private final EventSource<Consumer<Path>> directoryDeleted = new EventSource<>();

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

    public static boolean createDirectoryIfNotExists(Path dir) {
        // 同名のファイルが存在する場合もある
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            return false;
        }
        IOExceptions.unchecked(() -> Files.createDirectory(dir));
        return true;
    }

    public boolean movePath(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {

        if (Files.isDirectory(source)) {
            IOExceptions.unchecked(() -> {
                createDirectoryIfNotExists(dest);
                Files.walkFileTree(source, new MoveDirectoryVisitor(source,
                    dest, confirmer));
                Files.walkFileTree(source, new DeleteDirectoryVisitor(this));
                Message.info("moved: \n\t" + source.toString() + "\n\tto: \n\t" + dest.toString());
            });
            return true;
        }
        return moveFile(source, dest, confirmer);
    }

    private boolean moveFile(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {
        assertTrue(Files.isDirectory(source) == false);
        assertTrue(Files.isDirectory(dest) == false);
        if (Files.exists(dest) == false
            || (confirmer != null && confirmer.confirm(dest.toString() + " is already exists. overwrite this?"))) {

            IOExceptions.unchecked(() -> Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING));
            Message.info("moved: \n\t" + source.toString() + "\n\tto: \n\t" + dest.toString());
            return true;
        }
        return false;
    }

    private void raiseFileRemoved(Path removedFile) {
        fileDeleted.raiseEvent(listener -> listener.accept(removedFile));
    }

    private void raiseDirectoryRemoved(Path removedDir) {
        directoryDeleted.raiseEvent(listener -> listener.accept(removedDir));
    }
}
