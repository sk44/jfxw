/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.nio.file.Path;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 * @author sk
 */
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PathComparator implements Comparator<Path> {

    private final PathSortType sortType;
    private final PathSortOrder sortOrder;
    private final boolean sortDirectories;

    @Override
    public int compare(Path o1, Path o2) {
        return sortOrder.order(sortType.compare(o1, o2, sortDirectories));
    }

}
