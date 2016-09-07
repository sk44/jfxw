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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.fs.FileSystem;
import sk44.jfxw.model.fs.OverwriteFileConfirmer;
import sk44.jfxw.model.fs.PathHelper;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class Filer {

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

    public Filer(String initialPath, PathSortType sortType, PathSortOrder sortOrder, boolean sortDirectories, FileSystem fileSystem) {
        this.currentDir = normalizePath(Paths.get(initialPath));
        this.sortType = sortType;
        this.sortOrder = sortOrder;
        this.sortDirectories = sortDirectories;
        this.events = new FilerEvents();
        this.fileSystem = fileSystem;
    }

    private final PathHistoriesCache historiesCache = new PathHistoriesCache(HISTORY_BUFFER_SIZE);

    @Getter
    private final FilerEvents events;

    private final FileSystem fileSystem;

    private boolean focused;

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

    public void toggleFocus() {
        if (focused) {
            otherFiler.focus();
        } else {
            focus();
        }
    }

    // TODO ここにあるのは微妙に違和感
    public void updateFocus() {
        if (focused) {
            focus();
        } else {
            otherFiler.focus();
        }
    }

    public void focus() {
        focused = true;
        events.raiseFocused();
        otherFiler.lostFocus();
    }

    private void lostFocus() {
        focused = false;
        events.raiseLostFocused();
    }

    private void onMarkedEntryProcessed(Path pathToProcess) {
        events.raiseMarkedEntryProcessed(pathToProcess);
    }

    public void onCursorChangedTo(Path path) {
        events.raiseCursorChanged(path);
    }

    public void previewImage(Path imagePath) {
        events.raiseImageShowing(imagePath);
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
        events.raiseDirectoryWillChange(fromDir);
        this.currentDir = normalizePath(dir);
        collectEntries();
        events.raiseDirectoryChanged(fromDir, this.currentDir);
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
            // 複数階層も一応サポートしておく
            Files.createDirectories(newDir);
//            Files.createDirectory(newDir);
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

    public void copyToOtherFiler(List<Path> entries, OverwriteFileConfirmer confirmer) {

        entries.stream().forEach((entry) -> {
            // TODO バックグラウンド実行を検討
            Path newPath = otherFiler.resolve(entry);
            if (PathHelper.copyPath(entry, newPath, confirmer)) {
                onMarkedEntryProcessed(entry);
                // 移動後に反対側の窓でフォーカスさせる
                otherFiler.addToCache(newPath);
            }
        });
        otherFiler.reload();
    }

    public void createSymbolicLinks(List<Path> entries) {
        // TODO confirm が必要
        try {
            for (Path entry : entries) {
                Path newLink = otherFiler.resolve(entry);
                if (Files.exists(newLink)) {
                    Message.error(newLink + " is already exists.");
                    continue;
                }
                Files.createSymbolicLink(newLink, entry);
                // リンク作成後に反対側の窓でフォーカスさせる
                otherFiler.addToCache(newLink);
            }
            otherFiler.reload();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    public void moveToOtherFiler(List<Path> entries, OverwriteFileConfirmer confirmer) {
        if (isSameDirWithOther()) {
            Message.warn("cannot move to the same dir.");
            return;
        }
        entries.stream().forEach((entry) -> {
            // TODO スキップしてもマークが外れてしまう
            if (entry.startsWith(otherFiler.currentDir)) {
                Message.warn(entry + " is parent dir of " + otherFiler.currentDir + ". skip moving.");
                return;
            }
            // TODO バックグラウンド実行を検討
            Path movedPath = otherFiler.resolve(entry);
            if (fileSystem.movePath(entry, movedPath, confirmer)) {
                onMarkedEntryProcessed(entry);
                // 移動後に反対側の窓でフォーカスさせる（ reload に依存）
                otherFiler.addToCache(movedPath);
            }
        });
        // TODO 先頭にカーソルが移動してしまう場合がある
        reload();
        otherFiler.reload();
    }

    public void delete(List<Path> entries) {
        for (Path entry : entries) {
            // TODO バックグラウンド実行を検討
            fileSystem.deletePath(entry);
            Message.info("deleted: " + entry.toString());
            onMarkedEntryProcessed(entry);
        }
//        reload();
    }

    private boolean isSameDirWithOther() {
        return currentDir.equals(otherFiler.currentDir);
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
            events.raiseFilerEntryLoaded(normalizePath, true, value);
            index++;
        }
        // TODO 権限がない場合真っ白になる
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            List<Path> entries = StreamSupport.stream(stream.spliterator(), false)
                .sorted(new PathComparator(sortType, sortOrder, sortDirectories))
                .collect(Collectors.toList());
            for (Path entry : entries) {
                int value = index;
                events.raiseFilerEntryLoaded(entry, false, value);
                index++;
            }
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
