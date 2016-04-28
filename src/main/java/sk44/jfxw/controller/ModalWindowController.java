/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import lombok.Setter;

/**
 *
 * @author sk
 * @param <T>
 */
public abstract class ModalWindowController<T> {

    @Setter
    private Runnable closeAction;

    public abstract T getResult();

    protected void close() {
        closeAction.run();
    }
}
