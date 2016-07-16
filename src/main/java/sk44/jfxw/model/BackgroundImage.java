/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import sk44.jfxw.model.message.Message;

/**
 * 背景画像。

 * @author sk
 */
public class BackgroundImage {

    private final Random indexSelector = new Random();

    @FunctionalInterface
    public interface UpdateBackgroundImageListener {

        void changeImageTo(Path imagePath);
    }

    private final EventSource<UpdateBackgroundImageListener> backgroundImageUpdating = new EventSource<>();

    public void addBackgroundImageUpdatingListener(UpdateBackgroundImageListener listener) {
        this.backgroundImageUpdating.addListener(listener);
    }

    private void raiseBackgroundImageUpdating(Path imagePath) {
        this.backgroundImageUpdating.raiseEvent(listener -> listener.changeImageTo(imagePath));
    }

    public void update(Path imagePath) {
        raiseBackgroundImageUpdating(imagePath);
    }

    public void updateRandom(Path imageDir) {
        if (Files.exists(imageDir) == false || Files.isDirectory(imageDir) == false) {
            Message.warn(imageDir + " does not exists or not a directory.");
            return;
        }
        try (DirectoryStream<Path> stream = Files
            .newDirectoryStream(imageDir, "*.{jpg,jpeg,png,gif}")) {
            List<Path> images = StreamSupport.stream(stream.spliterator(), false)
                .collect(Collectors.toList());
            if (images.isEmpty()) {
                Message.warn("no images found in " + imageDir + ".");
                return;
            }
            int targetIndex = indexSelector.nextInt(images.size() - 1);
            raiseBackgroundImageUpdating(images.get(targetIndex));

        } catch (IOException ex) {
            Message.error(ex);
        }

    }
}
