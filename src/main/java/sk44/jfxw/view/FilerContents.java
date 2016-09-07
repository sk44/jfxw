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
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
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

    private final ObservableList<ContentRow> contents = FXCollections.observableArrayList();
    private int index = 0;
    @Setter
    private Filer filer;

    public void bindContentWith(ObservableList<Node> list) {
        Bindings.bindContent(list, contents);
    }

    public int size() {
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
        clearCursor();
        updateIndex(this.index - 1);
    }

    public void updateIndexToDown() {
        if (isBottom()) {
            return;
        }
        clearCursor();
        updateIndex(this.index + 1);
    }

    public void updateIndex(int index) {
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
        this.filer.addToCache(getCurrentContentPath());
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
            index = contents.size() - 1;
        }
        return contents.get(index);
    }

    public Path getCurrentContentPath() {
        return getCurrentContent().getPath();
    }

    public void clearCursor() {
        if (size() - 1 < index) {
            return;
        }
        getCurrentContent().updateSelected(false);
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

    public void removePathIfContains(Path path) {
        // TODO 親ディレクトリが消えた時の対応（親階層に移動するとか）
        if (filer.getCurrentDir().equals(path.getParent()) == false) {
            return;
        }
        findRowByPath(path).ifPresent(row -> {
            contents.remove(row);
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

    public void searchNext(String searchText, boolean keepCurrent, Runnable onFound) {

        if (isBottom()) {
            return;
        }
        Message.debug("search text: " + searchText);

        final int addIndex = keepCurrent ? 0 : 1;
        for (int i = index + addIndex; i < contents.size(); i++) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                clearCursor();
                updateIndex(i);
                onFound.run();
                return;
            }
        }
        Message.debug("not found.");
    }

    public void searchPrevious(String searchText, Runnable onFound) {
        if (isTop()) {
            return;
        }
        Message.debug("search text: " + searchText);

        for (int i = index - 1; i >= 0; i--) {
            ContentRow content = contents.get(i);
            if (content.isNameMatch(searchText)) {
                Message.debug("found: " + content.getName());
                clearCursor();
                updateIndex(i);
                onFound.run();
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
