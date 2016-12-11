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
import sk44.jfxw.model.FilerEvents;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.configuration.Configuration;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ImageViewer;
import sk44.jfxw.view.TextViewer;

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

    private final ImageViewer imageViewerInWindow = new ImageViewer(true);
    private final ImageViewer imageViewerInFiler = new ImageViewer(false);
    private final TextViewer textViewer = new TextViewer();

    private void initBackgroundImageView() {
        backgroundImageView.setSmooth(true);
        backgroundImageView.setCache(true);
        backgroundImageView.setPreserveRatio(true);
        // 横幅が足りないことが多いので横幅だけにバインドする
//        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
    }

    private void updateBackgroundImage(Path imagePath) {
        try {
            double width = rootPane.getPrefWidth();
            double height = rootPane.getPrefHeight();
            Image image = new Image(Files.newInputStream(imagePath), width, height, true, true);
            backgroundImageView.setImage(image);
            Message.info("background image updated: " + imagePath);
            ModelLocator.INSTANCE.getConfigurationStore().getConfiguration()
                .setBackgroundImagePath(imagePath.normalize().toString());
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Message.addObserver(this::appendMessage);
        ModelLocator locator = ModelLocator.INSTANCE;
        locator.getBackgroundImage().addBackgroundImageUpdatingListener(this::updateBackgroundImage);

        initBackgroundImageView();
        final Configuration configuration = locator.getConfigurationStore().getConfiguration();
        configuration.backgroundImagePath()
            .ifPresent(this::updateBackgroundImage);
        configuration.mainFont().ifPresent(font -> {
            rootPane.setStyle("-fx-font: 12px \"" + font + "\";");
        });

        // 左ファイル窓
        Filer leftFiler = locator.getLeftFiler();
        FilerEvents leftFilerEvents = leftFiler.getEvents();
        leftFilerEvents.addListenerToImageShowing(imagePath -> {
            // 反対側の filer に画像を表示する
            imageViewerInFiler.open(imagePath, leftFilerViewController, rightFilerViewController.getRootPane());
        });
        leftFilerEvents.addListenerToTextShowing(textPath -> {
            textViewer.open(textPath, rootPane, leftFilerViewController);
        });
        leftFilerEvents.addListenerToCursorChanged(this::updateStatus);

        // 右ファイル窓
        Filer rightFiler = locator.getRightFiler();
        FilerEvents rightFilerEvents = rightFiler.getEvents();
        rightFilerEvents.addListenerToImageShowing(imagePath -> {
            imageViewerInWindow.open(imagePath, rightFilerViewController, rootPane);
        });
        rightFilerEvents.addListenerToTextShowing(textPath -> {
            textViewer.open(textPath, rootPane, rightFilerViewController);
        });
        rightFilerEvents.addListenerToCursorChanged(this::updateStatus);

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
        if (Files.isSymbolicLink(path)) {
            try {
                Path link = Files.readSymbolicLink(path);
                statusLabel.setText(value + "@ -> " + link);
            } catch (IOException ex) {
                Message.error(ex);
            }
            return;
        }
        if (Files.isDirectory(path)) {
            statusLabel.setText(value + "/");
        } else {
            statusLabel.setText(value);
        }
    }
}
