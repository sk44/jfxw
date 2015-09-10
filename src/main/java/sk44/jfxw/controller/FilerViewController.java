package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ContentRow;
import sk44.jfxw.view.SortOptionPane;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class FilerViewController implements Initializable {

    private enum TextFieldType {

        SEARCH {

                @Override
                public void handleEnter(FilerViewController controller, String text) {
                    controller.searchText = text;
                    controller.searchNext();
                }

            }, CREATE_DIR {

                @Override
                public void handleEnter(FilerViewController controller, String text) {
                    controller.filer.createDirectory(text);
                }

            };

        public abstract void handleEnter(FilerViewController controller, String text);
    }

    private static final double CONTENT_HEIGHT = 16;
    private static final int HISTORY_BUFFER_SIZE = 24;

    private static void ensureVisible(ScrollPane pane, Node node) {
        double height = pane.getContent().getBoundsInLocal().getHeight();

        double y = node.getBoundsInParent().getMaxY();
        // content の高さ分、移動量を調節
        double range = y / height;
        y = y - CONTENT_HEIGHT * (1 - range);

        // scrolling values range from 0 to 1
        pane.setVvalue(y / height);
    }

    @FXML
    private AnchorPane rootPane;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane flowPane;
    @FXML
    private Label currentPathLabel;

    private int index = 0;
    private final ObservableList<ContentRow> contents = FXCollections.observableArrayList();
    private final PathHistoriesCache historiesCache = new PathHistoriesCache(HISTORY_BUFFER_SIZE);

    private Stage sortWindowStage;
    private SortOptionPane sortOptionPane;
    private TextField textField;
    private String searchText;
    @Getter
    private Filer filer;
    @Setter
    private Consumer<Path> changeCursorListener;
    @Setter
    private Runnable changeFocusListener;
    @Setter
    private PathExecutor executionHandler;
    @Setter
    private Runnable openConfigureHandler;
    @Setter
    @Deprecated
    private Runnable openSortHandler;

    private boolean isBottom() {
        return index + 1 == contents.size();
    }

    private void updateIndex(int index) {
        Message.debug("index update: " + this.index + " to " + index);
        this.index = index;
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case E:
                execute();
                break;
            case X:
                openByAssociated();
                break;
            case Z:
                openConfigure();
                break;
            case S:
                openSortOption();
                break;
            case DOWN:
            case J:
                // down
                next();
                break;
            case UP:
            case K:
                // up
                previous();
                break;
            case H:
                this.filer.changeDirectoryToParentDir();
                break;
            case L:
                ContentRow currentContent = getCurrentContent();
                if (currentContent.isDirectory()) {
                    this.filer.changeDirectoryTo(currentContent.getPath());
                }
                break;
            case G:
                clearCursor();
                if (event.isShiftDown()) {
                    updateIndex(contents.size() - 1);
                } else {
                    updateIndex(0);
                }
                updateCursor();
                break;
            case C:
                copy();
                break;
            case D:
                delete();
                break;
            case M:
                if (event.isShiftDown()) {
                    openTextField(TextFieldType.CREATE_DIR);
                } else {
                    move();
                }
                break;
            case N:
                if (event.isShiftDown()) {
                    searchPrevious();
                } else {
                    searchNext();
                }
                break;
            case O:
                if (event.isShiftDown()) {
                    this.filer.syncCurrentDirectoryToOther();
                } else {
                    this.filer.syncCurrentDirectoryFromOther();
                }
                break;
            case Q:
                Platform.exit();
                break;
            case SPACE:
                getCurrentContent().toggleMark();
                next();
                break;
            case SLASH:
                openTextField(TextFieldType.SEARCH);
                break;
            case TAB:
                if (changeFocusListener != null) {
                    changeFocusListener.run();
                }
                // TODO どっちにフォーカスがあるかわからなくなるので見た目をどうにかしたい
//                clearCursor();
                break;
            default:
                break;
        }
    }

    private void execute() {
        Path pathOnCursor = getCurrentContent().getPath();
        if (executionHandler != null) {
            if (executionHandler.tryExecute(pathOnCursor) == false) {
                // TODO たらい回しにするかんじで
            }
        }
    }

    private void next() {
        if (isBottom()) {
            return;
        }
        clearCursor();
        updateIndex(this.index + 1);
        updateCursor();
    }

    private void previous() {
        if (index > 0) {
            clearCursor();
            updateIndex(this.index - 1);
            updateCursor();
        }
    }

    private void copy() {
        filer.copy(collectMarkedPathes(), this::showConfirmDialog);
        updateCursor();
    }

    private boolean showConfirmDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getDialogPane().setContentText(message);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private void delete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getDialogPane().setContentText("Are you sure?");
        alert.showAndWait()
            .filter(response -> response == ButtonType.OK)
            .ifPresent(response -> {
                filer.delete(collectMarkedPathes());
                updateCursor();
            });
    }

    private void move() {
        filer.move(collectMarkedPathes());
        updateCursor();
    }

    private List<Path> collectMarkedPathes() {
        return contents
            .stream()
            .filter(content -> content.isMarked())
            .map(content -> content.getPath())
            .collect(Collectors.toList());
    }

    private void clearCursor() {
        getCurrentContent().updateSelected(false);
        Message.debug("cursor clear at index: " + index);
    }

    private void updateCursor() {
        ContentRow currentContent = getCurrentContent();
        currentContent.updateSelected(true);
        ensureVisible(scrollPane, currentContent);
        if (changeCursorListener != null && currentContent.isParent() == false) {
            changeCursorListener.accept(currentContent.getPath());
        }
        Message.debug("cursor updated at index: " + index);
    }

    private void openConfigure() {
        if (openConfigureHandler != null) {
            openConfigureHandler.run();
        } else {
            Message.warn("open configuration handler not set.");
        }
    }

    private void openSortOption() {

        // http://stackoverflow.com/questions/10486731/how-to-create-a-modal-window-in-javafx-2-1
        // http://nodamushi.hatenablog.com/entry/20130910/1378784711
        try {
            sortWindowStage = new Stage();
            // TODO 表示位置を調整. モニターの絶対座標になるもよう
//            sortWindowStage.setX(rootPane.getScaleX() + 10);
//            sortWindowStage.setY(20);
            sortWindowStage.initStyle(StageStyle.UTILITY);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortWindow.fxml"));
            // 先にロードしないと controller が取れない
            Parent root = loader.load();
            SortWindowController controller = loader.getController();
            controller.setCurrentSortType(this.filer.getSortType());
            controller.setCloseAction(this.sortWindowStage::close);
            controller.setUpdateAction(this.filer::updateSortType);

            sortWindowStage.setScene(new Scene(root));
            sortWindowStage.initModality(Modality.WINDOW_MODAL);
//            sortWindowStage.initModality(Modality.APPLICATION_MODAL);
            sortWindowStage.initOwner(rootPane.getScene().getWindow());
//            sortWindowStage.show();
            sortWindowStage.showAndWait();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    @Deprecated
    private void openSortOption2() {
        if (sortOptionPane != null) {
            removeSortOptionPane();
        }
        sortOptionPane = new SortOptionPane((filer.getSortType()));
        sortOptionPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // TODO view 内に移動？
            switch (e.getCode()) {
                case ESCAPE:
                    removeSortOptionPane();
                    break;
                case ENTER:
                    filer.updateSortType(sortOptionPane.getSelectedSortType());
                    removeSortOptionPane();
                    break;
                default:
                    sortOptionPane.handleKeyEvent(e);
                    break;
            }
        });
        sortOptionPane.focusedProperty().addListener((value, oldValue, newValue) -> {
            if (newValue == false) {
                removeSortOptionPane();
            }
        });
        sortOptionPane.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.6));
        // TODO bottom がきかない？
        AnchorPane.setBottomAnchor(sortOptionPane, 30.0);
        AnchorPane.setLeftAnchor(sortOptionPane, 30.0);
        rootPane.getChildren().add(sortOptionPane);
        Platform.runLater(sortOptionPane::requestFocus);
