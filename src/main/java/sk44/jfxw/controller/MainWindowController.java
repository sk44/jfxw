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

        leftFilerViewController.withFiler(ModelLocator.INSTANCE.getLeftFiler());
        rightFilerViewController.withFiler(ModelLocator.INSTANCE.getRightFiler());

        // TODO くどいのでどうにか
        leftFilerViewController.setChangeFocusListener(() -> {
            rightFilerViewController.focus();
            leftFilerViewController.onLostFocus();
        });
        rightFilerViewController.setChangeFocusListener(() -> {
            leftFilerViewController.focus();
            rightFilerViewController.onLostFocus();
        });

        leftFilerViewController.setChangeCursorListener(this::updateStatus);
        rightFilerViewController.setChangeCursorListener(this::updateStatus);

        rightFilerViewController.setPreviewImageHandler(imagePath -> {
            imageViewerInWindow.open(imagePath, rightFilerViewController, rootPane);
        });
        leftFilerViewController.setPreviewImageHandler(imagePath -> {
            // 反対側の filer に画像を表示する
            imageViewerInFiler.open(imagePath, leftFilerViewController, rightFilerViewController.getRootPane());
        });

        ModelLocator.INSTANCE.getLeftFiler().changeDirectoryToInitPath();
        ModelLocator.INSTANCE.getRightFiler().changeDirectoryToInitPath();
        // TODO 初回表示時、スクロールバーが動かない
        leftFilerViewController.focus();
        messageArea.appendText("Ready.");
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            messageArea.appendText("\n" + message);
        });
    }

    private void updateStatus(Path path) {
        String value = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            statusLabel.setText(value + "/");
        } else {
            statusLabel.setText(value);
        }
    }
}
