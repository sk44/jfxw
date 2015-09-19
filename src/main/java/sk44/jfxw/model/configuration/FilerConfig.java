/*
 *
 *
 *
 */
package sk44.jfxw.model.configuration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.PathSortOrder;
import sk44.jfxw.model.PathSortType;

/**
 * ファイラー設定。
 *
 * @author sk
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class FilerConfig {

    static FilerConfig defaultConfig(String defaultPath) {
        return new FilerConfig(defaultPath, PathSortType.FILE_NAME, PathSortOrder.ASC, false);
    }

    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private PathSortType sortType;
    @Getter
    @Setter
    private PathSortOrder sortOrder;
    @Getter
    @Setter
    private boolean sortDirectories;

    void update(Filer filer) {
        // TODO
        path = filer.getCurrentDir().toString();
        sortType = filer.getSortType();
        sortOrder = filer.getSortOrder();
        sortDirectories = filer.isSortDirectories();
    }

}
