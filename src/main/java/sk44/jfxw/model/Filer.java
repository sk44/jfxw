/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.fs.DeleteDirectoryVisitor;
import sk44.jfxw.model.fs.OverwriteFileConfirmer;
import sk44.jfxw.model.fs.PathHelper;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class Filer {

    @FunctionalInterface
    public interface PreChangeDirectoryListener {

        void changeDirectoryFrom(Path previousDir);
    }

    @FunctionalInterface
    public interface PostChangeDirectoryListener {

        void directoryChanged(Path fromDir, Path toDir);
    }

    @FunctionalInterface
    public interface PathEntryLoadedListener {

        void postLoad(Path entry, boolean parent, int index);
    }

    @FunctionalInterface
    public interface CursorChangedListener {

        void changedTo(Path path);
    }

    @FunctionalInterface
    public interface ToggleFilerFocusListener {

        void toggle();
    }

    @FunctionalInterface
    public interface PreviewImageListener {

        void preview(Path imagePath);
    }

    private static final int HISTORY_BUFFER_SIZE = 24;

    private static Path normalizePath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    public static Optional<String> extensionOf(Path path) {

        String fileName = path.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return Optional.of(fileName.substring(i + 1));
        }
        return Optional.empty();
    }

    public Filer(String initialPath, PathSortType sortType, PathSortOrder sortOrder, boolean sortDirectories) {
        this.currentDir = normalizePath(Paths.get(initialPath));
        this.sortType = sortType;
        this.sortOrder = sortOrder;
        this.sortDirectories = sortDirectories;
    }

    private final EventSource<ToggleFilerFocusListener> toggleFilerFocusEvent = new EventSource<>();
    private final EventSource<PreChangeDirectoryListener> preChangeDirectoryEvent = new EventSource<>();
    private final EventSource<PostChangeDirectoryListener> postChangeDirectoryEvent = new EventSource<>();
    private final EventSource<PathEntryLoadedListener> postEntryLoadedEvent = new EventSource<>();
    private final EventSource<CursorChangedListener> cursorChangedEvent = new EventSource<>();
    private final EventSource<PreviewImageListener> previewImageEvent = new EventSource<>();

    private final PathHistoriesCache historiesCache = new PathHistoriesCache(HISTORY_BUFFER_SIZE);

    @Getter
    private PathSortType sortType;
    @Getter
    private PathSortOrder sortOrder;
    @Getter
    private boolean sortDirectories;

    @Getter
    private Path currentDir;

    @Setter
    private Filer otherFiler;

    public void addListenerToToggleFilerFocusEvent(ToggleFilerFocusListener listener) {
        toggleFilerFocusEvent.addListener(listener);
    }

    public void addListenerToCursorChangedEvent(CursorChangedListener listener) {
        cursorChangedEvent.addListener(listener);
    }

    public void addListenerToPreviewImageEvent(PreviewImageListener listener) {
        previewImageEvent.addListener(listener);
    }

    // TODO remove の仕組みが必要かなー
    public void addListenerToPreChangeDirectoryEvent(PreChangeDirectoryListener listener) {
        this.preChangeDirectoryEvent.addListener(listener);
    }

    public void addListenerToPostChangeDirectoryEvent(PostChangeDirectoryListener listener) {
        this.postChangeDirectoryEvent.addListener(listener);
    }

    public void addListenerToPostEntryLoadedEvent(PathEntryLoadedListener listener) {
        this.postEntryLoadedEvent.addListener(listener);
    }

    public void toggleFocus() {
        toggleFilerFocusEvent.raiseEvent(ToggleFilerFocusListener::toggle);
    }

    public void onCursorChangedTo(Path path) {
        cursorChangedEvent.raiseEvent(listener -> listener.changedTo(path));
    }

    public void previewImage(Path imagePath) {
        previewImageEvent.raiseEvent(listener -> listener.preview(imagePath));
    }

    public void reload() {
        changeDirectoryTo(currentDir);
    }

    public void changeDirectoryToInitPath() {
        changeDirectoryTo(currentDir);
    }

    public void changeDirectoryToParentDir() {
        Path parent = currentDir.getParent();
        if (parent != null) {
            changeDirectoryTo(parent);
        }
    }

    public void addToCache(Path currentPath) {
        historiesCache.put(currentDir, currentPath);
    }

    public void changeDirectoryTo(Path dir) {
        if (Files.isDirectory(dir) == false) {
            Message.warn(dir + " is not a directory. change directory cancelled.");
            return;
        }
        Path fromDir = this.currentDir;
        this.preChangeDirectoryEvent.raiseEvent(listener -> listener.changeDirectoryFrom(fromDir));
        this.currentDir = normalizePath(dir);
        collectEntries();
        this.postChangeDirectoryEvent.raiseEvent(listener -> listener.directoryChanged(fromDir, this.currentDir));
    }

    public Optional<Path> lastFocusedPathIn(Path dir) {
        if (historiesCache.contains(dir)) {
            return Optional.of(historiesCache.lastFocusedIn(dir));
        }
        return Optional.empty();
    }

    public void createDirectory(String newDirectoryName) {
        // TODO
        Path newDir = currentDir.resolve(newDirectoryName);
        if (Files.exists(newDir) && Files.isDirectory(newDir)) {
            Message.info(newDir + " is already exists.");
            return;
        }
        try {
            Files.createDirectory(newDir);
            Message.info(newDir + " created.");
            changeDirectoryTo(newDir);
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    public void syncCurrentDirectoryFromOther() {
        changeDirectoryTo(otherFiler.getCurrentDir());
    }

    public void syncCurrentDirectoryToOther() {
        otherFiler.changeDirectoryTo(getCurrentDir());
    }

    public void updateSortType(PathSortType sortType, PathSortOrder sortOrder, boolean sortDirectories) {
        this.sortType = sortType;
        this.sortOrder = sortOrder;
        this.sortDirectories = sortDirectories;
        reload();
        Message.info("sorted by: " + this.sortType.getDisplayName()
            + ", order: " + this.sortOrder + ", sortDir: " + this.sortDirectories);
    }

    public void copy(List<Path> entries, OverwriteFileConfirmer confirmer, Consumer<Path> postCopy) {

        entries.stream().forEach((entry) -> {
            // TODO バックグラウンド実行を検討
            Path newPath = otherFiler.resolve(entry);
            PathHelper.copyPath(entry, newPath, confirmer);
            postCopy.accept(entry);
        });
        // TODO リロードやめる
//        reload();
        otherFiler.reload();
    }

    public void move(List<Path> entries, OverwriteFileConfirmer confirmer, Consumer<Path> postMove) {
        entries.stream().forEach((entry) -> {
            Path movedPath = otherFiler.resolve(entry);
            PathHelper.movePath(entry, movedPath, confirmer);
//            postMove.accept(entry);
            // 移動後に反対側の窓でフォーカスさせる
            otherFiler.addToCache(movedPath);
        });
        reload();
        otherFiler.reload();
    }

    public void delete(List<Path> entries) {
        for (Path entry : entries) {
            // TODO バックグラウンド実行を検討
            if (Files.isDirectory(entry)) {
                try {
                    Files.walkFileTree(entry, new DeleteDirectoryVisitor());
                } catch (IOException ex) {
                    Message.error(ex);
                    return;
                }
                Message.info("deleted: " + entry.toString());
                continue;
            }
            try {
                Files.delete(entry);
                Message.info("deleted: " + entry.toString());
            } catch (IOException ex) {
                Message.error(ex);
                return;
            }
        }
        reload();
    }

    private Path resolve(Path sourcePath) {
        return currentDir.resolve(sourcePath.getFileName());
    }

    private void collectEntries() {
        Path parent = currentDir.getParent();
        int index = 0;
        if (parent != null) {
            Path normalizePath = normalizePath(parent);
            int value = index;
            postEntryLoadedEvent.raiseEvent(listener -> listener.postLoad(normalizePath, true, value));
            index++;
        }
        // TODO 権限がない場合真っ白になる
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            List<Path> entries = StreamSupport.stream(stream.spliterator(), false)
                .sorted(new PathComparator(sortType, sortOrder, sortDirectories))
                .collect(Collectors.toList());
            for (Path entry : entries) {
                int value = index;
                postEntryLoadedEvent.raiseEvent(listener -> listener.postLoad(entry, false, value));
                index++;
            }
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
