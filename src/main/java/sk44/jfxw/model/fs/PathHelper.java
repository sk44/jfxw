/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.NonNull;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class PathHelper {

    // TODO なんか適切なクラスを考える
    public static boolean isParentDir(Path parentDir, Path childDir) {

        if (Files.isDirectory(parentDir) == false) {
            return false;
        }

        return childDir.startsWith(parentDir);
    }

    public static boolean createDirectoryIfNotExists(Path dir) throws IOException {
        // 同名のファイルが存在する場合もある
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            return false;
        }
        Files.createDirectory(dir);
        return true;
    }

    private static void assertTrue(boolean test) {
        if (test == false) {
            throw new IllegalArgumentException();
        }
    }

    public static void copyPath(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {

        if (Files.isDirectory(source)) {
            try {
                createDirectoryIfNotExists(dest);
                Files.walkFileTree(source, new CopyDirectoryVisitor(source,
                    dest, confirmer));
            } catch (IOException ex) {
                Message.error(ex);
                throw new UncheckedIOException(ex);
            }
            Message.info("copied: " + source.toString() + "\n\tto: " + dest.toString());
            return;
        }
        copyFile(source, dest, confirmer);
    }

    public static void copyFile(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {
        assertTrue(Files.isDirectory(source) == false);
        assertTrue(Files.isDirectory(dest) == false);
        if (Files.exists(dest) == false
            || (confirmer != null && confirmer.confirm(dest.toString() + " is already exists. overwrite this?"))) {
            try {
                // TODO リネームしてコピーのサポート
                Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                Message.info("copied: " + source.toString() + "\n\tto: " + dest.toString());
            } catch (IOException ex) {
                Message.error(ex);
                throw new UncheckedIOException(ex);
            }
        }
    }

    public static void movePath(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {

        if (Files.isDirectory(source)) {
            try {
                createDirectoryIfNotExists(dest);
                Files.walkFileTree(source, new MoveDirectoryVisitor(source,
                    dest, confirmer));
                Files.walkFileTree(source, new DeleteDirectoryVisitor());
            } catch (IOException ex) {
                Message.error(ex);
                throw new UncheckedIOException(ex);
            }
            Message.info("moved: " + source.toString() + "\n\tto: " + dest.toString());
            return;
        }
        moveFile(source, dest, confirmer);
    }

    public static void moveFile(@NonNull Path source, @NonNull Path dest, OverwriteFileConfirmer confirmer) {
        assertTrue(Files.isDirectory(source) == false);
        assertTrue(Files.isDirectory(dest) == false);
        if (Files.exists(dest) == false
            || (confirmer != null && confirmer.confirm(dest.toString() + " is already exists. overwrite this?"))) {
            try {
                Files.move(source, dest);
                Message.info("moved: " + source.toString() + "\n\tto: " + dest.toString());
            } catch (IOException ex) {
                Message.error(ex);
                throw new UncheckedIOException(ex);
            }
        }

    }
}
