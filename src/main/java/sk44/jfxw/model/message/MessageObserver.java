/*
 */
package sk44.jfxw.model.message;

/**
 *
 * @author sk
 */
@FunctionalInterface
public interface MessageObserver {

    void update(String message);

}