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

    private final EventSource<UpdateBackgroundImageListener> backgroundImageUpdating = new EventSource<>();

    public void addBackgroundImageUpdatingListener(UpdateBackgroundImageListener listener) {
        this.backgroundImageUpdating.addListener(listener);
    }

    public void raiseBackgroundImageUpdating(Path imagePath) {
        this.backgroundImageUpdating.raiseEvent(listener -> listener.changeImageTo(imagePath));
    }
}
