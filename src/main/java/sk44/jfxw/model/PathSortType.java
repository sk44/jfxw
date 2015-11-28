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
            public int compare(Path o1, Path o2, boolean sortDirectories) {
                if (sortDirectories == false) {
                    if (Files.isDirectory(o1) && Files.isDirectory(o2) == false) {
                        return -1;
                    }
                    if (Files.isDirectory(o1) == false && Files.isDirectory(o2)) {
                        return 1;
                    }
                }
                return o1.getFileName().toString().toLowerCase().compareTo(o2.getFileName().toString().toLowerCase());
            }

        },
    FILE_SIZE(2, "File Size") {

            @Override
            public int compare(Path o1, Path o2, boolean sortDirectories) {

                // サイズの場合は常にディレクトリを除外する（ソートしてもいまいち使いづらいので）
                if (Files.isDirectory(o1) || Files.isDirectory(o2)) {
                    return FILE_NAME.compare(o1, o2, sortDirectories);
                }
                try {
                    long size1 = Files.size(o1);
                    long size2 = Files.size(o2);
                    return Long.compare(size1, size2);
                } catch (IOException ex) {
                    Message.error(ex);
                    return -1;
                }
            }

        },
    /**
     * 更新日時でソート。
     */
    LAST_MODIFIED(3, "Last Modified") {

            @Override
            public int compare(Path o1, Path o2, boolean sortDirectories) {
                if (sortDirectories == false) {
                    if (Files.isDirectory(o1) || Files.isDirectory(o2)) {
                        return FILE_NAME.compare(o1, o2, sortDirectories);
                    }
                }
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

    public abstract int compare(Path path1, Path path2, boolean sortDirectories);
}