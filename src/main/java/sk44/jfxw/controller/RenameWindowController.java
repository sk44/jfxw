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
public class RenameWindowController implements Initializable {

    @FXML
    private Pane rootPane;
    @FXML
    private TextField textField;

    @Setter
    private Runnable closeAction;
    @Setter
    private Consumer<String> updateAction;

    /**
     * Initializes the controller class.
     */
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

    public void setInitialValue(String initialValue) {
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

    private void close() {
        closeAction.run();
    }
}
