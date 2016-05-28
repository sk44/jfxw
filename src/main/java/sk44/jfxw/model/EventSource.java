/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event source.

 * @author sk
 * @param <T> some action
 */
public class EventSource<T> {

    // TODO 弱参照にしたほうがいいか
    private final List<T> listeners = new ArrayList<>();

    public void addListener(T listener) {
        listeners.add(listener);
    }

    public void raiseEvent(Consumer<T> consumer) {
        listeners.forEach(consumer);
    }
}
