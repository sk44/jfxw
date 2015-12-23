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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.fs.CopyDirectoryVisitor;
import sk44.jfxw.model.fs.DeleteDirectoryVisitor;
import sk44.jfxw.model.fs.MoveDirectoryVisitor;
import sk44.jfxw.model.fs.OverwriteFileConfirmer;
import sk44.jfxw.model.fs.PathHelper;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class Filer {

    @FunctionalInterface
    public interface PreChangeDirectoryObserver {

        void changeDirectoryFrom(Path previousDir);
    }

    @FunctionalInterface
    public interface PostChangeDirectoryObserver {

        void directoryChanged(Path fromDir, Path toDir);
    }

    @FunctionalInterface
    public interface PathEntryLoadedObserver {

        void postLoad(Path entry, boolean parent, int index);
    }

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

    private final List<PreChangeDirectoryObserver> preChangeDirectoryObservers = new ArrayList<>();
    private final List<PostChangeDirectoryObserver> postChangeDirectoryObservers = new ArrayList<>();
    private final List<PathEntryLoadedObserver> postEntryLoadedObservers = new ArrayList<>();

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

    // TODO remove の仕組みが必要かなー
    public void addPreChangeDirectoryObserver(PreChangeDirectoryObserver observer) {
        this.preChangeDirectoryObservers.add(observer);
    }

    public void addPostChangeDirectoryObserver(PostChangeDirectoryObserver observer) {
        this.postChangeDirectoryObservers.add(observer);
    }

    public void addPostEntryLoadedObserver(PathEntryLoadedObserver observer) {
        this.postEntryLoadedObservers.add(observer);
    }

    private void reload() {
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

    public void changeDirectoryTo(Path dir) {
        if (Files.isDirectory(dir) == false) {
            // TODO assert?
            return;
        }
        Path fromDir = this.currentDir;
        this.preChangeDirectoryObservers.forEach(observer -> observer.changeDirectoryFrom(fromDir));
        this.currentDir = normalizePath(dir);
        collectEntries();
        this.postChangeDirectoryObservers.forEach(observer -> observer.directoryChanged(fromDir, this.currentDir));
        Message.debug("moved to: " + dir.toString());
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
        } catch (IOException ex) {
            Message.error(ex);
        }
        Message.info(newDir + " created.");
        changeDirectoryTo(newDir);
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
            + ", order: " + this.sortOrder + "sortDir: " + this.sortDirectories);
    }

    public void copy(List<Path> entries, OverwriteFileConfirmer confirmer) {

        for (Path entry : entries) {
            // TODO バックグラウンド実行を検討
            if (Files.isDirectory(entry)) {
                Path toDir = otherFiler.resolve(entry);
                try {
                    PathHelper.createDirectoryIfNotExists(toDir);
                    Files.walkFileTree(entry, new CopyDirectoryVisitor(entry,
                        toDir, confirmer));
                } catch (IOException ex) {
                    Message.error(ex);
                    return;
                }
                Message.info("copied: " + entry.toString() + "\n\tto: " + toDir.toString());
                continue;
            }
            if (otherFiler.copyFrom(entry) == false) {
                return;
            }
        }
        reload();
        otherFiler.reload();
    }

    public void move(List<Path> entries, OverwriteFileConfirmer confirmer) {
        for (Path entry : entries) {
            if (Files.isDirectory(entry)) {
                Path toDir = otherFiler.resolve(entry);
                try {
                    PathHelper.createDirectoryIfNotExists(toDir);
                    Files.walkFileTree(entry, new MoveDirectoryVisitor(entry,
                        toDir, confirmer));
                    Files.walkFileTree(entry, new DeleteDirectoryVisitor());
                } catch (IOException ex) {
                    Message.error(ex);
                    return;
                }
                Message.info("moved: " + entry.toString() + "\n\tto: " + toDir.toString());
                continue;
            }
            if (otherFiler.moveFrom(entry) == false) {
                break;
            }
        }
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

    private boolean copyFrom(Path sourcePath) {
        Path newPath = resolve(sourcePath);
        if (Files.exists(newPath)) {
            Message.debug("path " + newPath.toString() + " is already exists.");
            return true;
        }
        try {
            Files.copy(sourcePath, newPath);
            Message.info("copied: " + sourcePath.toString() + "\n\tto: " + newPath.toString());
        } catch (IOException ex) {
            Message.error(ex);
            return false;
        }
        return true;
    }

    private boolean moveFrom(Path sourcePath) {
        Path newPath = resolve(sourcePath);
        if (Files.exists(newPath)) {
            Message.debug("path " + newPath.toString() + " is already exists.");
            return true;
        }
        if (Files.exists(newPath)) {
            // TODO confirm
            Message.info("destination path " + newPath.toString() + " is already exists.");
            return true;
        }
        try {
            Files.move(sourcePath, newPath);
            Message.info("moved: " + sourcePath.toString() + "\n\tto: " + newPath.toString());
            return true;
        } catch (IOException ex) {
            Message.error(ex);
            return false;
        }
    }

    private Path resolve(Path sourcePath) {
        return currentDir.resolve(sourcePath.getFileName());
    }

    private void collectEntries() {
        Path parent = currentDir.getParent();
        int index = 0;
        if (parent != null) {
            Path normalizePath = normalizePath(parent);
            for (PathEntryLoadedObserver observer : this.postEntryLoadedObservers) {
                observer.postLoad(normalizePath, true, index);
            }
            index++;
        }
        // TODO 権限がない場合真っ白になる
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            List<Path> entries = StreamSupport.stream(stream.spliterator(), false)
                .sorted(new PathComparator(sortType, sortOrder, sortDirectories))
                .collect(Collectors.toList());
            for (Path entry : entries) {
                for (PathEntryLoadedObserver observer : this.postEntryLoadedObservers) {
                    observer.postLoad(entry, false, index);
                }
                index++;
            }
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
