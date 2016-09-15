/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.Setter;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class FilerContents {

    private static final double ROW_HEIGHT = 7.5;

    private static void ensureVisible(ScrollPane scrollPane, ContentRow row) {

        // http://stackoverflow.com/questions/15840513/javafx-scrollpane-programmatically-moving-the-viewport-centering-content
        Platform.runLater(() -> {
            // 全体の高さ
            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            // row の位置
            double rowY = (row.getBoundsInParent().getMaxY() + row.getBoundsInParent().getMinY()) / 2.0;
            // 表示範囲の高さ
            double visibleHeight = scrollPane.getViewportBounds().getHeight();
            Bounds b = scrollPane.getViewportBounds();
            if (rowY + b.getMinY() < 0) {
                // 上へスクロールが必要
                scrollPane.setVvalue(scrollPane.getVmax() * ((rowY - ROW_HEIGHT) / (contentHeight - visibleHeight)));
            } else if (rowY + b.getMinY() > visibleHeight) {
                // 下へスクロールが必要
                scrollPane.setVvalue(scrollPane.getVmax() * ((rowY - visibleHeight + ROW_HEIGHT) / (contentHeight - visibleHeight)));
            }
        });
    }

    // TODO ソートにはこのへん使えそう？
    // http://docs.oracle.com/javase/jp/8/javafx/api/javafx/collections/transformation/SortedList.html
    private final ObservableList<ContentRow> contents = FXCollections.observableArrayList();
    private int index = 0;
    @Setter
    private Filer filer;
    @Setter
    private ScrollPane scrollPane;

    public void bindContentWith(ObservableList<Node> list) {
        Bindings.bindContent(list, contents);
    }

    private int size() {
        return contents.size();
    }

    public void updateIndexToBottom() {
        updateIndex(contents.size() - 1);
    }

    public void updateIndexToTop() {
        updateIndex(0);
    }

    public void updateIndexToUp() {
        if (isTop()) {
            return;
        }
        updateIndex(this.index - 1);
    }

    public void updateIndexToDown() {
        if (isBottom()) {
            return;
        }
        updateIndex(this.index + 1);
    }

    private void fixIndex() {
        updateIndex(index);
    }

    public void onDirectoryChangedTo(Path toDir) {
        int focusIndex = this.filer.lastFocusedPathIn(toDir)
            .map(focused -> indexOfPath(focused).orElse(0))
            .orElse(0);
        updateIndex(focusIndex);
    }

    private void updateIndex(int newIndex) {
        if (newIndex < 0) {
            Message.warn("cannot update index to: " + newIndex);
            this.index = 0;
        } else {
            int size = size();
            if (size - 1 < this.index) {
                // ディレクトリ移動時に出る
                Message.info("skip clear: " + this.index + ", size: " + size);
            } else {
                Message.info("clear index: " + this.index);
                getCurrentContent().updateSelected(false);
            }

            if (size <= newIndex) {
                this.index = size - 1;
            } else {
                this.index = newIndex;
            }
        }
        Message.info("index updated: " + this.index);
        ContentRow currentContent = getCurrentContent();
        this.filer.addToCache(currentContent.getPath());
        currentContent.updateSelected(true);
        ensureVisible(scrollPane, currentContent);
        filer.onCursorChangedTo(currentContent.getPath());
    }

    public boolean isTop() {
        return index == 0;
    }

    public boolean isBottom() {
        return index + 1 == size();
    }

    public List<Path> collectMarkedPathes() {
        return contents
            .stream()
            .filter(content -> content.isMarked())
            .map(content -> content.getPath())
            .collect(Collectors.toList());
    }

    public ContentRow getCurrentContent() {
        if (contents.size() <= index) {
            Message.info("index " + index + " is out of bounds.");
            index = contents.size() - 1;
        }
        return contents.get(index);
    }

    public Path getCurrentContentPath() {
        return getCurrentContent().getPath();
    }

    public void add(ContentRow content) {
        contents.add(content);
    }

    public void removeMark(Path path) {
        Optional<ContentRow> content = findRowByPath(path);
        content.ifPresent(c -> {
            c.updateMark(false);
        });
    }

    private Optional<ContentRow> findRowByPath(Path path) {
        // TODO map とかでもっておいたほうが速そう
        return contents.stream().filter(e -> e.getPath().equals(path)).findAny();
    }

    public void onDirectoryDeleted(Path deletedDir) {
        // 親ディレクトリが消えた場合
        if (filer.getCurrentDir().startsWith(deletedDir)) {
            filer.changeDirectoryToExistingParentDir();
            return;
        }
        removePathIfContains(deletedDir);
    }

    public void removePathIfContains(Path path) {
        if (filer.getCurrentDir().equals(path.getParent()) == false) {
            return;
        }
        findRowByPath(path).ifPresent(row -> {
            contents.remove(row);
            // FIXME 削除対象業以降にカーソルがあるとカーソル表示がだぶる
            // index と selected で二重管理になってるのがうまくない
            fixIndex();
        });
    }

    public void clear() {
        if (contents.isEmpty()) {
            return;
        }
        contents.clear();
    }

    public OptionalInt indexOfPath(Path path) {
        for (int i = 0; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            if (content.getPath().equals(path)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public Optional<Path> currentImage() {
        Path path = getCurrentContentPath();
        return Filer.extensionOf(path)
            .filter(ext -> ext.equalsIgnoreCase("jpg")
                || ext.equalsIgnoreCase("jpeg")
                || ext.equalsIgnoreCase("png")
                || ext.equalsIgnoreCase("gif"))
            .map(ext -> path);
    }

    public void searchNext(String searchText, boolean keepCurrent) {

        if (isBottom()) {
            return;
        }
        Message.debug("search text: " + searchText);

        final int addIndex = keepCurrent ? 0 : 1;
        for (int i = index + addIndex; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                updateIndex(i);
                return;
            }
        }
        Message.debug("not found.");
    }

    public void searchPrevious(String searchText) {
        if (isTop()) {
            return;
        }
        Message.debug("search text: " + searchText);

        for (int i = index - 1; i >= 0; i--) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                updateIndex(i);
                return;
            }
        }
        Message.debug("not found.");

    }

    public void yankCurrentContent() {
        String path = getCurrentContentPath().toString();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(path);
        clipboard.setContent(content);
        Message.info("yank: " + path);
    }
}
