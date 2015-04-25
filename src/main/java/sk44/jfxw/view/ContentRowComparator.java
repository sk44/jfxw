/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.util.Comparator;

/**
 *
 * @author sk
 */
public enum ContentRowComparator implements Comparator<ContentRow> {

    BY_DEFAULT {

            @Override
            public int compare(ContentRow o1, ContentRow o2) {
                if (o1.isDirectory() && o2.isDirectory() == false) {
                    return -1;
                }
                if (o1.isDirectory() == false && o2.isDirectory()) {
                    return 1;
                }
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }

        }

}
