/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private HBox previewImageContainer;
    private ImageView previewImageView;

    public void open(Path imagePath, FilerViewController filerViewController, Pane basePane) {
        if (previewImageContainer == null) {
            initImagePreview(filerViewController, basePane);
        }
        loadPreviewImage(imagePath, basePane);
    }

    private void initImagePreview(FilerViewController filerViewController, Pane basePane) {
        // 中央寄せするために HBox をかます
        previewImageContainer = new HBox();
        previewImageContainer.setAlignment(Pos.CENTER);
        previewImageContainer.prefWidthProperty().bind(basePane.widthProperty());
        previewImageContainer.prefHeightProperty().bind(basePane.heightProperty());
        previewImageContainer.getStyleClass().add("imagePreviewBackground");

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
                    // TODO 次イメージの表示
                    break;
                case K:
                    // TODO 前イメージの表示
                    break;
                case ESCAPE:
                case ENTER:
                    previewImageView.setImage(null);
                    basePane.getChildren().remove(previewImageContainer);
                    filerViewController.focus();
                    break;
                default:
                    break;
            }
        });
        previewImageContainer.getChildren().add(previewImageView);
    }

    private void loadPreviewImage(Path imagePath, Pane basePane) {
        try {
            Image image = new Image(Files.newInputStream(imagePath));
            previewImageView.setImage(image);
            basePane.getChildren().add(previewImageContainer);
            previewImageView.requestFocus();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
