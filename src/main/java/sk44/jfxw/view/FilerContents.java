/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
import sk44.jfxw.model.ModelLocator;
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
    private ContentRow selectedRow;
    @Setter
    private Filer filer;
    @Setter
    private ScrollPane scrollPane;

    public void bindContentWith(ObservableList<Node> list) {
        Bindings.bindContent(list, contents);
    }

    public void updateIndexToBottom() {
        updateSelected(contents.get(contents.size() - 1));
    }

    public void updateIndexToTop() {
        updateSelected(contents.get(0));
    }

    private int selectedIndex() {
        return contents.indexOf(selectedRow);
    }

    public void updateIndexToUp() {
        if (isTop()) {
            return;
        }
        updateSelected(contents.get(selectedIndex() - 1));
    }

    public void updateIndexToDown() {
        if (isBottom()) {
            return;
        }
        updateSelected(contents.get(selectedIndex() + 1));
    }

    public void onDirectoryChangedTo(Path toDir) {
        ContentRow newSelectedRow = this.filer.lastFocusedPathIn(toDir)
            .flatMap(path -> findRowByPath(path))
            .orElse(contents.get(0));
        updateSelected(newSelectedRow);
    }

    private void updateSelected(ContentRow selected) {
        if (selectedRow != null) {
            selectedRow.updateSelected(false);
        }
        selectedRow = selected;
        selectedRow.updateSelected(true);
        this.filer.addToCache(selectedRow.getPath());
        ensureVisible(scrollPane, selectedRow);
        filer.onCursorChangedTo(selectedRow.getPath());
    }

    public boolean isTop() {
        return selectedIndex() == 0;
    }

    public boolean isBottom() {
        return selectedIndex() + 1 == contents.size();
    }

    public List<Path> collectMarkedPathes() {
        return contents
            .stream()
            .filter(content -> content.isMarked())
            .map(content -> content.getPath())
            .collect(Collectors.toList());
    }

    public ContentRow getCurrentContent() {
        return selectedRow;
    }

    public Path getCurrentContentPath() {
        return getCurrentContent().getPath();
    }

    public void openExternalEditor() {
        Path onCursor = getCurrentContentPath();
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getEditorProcessFor(onCursor)
            .execute();
    }

    public void openExternalEditorFor(String textFileName) {
        Path newFile = filer.getCurrentDir().resolve(textFileName);
        if (Files.exists(newFile) && Files.isRegularFile(newFile)) {
            Message.warn(newFile + " is already exists.");
            return;
        }
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getEditorProcessFor(newFile)
            .execute();
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
//            fixIndex();
        });
    }

    public void clear() {
        if (contents.isEmpty()) {
            return;
        }
        contents.clear();
    }

    private Optional<ContentRow> findRowByPath(Path path) {
        // TODO map とかでもっておいたほうが速そう
        return contents.stream().filter(e -> e.getPath().equals(path)).findAny();
    }

    public void preview() {
        Path path = getCurrentContentPath();
        if (isImage(path)) {
            filer.previewImage(path);
            return;
        }
        filer.previewText(path);
    }

    public Optional<Path> currentImage() {
        Path path = getCurrentContentPath();
        return isImage(path) ? Optional.of(path) : Optional.empty();
    }

    private boolean isImage(Path path) {
        return Filer.extensionOf(path)
            .filter(ext -> ext.equalsIgnoreCase("jpg")
            || ext.equalsIgnoreCase("jpeg")
            || ext.equalsIgnoreCase("png")
            || ext.equalsIgnoreCase("gif"))
            .isPresent();
    }

    public void searchNext(String searchText, boolean keepCurrent) {

        if (isBottom()) {
            return;
        }
        Message.debug("search text: " + searchText);

        final int addIndex = keepCurrent ? 0 : 1;
        for (int i = selectedIndex() + addIndex; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                updateSelected(content);
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

        for (int i = selectedIndex() - 1; i >= 0; i--) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                updateSelected(content);
                return;
            }
        }
        Message.debug("not found.");

    }

    public void extractArchive() {
        filer.extractArchiveToOtherFiler(getCurrentContentPath());
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
