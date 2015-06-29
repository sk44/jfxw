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
import javafx.scene.layout.Pane;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class ConfigureWindowController implements Initializable {

    @FXML
    private Pane rootPane;
    private Pane parentPane;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    void showOn(Pane parentPane) {
        this.parentPane = parentPane;
        rootPane.prefHeightProperty().bind(parentPane.heightProperty());
        rootPane.prefWidthProperty().bind(parentPane.widthProperty());

        parentPane.getChildren().add(rootPane);
    }

    private void close() {
        parentPane.getChildren().remove(rootPane);
    }

    @FXML
    private void handleCancelButton(ActionEvent event) {
        close();
    }

    @FXML
    private void handleSaveButton(ActionEvent event) {
        // TODO 保存処理
        close();
    }
}
