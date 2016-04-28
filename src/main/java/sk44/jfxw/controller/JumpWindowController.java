/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class JumpWindowController extends ModalWindowController<Void> implements Initializable {

    @FXML
    private Pane rootPane;
    @FXML
    private TextField textField;

    @Setter
    private Consumer<Path> jumpAction;

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
//        update();
        // TODO 絞り込む？
        jumpAction.accept(Paths.get(textField.getText()));
        close();
    }

    @Override
    public Void getResult() {
        return null;
    }
}
