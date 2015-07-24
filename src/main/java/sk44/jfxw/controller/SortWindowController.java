/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import sk44.jfxw.model.message.Message;

/**
 * ソート種別選択ウィンドウコントローラー。
 *
 * @author sk
 */
public class SortWindowController implements Initializable {

    @FXML
    private Pane rootPane;
    private Pane parentPane;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.rootPane.requestFocus();
    }

    void showOn(Pane parentPane) {
        // TODO: 同じコード
        this.parentPane = parentPane;
        rootPane.prefHeightProperty().bind(parentPane.heightProperty());
        rootPane.prefWidthProperty().bind(parentPane.widthProperty());
        parentPane.getChildren().add(rootPane);
    }

    private void close() {
        parentPane.getChildren().remove(rootPane);
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
        Message.debug(event.getCode().toString());
    }

}
