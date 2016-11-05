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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
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
    private BorderPane previewImageContainer;
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
//        previewImageContainer = new HBox();
//        previewImageContainer.setAlignment(Pos.CENTER);
        previewImageContainer = new BorderPane();
        previewImageContainer.prefWidthProperty().bind(basePane.widthProperty());
        previewImageContainer.prefHeightProperty().bind(basePane.heightProperty());
        previewImageContainer.getStyleClass().add("imagePreviewBackground");

        initPreviewImageView(launcherController);

//        StackPane imageHolder = new StackPane(previewImageView);
        BorderPane imageHolder = new BorderPane();
        imageHolder.setCenter(previewImageView);

        // TODO 上下中央寄せにならない（上詰めになる）
        // http://stackoverflow.com/questions/30687994/how-to-center-the-content-of-a-javafx-8-scrollpane
        scrollPane = new ScrollPane();
        scrollPane.prefWidthProperty().bind(basePane.widthProperty());
        scrollPane.prefHeightProperty().bind(basePane.heightProperty());
        scrollPane.setContent(imageHolder);

        imageHolder.minWidthProperty().bind(Bindings.createDoubleBinding(()
            -> scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));

//        previewImageContainer.getChildren().add(scrollPane);
        previewImageContainer.setCenter(scrollPane);

    }

    private void initPreviewImageView(FilerViewController launcherController) {

        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewImageView.setCache(true);

        // container にバインドすると画質がえらく悪くなる上にリサイズがうまくいかない
//        previewImageView.fitWidthProperty().bind(previewImageContainer.widthProperty());
//        previewImageView.fitHeightProperty().bind(previewImageContainer.heightProperty());
        if (scaleable) {
            previewImageContainer.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                toggleBinding();
            });
        } else {
            bindImageSize();
            // メッセージ窓をクリックしたりしてフォーカスを失うと制御不能になるので
//        previewImageView.focusedProperty().addListener((arg, oldValue, focused) -> {
            previewImageContainer.focusedProperty().addListener((arg, oldValue, focused) -> {
                if (focused == false) {
                    close(launcherController);
                }
            });
        }
        previewImageContainer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
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
                    ModelLocator.INSTANCE.getBackgroundImage().update(imagePath);
                    break;
                case ESCAPE:
                case ENTER:
                    close(launcherController);
                    break;
                default:
                    break;
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
        // TODO カーソルの形状を変えるとか
//        previewImageView.setOnMouseEntered(e -> {
//            basePane.getScene().setCursor(Cursor.OPEN_HAND);
//        });
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
        try {
            Image image = new Image(Files.newInputStream(imagePath));
            // ウィンドウサイズより小さい画像であればこれでよい
            // が、右窓でやるとおかしくなる
//            previewImageView.setFitWidth(image.getWidth());
//            previewImageView.setFitHeight(image.getHeight());
//            previewImageView.fitWidthProperty().bind(basePane.widthProperty());
//            previewImageView.fitHeightProperty().bind(basePane.heightProperty());
            previewImageView.setImage(image);
            if (scaleable) {
                if (binding
                    && (basePane.getWidth() < image.getWidth() || basePane.getHeight() < image.getHeight())) {
                    bindImageSize();
                } else {
                    unbindImageSize();
                    // ページ移動したときスクロール位置を一番上に
                    scrollPane.setVvalue(0.0);
                }
            }
            this.imagePath = imagePath;
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
