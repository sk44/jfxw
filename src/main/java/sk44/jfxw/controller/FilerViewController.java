package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ContentRow;
import sk44.jfxw.view.CurrentPathInfoBox;
import sk44.jfxw.view.Fxml;
import sk44.jfxw.view.ModalWindow;
import sk44.jfxw.view.Nodes;
import sk44.jfxw.view.SearchTextField;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class FilerViewController implements Initializable {

    private static final int HISTORY_BUFFER_SIZE = 24;

    private static final String CLASS_NAME_PREVIEW_FILER = "previewFiler";
    private static final String CLASS_NAME_CURRENT_FILER = "currentFiler";

    private static void ensureVisible(ScrollPane scrollPane, ContentRow row) {

        // http://stackoverflow.com/questions/15840513/javafx-scrollpane-programmatically-moving-the-viewport-centering-content
        // 全体の高さ
        double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        // row の位置
        double rowY = (row.getBoundsInParent().getMaxY() + row.getBoundsInParent().getMinY()) / 2.0;
        // 表示範囲の高さ
        double visibleHeight = scrollPane.getViewportBounds().getHeight();
        // TODO 中央位置に合わせてスクロールしてしまうので、上か下に
        scrollPane.setVvalue(scrollPane.getVmax() * ((rowY - 0.5 * visibleHeight) / (contentHeight - visibleHeight)));
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

    private ModalWindow<SortWindowController> sortWindow;
    private ModalWindow<TextFieldWindowController> renameWindow;
    private ModalWindow<TextFieldWindowController> createDirWindow;
    private SearchTextField searchTextField;
    private String searchText;
    private CurrentPathInfoBox currentPathInfoBox;
    @Getter
    private Filer filer;
    @Setter
    private Consumer<Path> changeCursorListener;
    @Setter
    private Runnable changeFocusListener;
    @Setter
    private PathExecutor executionHandler;
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
            case C:
                copy();
                break;
            case D:
                delete();
                break;
            case E:
                execute();
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
            case H:
                this.filer.changeDirectoryToParentDir();
                break;
            case J:
            case DOWN:
                // down
                next();
                break;
            case K:
            case UP:
                // up
                previous();
                break;
            case L:
                ContentRow currentContent = getCurrentContent();
                if (currentContent.isDirectory()) {
                    this.filer.changeDirectoryTo(currentContent.getPath());
                }
                break;
            case M:
                if (event.isShiftDown()) {
                    openCreateDirectoryWindow();
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
            case R:
                if (event.isShiftDown()) {
                    filer.reload();
                } else {
                    openRenameWindow();
                }
                break;
            case S:
                openSortOptionWindow();
                break;
            case X:
                openByAssociated();
                break;
            case Y:
                yank();
                break;
            case Z:
                // TODO 設定画面？
                break;
            case SPACE:
                // Space のデフォルト動作？で勝手にスクロールしてしまうので無効化
                // TODO 全体的にやるべき？
                event.consume();
                getCurrentContent().toggleMark();
                next();
                break;
            case SLASH:
                openSearchTextField();
                break;
            case TAB:
                if (changeFocusListener != null) {
                    changeFocusListener.run();
                }
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
        List<ContentRow> markedRows = collectMarkedRows();
        Map<Path, ContentRow> rowMap = markedRows.stream()
            .collect(Collectors.toMap(ContentRow::getPath, row -> row));
        filer.copy(markedRows.stream().map(ContentRow::getPath).collect(Collectors.toList()),
            this::showConfirmDialog,
            copiedPath -> {
                rowMap.get(copiedPath).toggleMark();
            });
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
        // TODO 移動後に一番上にスクロールしてしまう対策
        List<ContentRow> markedRows = collectMarkedRows();
        Map<Path, ContentRow> rowMap = markedRows.stream()
            .collect(Collectors.toMap(ContentRow::getPath, row -> row));
        filer.move(markedRows.stream().map(ContentRow::getPath).collect(Collectors.toList()),
            this::showConfirmDialog,
            movedPath -> {
                // TODO
                ContentRow moved = rowMap.get(movedPath);
                contents.remove(moved);
            });
        // 要素数が減った結果インデックスが超過してしまう場合
        if (index > contents.size() - 1) {
            index = contents.size() - 1;
        }

        updateCursor();
    }

    private List<ContentRow> collectMarkedRows() {
        return contents.stream()
            .filter(ContentRow::isMarked)
            .collect(Collectors.toList());
    }

    @Deprecated
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

    private void openCreateDirectoryWindow() {
        createDirWindow = new ModalWindow<>();
        createDirWindow.show(Fxml.TEXT_FIELD_WINDOW, rootPane.getScene().getWindow(), (controller) -> {
            controller.updateContent("New directory", "");
            controller.setCloseAction(createDirWindow::close);
            controller.setUpdateAction(dirName -> {
                filer.createDirectory(dirName);
            });
        });
    }

    private void openRenameWindow() {
        if (getCurrentContent().isParent()) {
            return;
        }
        Path target = getCurrentContent().getPath();
        renameWindow = new ModalWindow<>();
        renameWindow.show(Fxml.TEXT_FIELD_WINDOW, rootPane.getScene().getWindow(), (controller) -> {
            controller.updateContent("Rename", target.getFileName().toString());
            controller.setCloseAction(renameWindow::close);
            controller.setUpdateAction(newName -> {
                try {
                    // TODO パス区切り文字が入っている場合とか
                    // TODO ディレクトリとファイルが同名の場合とか
                    Path newPath = target.resolveSibling(newName);
                    if (Files.exists(newPath)) {
                        Message.warn(newPath + " is already exists.");
                        return;
                    }
                    Files.move(target, newPath);
                    Message.info("rename " + target + " to " + newPath);
                    filer.reload();
                } catch (IOException ex) {
                    Message.error(ex);
                }
            });
        });
    }

    private void openSortOptionWindow() {

        sortWindow = new ModalWindow<>();
        sortWindow.show(Fxml.SORT_WINDOW, rootPane.getScene().getWindow(), (controller) -> {
            controller.updateSortOptions(this.filer.getSortType(),
                this.filer.getSortOrder(), this.filer.isSortDirectories());
            controller.setCloseAction(this.sortWindow::close);
            controller.setUpdateAction(this.filer::updateSortType);
        });
    }

    private void openSearchTextField() {
        searchTextField.open((query) -> {
            searchText = query;
            searchNext();
        });
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
        searchTextField = new SearchTextField(rootPane, () -> {
            flowPane.requestFocus();
        });
        currentPathInfoBox = new CurrentPathInfoBox();
        currentPathInfoBox.addTo(rootPane);
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
        currentPathInfoBox.update(toDir);
        updateCursor();
    }

    void focus() {
        // runLater でないと効かない
        Platform.runLater(flowPane::requestFocus);
//        flowPane.requestFocus();
        Nodes.addStyleClassTo(rootPane, CLASS_NAME_CURRENT_FILER);
        updateCursor();
    }

    void onLostFocus() {
        Nodes.removeStyleClassFrom(rootPane, CLASS_NAME_CURRENT_FILER);
    }

    private void postEntryLoaded(Path entry, boolean parent, int index) {
        final boolean odd = index % 2 != 0;
        if (parent) {
            addContent(ContentRow.forParent(entry, scrollPane.widthProperty(), odd));
            return;
        }
        addContent(ContentRow.create(entry, scrollPane.widthProperty(), odd));
    }

    void searchNext() {

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
        currentImage().ifPresent(image -> {
            previewImageHandler.accept(image);
            Nodes.addStyleClassTo(flowPane, CLASS_NAME_PREVIEW_FILER);
        });
    }

    private Optional<Path> currentImage() {
        Path path = getCurrentContent().getPath();
        return Filer.extensionOf(path)
            .filter(ext -> ext.equalsIgnoreCase("jpg")
                || ext.equalsIgnoreCase("jpeg")
                || ext.equalsIgnoreCase("png")
                || ext.equalsIgnoreCase("gif"))
            .map(ext -> path);
    }

    public Optional<Path> nextImage() {
        next();
        return currentImage();
    }

    public Optional<Path> previousImage() {
        previous();
        return currentImage();
    }

    public void endPreviewImage() {
        Nodes.removeStyleClassFrom(flowPane, CLASS_NAME_PREVIEW_FILER);
        focus();
    }
}
