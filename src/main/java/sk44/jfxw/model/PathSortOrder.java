package sk44.jfxw.model;

/**
 *
 * @author sk
 */
public enum PathSortOrder {

    ASC {

        @Override
        int order(int order) {
            return order;
        }
    },
    DESC {

        @Override
        int order(int order) {
            return order * -1;
        }
    };

    abstract int order(int order);

}
