package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ContentRow;

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
    private static final String CLASS_NAME_TEXT_INPUT = "filerTextInput";

    private void ensureVisible(ScrollPane pane, ContentRow node) {

        // http://stackoverflow.com/questions/15840513/javafx-scrollpane-programmatically-moving-the-viewport-centering-content
        double h = pane.getContent().getBoundsInLocal().getHeight();
        double y = (node.getBoundsInParent().getMaxY() + node.getBoundsInParent().getMinY()) / 2.0;
        double v = pane.getViewportBounds().getHeight();
        pane.setVvalue(pane.getVmax() * ((y - 0.5 * v) / (h - v)));
    }

    @FXML
    @Getter
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
    private Consumer<Path> previewImageHandler;

    private boolean isBottom() {
        return index + 1 == contents.size();
    }

    private void updateIndex(int index) {
        if (index < 0) {
            this.index = 0;
            return;
        }
        int size = this.contents.size();
        if (size <= index) {
            this.index = size - 1;
        } else {
            this.index = index;
        }
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case E:
                execute();
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
            case X:
                openByAssociated();
                break;
            case Y:
                yank();
                break;
            case Z:
                // TODO 設定
//                openConfigure();
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
            case ENTER:
                preview();
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
    }

    private void updateCursor() {
        ContentRow currentContent = getCurrentContent();
        currentContent.updateSelected(true);
        ensureVisible(scrollPane, currentContent);
        if (changeCursorListener != null && currentContent.isParent() == false) {
            changeCursorListener.accept(currentContent.getPath());
        }
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
            sortWindowStage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortWindow.fxml"));
            // 先にロードしないと controller が取れない
            Parent root = loader.load();
            SortWindowController controller = loader.getController();
            controller.updateSortOptions(this.filer.getSortType(),
                this.filer.getSortOrder(), this.filer.isSortDirectories());
            controller.setCloseAction(this.sortWindowStage::close);
            controller.setUpdateAction(this.filer::updateSortType);

            sortWindowStage.setScene(new Scene(root, Color.TRANSPARENT));
            sortWindowStage.initModality(Modality.WINDOW_MODAL);
//            sortWindowStage.initModality(Modality.APPLICATION_MODAL);
            sortWindowStage.initOwner(rootPane.getScene().getWindow());
//            sortWindowStage.show();
            sortWindowStage.showAndWait();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    private void openTextField(TextFieldType type) {

        // 残っているものがあると永久に消えないのでクリアしておく
        if (textField != null) {
            removeTextField();
        }
        // スラッシュが入力されてしまうので都度 new する
        textField = new TextField();
        textField.getStyleClass().add(CLASS_NAME_TEXT_INPUT);
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
        Bindings.bindContent(flowPane.getChildren(), contents);
    }

    private void addContent(ContentRow content) {
        contents.add(content);
//        flowPane.getChildren().add(content);
    }

    private void clearContents() {
        contents.clear();
//        flowPane.getChildren().clear();
    }

    public void withFiler(Filer filer) {
        this.filer = filer;
        this.filer.addPreChangeDirectoryObserver(this::preChangeDirectory);
        this.filer.addPostChangeDirectoryObserver(this::directoryChanged);
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
    private void directoryChanged(Path fromDir, Path toDir) {

        if (fromDir != null && fromDir.toString().equals(toDir.toString())) {
            // リロード時に上に戻らないように
            updateIndex(this.index - 1);
        } else if (historiesCache.contains(toDir)) {
            Path focused = historiesCache.lastFocusedIn(toDir);
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
        currentPathLabel.setText(toDir.toString());
        updateCursor();
    }

    public void focus() {
        // runLater でないと効かない
        Platform.runLater(flowPane::requestFocus);
//        flowPane.requestFocus();
        updateCursor();
    }

    private void postEntryLoaded(Path entry, boolean parent, int index) {
        final boolean odd = index % 2 != 0;
        if (parent) {
            addContent(ContentRow.forParent(entry, scrollPane.widthProperty(), odd));
            return;
        }
        addContent(ContentRow.create(entry, scrollPane.widthProperty(), odd));
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

    private void yank() {
        String path = getCurrentContent().getPath().toString();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(path);
        clipboard.setContent(content);
        Message.info("yank: " + path);
    }

    private void preview() {
        if (previewImageHandler == null) {
            return;
        }
        Path path = getCurrentContent().getPath();
        Filer.extensionOf(path)
            .filter(ext -> ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))
            .ifPresent(ext -> {
                previewImageHandler.accept(path);
            });
    }

}
