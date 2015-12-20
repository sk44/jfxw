/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
