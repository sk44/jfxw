package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import sk44.jfxw.model.Message;
import sk44.jfxw.model.PathHistoriesCache;
import sk44.jfxw.view.ContentRow;
import sk44.jfxw.view.ContentRowComparator;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class FilerViewController implements Initializable {

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

    private static Path normalizePath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    @FXML
    private AnchorPane rootPane;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane flowPane;
    @FXML
    private Label currentPathLabel;

    private FilerViewController otherFilerViewController;

    private int index = 0;
    private final ObservableList<ContentRow> contents = FXCollections.observableArrayList();
    private final PathHistoriesCache historiesCache = new PathHistoriesCache(HISTORY_BUFFER_SIZE);

    private TextField textField;
    private String searchText;
    private Path currentPath;

    private boolean isBottom() {
        return index + 1 == contents.size();
    }

    private void updateIndex(int index) {
        this.index = index;
    }

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
        Message.debug(event.getCode().name());
        switch (event.getCode()) {
            case DOWN:
            case J:
                // down
                if (isBottom() == false) {
                    clearCursor();
                    updateIndex(this.index + 1);
                    updateCursor();
                }
                break;
            case UP:
            case K:
                // up
                if (index > 0) {
                    clearCursor();
                    updateIndex(this.index - 1);
                    updateCursor();
                }
                break;
            case H:
                Path parent = currentPath.getParent();
                if (parent != null) {
                    moveTo(parent);
                    updateCursor();
                }
                break;
            case L:
                ContentRow currentContent = getCurrentContent();
                if (currentContent.isDirectory()) {
                    moveTo(currentContent.getPath());
                    updateCursor();
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
            case N:
                if (event.isShiftDown()) {
                    // TODO
                } else {
                    searchNext();
                }
                break;
            case Q:
                Platform.exit();
                break;
            case SLASH:
                event.consume();
                initTextField();
                break;
            case TAB:
                otherFilerViewController.focus();
                clearCursor();
                break;
            default:
                break;
        }
    }

    private void clearCursor() {
        getCurrentContent().updateSelected(false);
    }

    private void updateCursor() {
        ContentRow currentContent = getCurrentContent();
        currentContent.updateSelected(true);
        ensureVisible(scrollPane, currentContent);
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

    public void withOtherFileViewController(FilerViewController otherFilerViewController) {
        this.otherFilerViewController = otherFilerViewController;
    }

    void moveTo(Path path) {
        if (currentPath != null) {
            historiesCache.put(currentPath, getCurrentContent().getPath());
        }
        Path normalizedPath = normalizePath(path);
        currentPath = normalizedPath;
        loadFiles();
        if (historiesCache.contains(currentPath)) {
            Path focused = historiesCache.lastFocusedIn(currentPath);
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
        currentPathLabel.setText(currentPath.toString());
    }

    void focus() {
        Platform.runLater(flowPane::requestFocus);
        updateCursor();
    }

    private void loadFiles() {
        clearContents();
        List<ContentRow> rows = new ArrayList<>();
        Path parentPath = currentPath.getParent();
        if (parentPath != null) {
            rows.add(ContentRow.forParent(parentPath, scrollPane.widthProperty()));
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                rows.add(ContentRow.create(entry, scrollPane.widthProperty()));
            }
        } catch (IOException ex) {
            Message.error(ex);
            return;
        }
        rows.stream().sorted(ContentRowComparator.BY_DEFAULT).forEach(row -> addContent(row));
    }

    private void initTextField() {
        // 残っているものがあると永久に消えないのでクリアしておく
        // TODO フォーカスが外れた時点で消すなどする
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
                    searchText = textField.getText();
                    removeTextField();
//                    index = 0;
                    searchNext();
                    break;
                default:
                    break;
            }
        });
        textField.prefWidthProperty().bind(rootPane.widthProperty());
        AnchorPane.setBottomAnchor(textField, 0.0);
        rootPane.getChildren().add(textField);
        textField.requestFocus();
    }

    private void searchNext() {

        if (isBottom()) {
            return;
        }
        System.out.println("search text: " + searchText);

        for (int i = index + 1; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            System.out.println("content: " + content.getName());
            if (content.isNameMatch(searchText)) {
                System.out.println("found: " + content.getName() + "index: " + i);
                clearCursor();
                updateIndex(i);
                updateCursor();
                return;
            }
        }
        System.out.println("not found");
    }

    private void removeTextField() {
        rootPane.getChildren().remove(textField);
        flowPane.requestFocus();
    }

}