//        sortOptionPane.requestFocus();
    }

    private void openTextField(TextFieldType type) {

        // 残っているものがあると永久に消えないのでクリアしておく
        if (textField != null) {
            removeTextField();
        }
        // スラッシュが入力されてしまうので都度 new する
        textField = new TextField();
        textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE:
                    removeTextField();
                    break;
                case ENTER:
                    String text = textField.getText();
                    if (text != null && text.length() > 0) {
                        type.handleEnter(this, text);
                        removeTextField();
                    }
                    break;
                default:
                    break;
            }
        });
        // フォーカスアウトで消す
        textField.focusedProperty().addListener((value, oldValue, newValue) -> {
            if (newValue == false) {
                removeTextField();
            }
        });
        textField.prefWidthProperty().bind(rootPane.widthProperty());
        AnchorPane.setBottomAnchor(textField, 0.0);
        rootPane.getChildren().add(textField);
        textField.requestFocus();
    }

    private ContentRow getCurrentContent() {
        return contents.get(index);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // bind しないと、ウィンドウ幅を変更したとき表示がズレる
//        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        flowPane.prefWidthProperty().bind(scrollPane.widthProperty());
    }

    private void addContent(ContentRow content) {
        contents.add(content);
        flowPane.getChildren().add(content);
    }

    private void clearContents() {
        contents.clear();
        flowPane.getChildren().clear();
    }

    public void withFiler(Filer filer) {
        this.filer = filer;
        this.filer.addPreChangeDirectoryObserver(this::preChangeDirectory);
        this.filer.addPostChangeDirectoryObserver(this::postChangeDirectoryTo);
        this.filer.addPostEntryLoadedObserver(this::postEntryLoaded);
    }

    private void preChangeDirectory(Path previousPath) {
        if (contents.isEmpty()) {
            return;
        }
        historiesCache.put(previousPath, getCurrentContent().getPath());
        clearContents();
    }

    // TODO notification
    private void postChangeDirectoryTo(Path path) {
        if (historiesCache.contains(path)) {
            Path focused = historiesCache.lastFocusedIn(path);
            boolean found = false;
            for (int i = 0; i < contents.size(); i++) {
                ContentRow content = contents.get(i);
                if (content.getPath().equals(focused)) {
                    updateIndex(i);
                    found = true;
                    break;
                }
            }
            if (found == false) {
                updateIndex(0);
            }
        } else {
            updateIndex(0);
        }
        // TODO バインド
        currentPathLabel.setText(path.toString());
        updateCursor();
    }

    void focus() {
        Platform.runLater(flowPane::requestFocus);
        updateCursor();
    }

    private void postEntryLoaded(Path entry, boolean parent) {
        if (parent) {
            addContent(ContentRow.forParent(entry, scrollPane.widthProperty()));
            return;
        }
        addContent(ContentRow.create(entry, scrollPane.widthProperty()));
    }

    private void searchNext() {

        if (isBottom()) {
            return;
        }
        Message.debug("search text: " + searchText);

        for (int i = index + 1; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                clearCursor();
                updateIndex(i);
                updateCursor();
                return;
            }
        }
        Message.info("not found.");
    }

    private void searchPrevious() {
        if (index == 0) {
            return;
        }
        Message.debug("search text: " + searchText);

        for (int i = index - 1; i >= 0; i--) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                clearCursor();
                updateIndex(i);
                updateCursor();
                return;
            }
        }
        Message.info("not found.");

    }

    private void removeSortOptionPane() {
        rootPane.getChildren().remove(sortOptionPane);
        flowPane.requestFocus();
    }

    private void removeTextField() {
        rootPane.getChildren().remove(textField);
        flowPane.requestFocus();
    }

    private void openByAssociated() {
        Path onCursor = getCurrentContent().getPath();
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getAssociatedCommandFor(onCursor)
            .ifPresent(command -> {
                try {
                    Message.info("exec: " + command);
                    new ProcessBuilder(command).start();
                } catch (IOException ex) {
                    Message.error(ex);
                }
            });
    }
}
