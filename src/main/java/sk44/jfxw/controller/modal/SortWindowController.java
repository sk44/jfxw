/*
 *
 *
 *
 */
package sk44.jfxw.controller.modal;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;
import sk44.jfxw.model.PathSortOrder;
import sk44.jfxw.model.PathSortType;
import sk44.jfxw.view.Nodes;

/**
 * ソート種別選択ウィンドウコントローラー。
 *
 * @author sk
 */
public class SortWindowController extends ModalWindowController<Void> implements Initializable {

    private static final String CURRENT_SORT_OPTION_CLASS_NAME = "currentSortOption";

    @Override
    public Void getResult() {
        return null;
    }

    @FunctionalInterface
    public interface UpdateAction {

        void update(PathSortType sortType, PathSortOrder sortOrder, boolean sortDir);
    }

    @FXML
    private Pane rootPane;

    @FXML
    private Label fileNameLabel;
    @FXML
    private Label fileSizeLabel;
    @FXML
    private Label lastModifiedLabel;
    @FXML
    private Label ascLabel;
    @FXML
    private Label descLabel;
    @FXML
    private Label sortDirLabel;
    private boolean sortDir;

    private PathSortType currentSortType;
    private PathSortOrder sortOrder;
    @Setter
    private UpdateAction updateAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // runLater しないとフォーカスが効かない
        Platform.runLater(() -> {
            this.rootPane.requestFocus();
        });
        this.rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCommandKeyPressed);
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {

        switch (event.getCode()) {
            case M:
                updateCurrentSortType(PathSortType.LAST_MODIFIED, this.sortOrder);
                break;
            case N:
                updateCurrentSortType(PathSortType.FILE_NAME, this.sortOrder);
                break;
            case S:
                updateCurrentSortType(PathSortType.FILE_SIZE, this.sortOrder);
                break;
            case D:
            case RIGHT:
                // desc
                updateCurrentSortType(this.currentSortType, PathSortOrder.DESC);
                break;
            case A:
            case LEFT:
                // asc
                updateCurrentSortType(this.currentSortType, PathSortOrder.ASC);
                break;
            case R:
                // toggle
                this.updateSortDir(this.sortDir == false);
                break;
            case ESCAPE:
                close();
                break;
            case ENTER:
                updateAction.update(this.currentSortType, this.sortOrder, this.sortDir);
                close();
                break;
            default:
                break;
        }
    }

    public void updateSortOptions(PathSortType sortType, PathSortOrder sortOrder, boolean sortDirectories) {
        this.updateSortDir(sortDirectories);
        this.updateCurrentSortType(sortType, sortOrder);
    }

    private void updateSortDir(boolean sortDirectories) {
        if (sortDirectories) {
            Nodes.addStyleClassTo(this.sortDirLabel, CURRENT_SORT_OPTION_CLASS_NAME);
        } else {
            this.sortDirLabel.getStyleClass().remove(CURRENT_SORT_OPTION_CLASS_NAME);
        }
        this.sortDir = sortDirectories;
    }

    private void updateCurrentSortType(PathSortType currentSortType, PathSortOrder sortOrder) {
        if (this.currentSortType != null) {
            labelOfSortType(this.currentSortType).getStyleClass().remove(CURRENT_SORT_OPTION_CLASS_NAME);
        }
        labelOfSortType(currentSortType).getStyleClass().add(CURRENT_SORT_OPTION_CLASS_NAME);
        this.currentSortType = currentSortType;
        this.sortOrder = sortOrder;
        if (this.sortOrder == PathSortOrder.ASC) {
            Nodes.addStyleClassTo(this.ascLabel, CURRENT_SORT_OPTION_CLASS_NAME);
            this.descLabel.getStyleClass().remove(CURRENT_SORT_OPTION_CLASS_NAME);
        } else {
            this.ascLabel.getStyleClass().remove(CURRENT_SORT_OPTION_CLASS_NAME);
            Nodes.addStyleClassTo(this.descLabel, CURRENT_SORT_OPTION_CLASS_NAME);
        }
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
