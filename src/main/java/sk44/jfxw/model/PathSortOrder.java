package sk44.jfxw.model;

/**
 *
 * @author sk
 */
public enum PathSortOrder {

    ASC {

            @Override
            public int order(int order) {
                return order;
            }
        },
    DESC {

            @Override
            public int order(int order) {
                return order * -1;
            }
        };

    public abstract int order(int order);

}
