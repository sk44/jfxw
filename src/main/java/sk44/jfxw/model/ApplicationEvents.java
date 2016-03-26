/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.nio.file.Path;

/**
 *
 * @author sk
 */
public class ApplicationEvents {

    @FunctionalInterface
    public interface UpdateBackgroundImageListener {

        void changeImageTo(Path imagePath);
    }

    private final EventSource<UpdateBackgroundImageListener> updateBackgroundImageEvent = new EventSource<>();

    public void addUpdateBackgroundImageListener(UpdateBackgroundImageListener listener) {
        this.updateBackgroundImageEvent.addListener(listener);
    }

    public void updateBackgroundImage(Path imagePath) {
        this.updateBackgroundImageEvent.raiseEvent(listener -> listener.changeImageTo(imagePath));
    }
}
