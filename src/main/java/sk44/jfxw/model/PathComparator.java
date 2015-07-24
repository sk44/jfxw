/*
 *
 *
 *
 */
package sk44.jfxw.model;

import sk44.jfxw.model.message.Message;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Path のソート用 Comparator 実装。
 *
 * @author sk
 */
public enum PathComparator implements Comparator<Path> {

    /**
     * ファイル名でソート。
     */
    FILE_NAME {

            @Override
            public int compare(Path o1, Path o2) {
                if (Files.isDirectory(o1) && Files.isDirectory(o2) == false) {
                    return -1;
                }
                if (Files.isDirectory(o1) == false && Files.isDirectory(o2)) {
                    return 1;
                }
                return o1.getFileName().toString().toLowerCase().compareTo(o2.getFileName().toString().toLowerCase());
            }

        },
    /**
     * 更新日時でソート。
     */
    LAST_MODIFIED {

            @Override
            public int compare(Path o1, Path o2) {
                try {
                    return Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
                } catch (IOException ex) {
                    Message.error(ex);
                    return -1;
                }
            }

        }
}
