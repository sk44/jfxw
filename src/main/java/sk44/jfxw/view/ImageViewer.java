/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import sk44.jfxw.controller.FilerViewController;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;

/**
 * 画像をプレビューする用.
 *
 * @author sk
 */
public class ImageViewer {

    public ImageViewer(boolean scaleable) {
        this.scaleable = scaleable;
    }

    private final boolean scaleable;

    private ScrollPane scrollPane;
    private HBox previewImageContainer;
    private ImageView previewImageView;
    private Path imagePath;
    private Pane basePane;

    public void open(Path imagePath, FilerViewController launcherController, Pane basePane) {
        if (previewImageContainer == null) {
            this.basePane = basePane;
            initImagePreview(launcherController);
        }
        loadPreviewImage(imagePath);
        basePane.getChildren().add(previewImageContainer);
        Platform.runLater(() -> {
            // これらだとフォーカスが効かない
//            previewImageView.requestFocus();
//            scrollPane.requestFocus();
            previewImageContainer.requestFocus();
        });
    }

    private void close(FilerViewController launcherController) {
        previewImageView.setImage(null);
        basePane.getChildren().remove(previewImageContainer);
        launcherController.endPreviewImage();
    }

    private void initImagePreview(FilerViewController launcherController) {
        // 中央寄せするために HBox をかます
        previewImageContainer = new HBox();
        previewImageContainer.setAlignment(Pos.CENTER);
        previewImageContainer.prefWidthProperty().bind(basePane.widthProperty());
        previewImageContainer.prefHeightProperty().bind(basePane.heightProperty());
        previewImageContainer.getStyleClass().add("imagePreviewBackground");

        initPreviewImageView(launcherController);

        StackPane imageHolder = new StackPane(previewImageView);

//        previewImageContainer.getChildren().add(previewImageView);
        // TODO 上下中央寄せにならない（上詰めになる）
        // http://stackoverflow.com/questions/30687994/how-to-center-the-content-of-a-javafx-8-scrollpane
        scrollPane = new ScrollPane();
        scrollPane.prefWidthProperty().bind(basePane.widthProperty());
        scrollPane.prefHeightProperty().bind(basePane.heightProperty());
//        scrollPane.setContent(previewImageView);
        scrollPane.setContent(imageHolder);
        GridPane grid = new GridPane();

        imageHolder.minWidthProperty().bind(Bindings.createDoubleBinding(()
            -> scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));
        grid.getChildren().add(imageHolder);

//        scrollPane.setContent(previewImageContainer);
        previewImageContainer.getChildren().add(scrollPane);

    }

    private void initPreviewImageView(FilerViewController launcherController) {

        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewImageView.setCache(true);

        // container にバインドすると画質がえらく悪くなる上にリサイズがうまくいかない
//        previewImageView.fitWidthProperty().bind(previewImageContainer.widthProperty());
//        previewImageView.fitHeightProperty().bind(previewImageContainer.heightProperty());
// TODO
        bindImageSize();
        previewImageContainer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
//        previewImageView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
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
                    toggleBinding();
                    break;
                case W:
                    ModelLocator.INSTANCE.getApplicationEvents().raiseBackgroundImageUpdating(imagePath);
                    break;
                case ESCAPE:
                case ENTER:
                    close(launcherController);
                    break;
                default:
                    break;
            }
        });
        // メッセージ窓をクリックしたりしてフォーカスを失うと制御不能になるので
//        previewImageView.focusedProperty().addListener((arg, oldValue, focused) -> {
        previewImageContainer.focusedProperty().addListener((arg, oldValue, focused) -> {
            if (focused == false) {
                close(launcherController);
            }
        });
    }

    private boolean binding = false;

    private void toggleBinding() {
        if (scaleable == false) {
            return;
        }
        if (binding) {
            unbindImageSize();
        } else {
            bindImageSize();
        }
    }

    private void bindImageSize() {
        previewImageView.fitWidthProperty().bind(basePane.widthProperty());
        previewImageView.fitHeightProperty().bind(basePane.heightProperty());
        binding = true;
    }

    private void unbindImageSize() {
        previewImageView.fitWidthProperty().unbind();
        previewImageView.fitHeightProperty().unbind();
        Image image = previewImageView.getImage();
        if (image != null) {
            previewImageView.setFitWidth(image.getWidth());
            previewImageView.setFitHeight(image.getHeight());
        }
        binding = false;
    }

    private void loadPreviewImage(Path imagePath) {
        if (scaleable && binding == false) {
            bindImageSize();
        }
        try {
            Image image = new Image(Files.newInputStream(imagePath));
            // ウィンドウサイズより小さい画像であればこれでよい
            // が、右窓でやるとおかしくなる
//            previewImageView.setFitWidth(image.getWidth());
//            previewImageView.setFitHeight(image.getHeight());
//            previewImageView.fitWidthProperty().bind(basePane.widthProperty());
//            previewImageView.fitHeightProperty().bind(basePane.heightProperty());
            previewImageView.setImage(image);
            this.imagePath = imagePath;
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
