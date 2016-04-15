/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class ConfirmWindowController implements Initializable, ModalWindowController<Boolean> {

    @FXML
    private Pane rootPane;

    @FXML
    private Label messageLabel;

    @Setter
    private Runnable okAction;
    @Setter
    private Runnable closeAction;

    private boolean resultOK = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCommandKeyPressed);
    }

    protected void handleCommandKeyPressed(KeyEvent event) {

        switch (event.getCode()) {
            case ESCAPE:
                close();
                break;
            case ENTER:
                execute();
                break;
            default:
                break;
        }
    }

    @FXML
    protected void handleOKAction(ActionEvent event) {
        resultOK = true;
        execute();
    }

    @FXML
    protected void handleCancelAction(ActionEvent event) {
        resultOK = false;
        close();
    }

    public void updateMessage(String message) {
        messageLabel.setText(message);
    }

    private void execute() {
        this.okAction.run();
        close();
    }

    private void close() {
        this.closeAction.run();
    }

    @Override
    public Boolean getResult() {
        return resultOK;
    }

}
