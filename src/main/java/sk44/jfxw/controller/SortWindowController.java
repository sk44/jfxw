/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;
import sk44.jfxw.model.PathSortType;
import sk44.jfxw.model.message.Message;

/**
 * ソート種別選択ウィンドウコントローラー。
 *
 * @author sk
 */
public class SortWindowController implements Initializable {

    private static final String CURRENT_SORT_TYPE_CLASS_NAME = "currentSortType";

    @FXML
    private Pane rootPane;
    @Deprecated
    private Pane parentPane;

    @FXML
    private Label fileNameLabel;
    @FXML
    private Label fileSizeLabel;
    @FXML
    private Label lastModifiedLabel;

    private PathSortType currentSortType;
    @Setter
    private Runnable closeAction;
    @Setter
    private Consumer<PathSortType> updateAction;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
//        this.rootPane.requestFocus();
        this.rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCommandKeyPressed);
    }

    @Deprecated
    void showOn(Pane parentPane) {
        // TODO: 同じコード
        this.parentPane = parentPane;
        rootPane.prefHeightProperty().bind(parentPane.heightProperty());
        rootPane.prefWidthProperty().bind(parentPane.widthProperty());
        parentPane.getChildren().add(rootPane);
    }

    @Deprecated
    private void close() {
        parentPane.getChildren().remove(rootPane);
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
        Message.debug("sort window: " + event.getCode().toString());
        switch (event.getCode()) {
            case M:
                setCurrentSortType(PathSortType.LAST_MODIFIED);
                break;
            case N:
                setCurrentSortType(PathSortType.FILE_NAME);
                break;
            case S:
                setCurrentSortType(PathSortType.FILE_SIZE);
                break;
            case ESCAPE:
                closeAction.run();
                break;
            case ENTER:
                updateAction.accept(this.currentSortType);
                closeAction.run();
                break;
            default:
                break;
        }
    }

    public void setCurrentSortType(PathSortType currentSortType) {
        if (this.currentSortType != null) {
            labelOfSortType(this.currentSortType).getStyleClass().remove(CURRENT_SORT_TYPE_CLASS_NAME);
        }
        labelOfSortType(currentSortType).getStyleClass().add(CURRENT_SORT_TYPE_CLASS_NAME);
        this.currentSortType = currentSortType;
    }

    private Label labelOfSortType(PathSortType sortType) {
        switch (sortType) {
            case FILE_NAME:
                return fileNameLabel;
            case FILE_SIZE:
                return fileSizeLabel;
            case LAST_MODIFIED:
                return lastModifiedLabel;
            default:
                throw new IllegalArgumentException("sortType: " + sortType + " does not defined.");
        }
    }
}
