/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import sk44.jfxw.controller.FilerViewController;
import sk44.jfxw.model.message.Message;

/**
 * 画像をプレビューする用.
 *
 * @author sk
 */
public class ImageViewer {

    private final Consumer<Path> saveHandler;
    private HBox previewImageContainer;
    private ImageView previewImageView;
    private Path imagePath;

    public ImageViewer(Consumer<Path> saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void open(Path imagePath, FilerViewController launcherController, Pane basePane) {
        if (previewImageContainer == null) {
            initImagePreview(launcherController, basePane);
        }
        loadPreviewImage(imagePath);
        basePane.getChildren().add(previewImageContainer);
        previewImageView.requestFocus();
    }

    private void close(FilerViewController launcherController, Pane basePane) {
        previewImageView.setImage(null);
        basePane.getChildren().remove(previewImageContainer);
        launcherController.endPreviewImage();
    }

    private void initImagePreview(FilerViewController launcherController, Pane basePane) {
        // 中央寄せするために HBox をかます
        previewImageContainer = new HBox();
        previewImageContainer.setAlignment(Pos.CENTER);
        previewImageContainer.prefWidthProperty().bind(basePane.widthProperty());
        previewImageContainer.prefHeightProperty().bind(basePane.heightProperty());
        previewImageContainer.getStyleClass().add("imagePreviewBackground");

        initPreviewImageView(launcherController, basePane);

        previewImageContainer.getChildren().add(previewImageView);
    }

    private void initPreviewImageView(FilerViewController launcherController, Pane basePane) {

        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewImageView.setCache(true);

        // container にバインドすると画質がえらく悪くなる上にリサイズがうまくいかない
//        previewImageView.fitWidthProperty().bind(previewImageContainer.widthProperty());
//        previewImageView.fitHeightProperty().bind(previewImageContainer.heightProperty());
        previewImageView.fitWidthProperty().bind(basePane.widthProperty());
        previewImageView.fitHeightProperty().bind(basePane.heightProperty());
        previewImageView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case J:
                    launcherController.nextImage().ifPresent(nextImage -> {
                        loadPreviewImage(nextImage);
                    });
                    break;
                case K:
                    launcherController.previousImage().ifPresent(previousImage -> {
                        loadPreviewImage(previousImage);
                    });
                    break;
                case S:
                    if (saveHandler != null) {
                        saveHandler.accept(imagePath);
                    }
                    break;
                case ESCAPE:
                case ENTER:
                    close(launcherController, basePane);
                    break;
                default:
                    break;
            }
        });
        // メッセージ窓をクリックしたりしてフォーカスを失うと制御不能になるので
        previewImageView.focusedProperty().addListener((arg, oldValue, focused) -> {
            if (focused == false) {
                close(launcherController, basePane);
            }
        });
    }

    private void loadPreviewImage(Path imagePath) {
        try {
            Image image = new Image(Files.newInputStream(imagePath));
            previewImageView.setImage(image);
            this.imagePath = imagePath;
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
