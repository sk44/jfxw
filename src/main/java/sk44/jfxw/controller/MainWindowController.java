package sk44.jfxw.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import sk44.jfxw.model.Message;
import sk44.jfxw.model.MessageLevel;

public class MainWindowController implements Initializable {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private FilerViewController leftFilerViewController;
    @FXML
    private FilerViewController rightFilerViewController;
    @FXML
    private TextArea messageArea;
    @FXML
    private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Path initialPath = getInitialPath();
        leftFilerViewController.changeDirectoryTo(initialPath);
        rightFilerViewController.changeDirectoryTo(initialPath);

        leftFilerViewController.withOtherFileViewController(rightFilerViewController);
        rightFilerViewController.withOtherFileViewController(leftFilerViewController);

        // TODO くどいのでどうにか
        leftFilerViewController.setChangeCursorListener(this::updateStatus);
        rightFilerViewController.setChangeCursorListener(this::updateStatus);

        leftFilerViewController.setOpenConfigureHandler(this::handleOpenConfigureWindow);
        rightFilerViewController.setOpenConfigureHandler(this::handleOpenConfigureWindow);

        leftFilerViewController.setOpenSortHandler(this::handleOpenSortWindow);
        rightFilerViewController.setOpenSortHandler(this::handleOpenSortWindow);

        leftFilerViewController.setExecutionHandler(this::handleExecute);
        rightFilerViewController.setExecutionHandler(this::handleExecute);

        leftFilerViewController.focus();

        messageArea.appendText("Ready.");
        Message.minLevel(MessageLevel.TRACE);
        Message.addObserver(message -> messageArea.appendText("\n" + message));
    }

    private Path getInitialPath() {
        // TODO どこかに設定
        return new File(".").toPath();
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

    private void handleOpenSortWindow() {
        // handleOpenConfigureWindow とほとんど同じなのでどうにか
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortWindow.fxml"));
        try {
            loader.load();
        } catch (IOException ex) {
            Message.error(ex);
            return;
        }
        SortWindowController controller = loader.getController();
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
