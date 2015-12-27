/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import sk44.jfxw.model.message.Message;

/**
 * Path のソート種別定義。
 *
 * @author sk
 */
public enum PathSortType {

    /**
     * ファイル名でソート。
     */
    FILE_NAME(1, "File Name") {

        @Override
        public int compare(Path o1, Path o2) {
            return o1.getFileName().toString().toLowerCase().compareTo(o2.getFileName().toString().toLowerCase());
        }

    },
    FILE_SIZE(2, "File Size") {

        @Override
        public int compare(Path o1, Path o2) {
            try {
                long size1 = Files.size(o1);
                long size2 = Files.size(o2);
                return Long.compare(size1, size2);
            } catch (IOException ex) {
                Message.error(ex);
                return -1;
            }
        }

        @Override
        protected boolean canSortDirectories() {
            // サイズの場合ディレクトリをソートしても仕方ないので常にソートしない
            return false;
        }

    },
    /**
     * 更新日時でソート。
     */
    LAST_MODIFIED(3, "Last Modified") {

        @Override
        public int compare(Path o1, Path o2) {
            try {
                return Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
            } catch (IOException ex) {
                Message.error(ex);
                return -1;
            }
        }

    };

    @Getter
    private final int id;
    @Getter
    private final String displayName;

    private PathSortType(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    protected abstract int compare(Path path1, Path path2);

    protected boolean canSortDirectories() {
        return true;
    }

    int compare(Path path1, Path path2, boolean sortDirectories, PathSortOrder order) {
        if (sortDirectories == false || canSortDirectories() == false) {
            // ディレクトリは常に上に表示
            if (Files.isDirectory(path1) && Files.isDirectory(path2) == false) {
                return -1;
            }
            if (Files.isDirectory(path1) == false && Files.isDirectory(path2)) {
                return 1;
            }
            if (Files.isDirectory(path1) && Files.isDirectory(path2)) {
                return order.order(FILE_NAME.compare(path1, path2));
            }
        }
        return order.order(compare(path1, path2));
    }
}
