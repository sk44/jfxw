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
@Deprecated
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

    public static boolean copyPath(@NonNull Path source, @NonNull Path dest, @NonNull OverwriteFileConfirmer confirmer) {

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
            return true;
        }
        return copyFile(source, dest, confirmer);
    }

    public static boolean copyFile(@NonNull Path source, @NonNull Path dest, @NonNull OverwriteFileConfirmer confirmer) {
        if (source.equals(dest)) {
            Message.warn("cannot copy to same path.");
            return false;
        }
        assertTrue(Files.isDirectory(source) == false);
        assertTrue(Files.isDirectory(dest) == false);
        if (Files.exists(dest) == false
            || confirmer.confirm(dest.toString() + " is already exists. overwrite this?")) {
            copy(source, dest);
            return true;
        }
        return false;
    }

    private static void copy(@NonNull Path source, @NonNull Path dest) {
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
