/*
 *
 *
 *
 */
package sk44.jfxw.controller.modal;

import lombok.Setter;

/**
 *
 * @author sk
 * @param <T>
 */
public abstract class ModalWindowController<T> {

    @Setter
    private Runnable closeAction;

    public void preShown() {
        // overridable
    }

    public abstract T getResult();

    protected void close() {
        closeAction.run();
    }
}
