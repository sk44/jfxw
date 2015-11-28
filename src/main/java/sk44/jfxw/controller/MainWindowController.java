package sk44.jfxw.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;

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
    private HBox previewImageContainer;
    private ImageView previewImageView;

    private void loadBackgroundImage(Path imagePath) {
        try {
            double width = rootPane.getPrefWidth();
            double height = rootPane.getPrefHeight();
            Image image = new Image(Files.newInputStream(imagePath), width, height, true, true);
            backgroundImageView.setSmooth(true);
            backgroundImageView.setCache(true);
            backgroundImageView.setPreserveRatio(true);
//            backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());
            backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImageView.setImage(image);
            Message.debug("background image loaded: " + imagePath + ", " + width + "x" + height);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Message.addObserver(this::appendMessage);

//        rootPane.setPrefSize(800, 600);
        ModelLocator.INSTANCE.getConfigurationStore().getConfiguration().backgroundImagePath()
            .ifPresent(this::loadBackgroundImage);

        leftFilerViewController.withFiler(ModelLocator.INSTANCE.getLeftFiler());
        rightFilerViewController.withFiler(ModelLocator.INSTANCE.getRightFiler());

        // TODO くどいのでどうにか
        leftFilerViewController.setChangeFocusListener(rightFilerViewController::focus);
        rightFilerViewController.setChangeFocusListener(leftFilerViewController::focus);

        leftFilerViewController.setChangeCursorListener(this::updateStatus);
        rightFilerViewController.setChangeCursorListener(this::updateStatus);

        leftFilerViewController.setOpenConfigureHandler(this::handleOpenConfigureWindow);
        rightFilerViewController.setOpenConfigureHandler(this::handleOpenConfigureWindow);

        rightFilerViewController.setPreviewImageHandler(this::handleOpenImagePreview);
        // TODO 左

//        leftFilerViewController.setOpenSortHandler(this::handleOpenSortWindow);
//        rightFilerViewController.setOpenSortHandler(this::handleOpenSortWindow);
        leftFilerViewController.setExecutionHandler(this::handleExecute);
        rightFilerViewController.setExecutionHandler(this::handleExecute);

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

    private boolean handleExecute(Path file) {
        // TODO
        String filename = file.toString();
        Message.debug(filename);
        if (filename.endsWith(".mp3")) {
            Message.info("Now playing: " + filename);
            Media media;
            try {
                // TODO 怪しい
//                media = new Media("file://" + URLEncoder.encode(filename, "UTF-8"));
                media = new Media("file://" + filename.replaceAll(" ", "%20"));
                MediaPlayer player = new MediaPlayer(media);
                player.play();
                MediaView mediaView = new MediaView(player);
                // TODO どこでリムーブ？
                // TODO どやってとめるの
                rootPane.getChildren().add(mediaView);
//            } catch (UnsupportedEncodingException ex) {
            } catch (Exception ex) {
                Message.error(ex);
                return false;
            }

            return true;
        }
        return false;
    }

    private void handleOpenImagePreview(Path imagePath, FilerViewController filerViewController) {
        if (previewImageContainer == null) {
            initImagePreview(filerViewController);
        }
        loadPreviewImage(imagePath);
    }

    private void initImagePreview(FilerViewController filerViewController) {
        // 中央寄せするために HBox をかます
        previewImageContainer = new HBox();
        previewImageContainer.setAlignment(Pos.CENTER);
        previewImageContainer.prefWidthProperty().bind(rootPane.widthProperty());
        previewImageContainer.prefHeightProperty().bind(rootPane.heightProperty());
        previewImageContainer.getStyleClass().add("imagePreviewBackground");

        previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewImageView.fitWidthProperty().bind(previewImageContainer.widthProperty());
        previewImageView.fitHeightProperty().bind(previewImageContainer.heightProperty());
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
                    rootPane.getChildren().remove(previewImageContainer);
                    filerViewController.focus();
                    break;
                default:
                    break;
            }
        });
        previewImageContainer.getChildren().add(previewImageView);
    }

    private void loadPreviewImage(Path imagePath) {
        double width = rootPane.getPrefWidth();
        double height = rootPane.getPrefHeight();
        try {
            // TODO 画像が左よりになる
            Image image = new Image(Files.newInputStream(imagePath), width, height, true, true);
            previewImageView.setImage(image);
            rootPane.getChildren().add(previewImageContainer);
            previewImageView.requestFocus();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    private void handleOpenConfigureWindow() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConfigureWindow.fxml"));
        try {
            loader.load();
        } catch (IOException ex) {
            Message.error(ex);
            return;
        }
        ConfigureWindowController controller = loader.getController();
        controller.showOn(rootPane);
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
