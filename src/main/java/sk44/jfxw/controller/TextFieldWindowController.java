/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;
import sk44.jfxw.model.message.Message;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class TextFieldWindowController implements Initializable {

    @FunctionalInterface
    public interface KeyReleaseHandler {

        void handle(String text, KeyEvent e);
    }

    @FXML
    private Pane rootPane;
    @FXML
    private Label titleLabel;
    @FXML
    private TextField textField;

    @Setter
    private Runnable closeAction;
    @Setter
    private Consumer<String> updateAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textField.requestFocus();
        this.rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCommandKeyPressed);
    }

    protected void handleCommandKeyPressed(KeyEvent event) {

        switch (event.getCode()) {
            case ESCAPE:
                close();
                break;
            default:
                break;
        }
    }

    @FXML
    void handleTextEnter(Event event) {
        update();
    }

    public void updateContent(String title, String initialValue) {
        titleLabel.setText(title);
        textField.setText(initialValue);
    }

    private void update() {

        String newValue = textField.getText();
        if (newValue == null || newValue.isEmpty()) {
            Message.warn("new value must not be empty.");
            return;
        }

        updateAction.accept(newValue);
        close();

    }

    public void addKeyReleasedEventHandler(KeyReleaseHandler handler) {
        textField.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            handler.handle(textField.getText(), e);
        });
    }

    private void close() {
        closeAction.run();
    }
}
