package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ImageViewer;

public class MainWindowController implements Initializable {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private ImageView backgroundImageView;
    @FXML
    private FilerViewController leftFilerViewController;
    @FXML
    private FilerViewController rightFilerViewController;
    @FXML
    private TextArea messageArea;
    @FXML
    private Label statusLabel;

    private final ImageViewer imageViewerInWindow = new ImageViewer(this::loadBackgroundImage);
    private final ImageViewer imageViewerInFiler = new ImageViewer(this::loadBackgroundImage);

    private void initBackgroundImageView() {
        backgroundImageView.setSmooth(true);
        backgroundImageView.setCache(true);
        backgroundImageView.setPreserveRatio(true);
        // 横幅が足りないことが多いので横幅だけにバインドする
//        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
    }

    private void loadBackgroundImage(Path imagePath) {
        try {
            double width = rootPane.getPrefWidth();
            double height = rootPane.getPrefHeight();
            Image image = new Image(Files.newInputStream(imagePath), width, height, true, true);
            backgroundImageView.setImage(image);
            Message.debug("background image loaded: " + imagePath + ", " + width + "x" + height);
            ModelLocator.INSTANCE.getConfigurationStore().getConfiguration()
                .setBackgroundImagePath(imagePath.normalize().toString());
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Message.addObserver(this::appendMessage);

        initBackgroundImageView();
        ModelLocator.INSTANCE.getConfigurationStore().getConfiguration().backgroundImagePath()
            .ifPresent(this::loadBackgroundImage);

        // 左ファイル窓
        Filer leftFiler = ModelLocator.INSTANCE.getLeftFiler();
        leftFiler.addListenerToPreviewImageEvent(imagePath -> {
            // 反対側の filer に画像を表示する
            imageViewerInFiler.open(imagePath, leftFilerViewController, rightFilerViewController.getRootPane());
        });
        leftFiler.addListenerToCursorChangedEvent(this::updateStatus);

        // 右ファイル窓
        Filer rightFiler = ModelLocator.INSTANCE.getRightFiler();
        rightFiler.addListenerToPreviewImageEvent(imagePath -> {
            imageViewerInWindow.open(imagePath, rightFilerViewController, rootPane);
        });
        rightFiler.addListenerToCursorChangedEvent(this::updateStatus);

        leftFilerViewController.withFiler(leftFiler);
        rightFilerViewController.withFiler(rightFiler);

        messageArea.focusedProperty().addListener((arg, oldValue, focused) -> {
            if (focused) {
                // カーソル操作ができなくなるので、むりやり元のファイル窓にフォーカスを戻す
                leftFiler.updateFocus();
            }
        });

        leftFiler.changeDirectoryToInitPath();
        rightFiler.changeDirectoryToInitPath();

        leftFiler.focus();
        messageArea.appendText("Ready.");
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            messageArea.appendText("\n" + message);
        });
    }

    private void updateStatus(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            statusLabel.setText("");
            return;
        }
        String value = fileName.toString();
        if (Files.isDirectory(path)) {
            statusLabel.setText(value + "/");
        } else {
            statusLabel.setText(value);
        }
    }
}
