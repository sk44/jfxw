package sk44.jfxw.controller;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import sk44.jfxw.model.Message;
import sk44.jfxw.model.MessageLevel;

public class MainWindowController implements Initializable {

    @FXML
    private FilerViewController leftFilerViewController;
    @FXML
    private FilerViewController rightFilerViewController;
    @FXML
    private TextArea messageArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Path initialPath = getInitialPath();
        leftFilerViewController.moveTo(initialPath);
        rightFilerViewController.moveTo(initialPath);

        leftFilerViewController.withOtherFileViewController(rightFilerViewController);
        rightFilerViewController.withOtherFileViewController(leftFilerViewController);

        leftFilerViewController.focus();

        messageArea.appendText("Ready.");
        Message.minLevel(MessageLevel.TRACE);
        Message.addObserver(message -> messageArea.appendText("\n" + message));
    }

    private Path getInitialPath() {
        // TODO
        return new File(".").toPath();
    }
}
