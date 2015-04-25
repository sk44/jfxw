/*
 */
package sk44.jfxw.model;

/**
 *
 * @author sk
 */
@FunctionalInterface
public interface MessageObserver {

    void update(String message);

}
