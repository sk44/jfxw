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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.fs.CopyDirectoryVisitor;
import sk44.jfxw.model.fs.DeleteDirectoryVisitor;
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

        void changedDirectoryTo(Path dir);
    }

    @FunctionalInterface
    public interface PathEntryLoadedObserver {

        void postLoad(Path entry, boolean parent);
    }

    private static Path normalizePath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    public Filer(Path initialPath) {
        this.currentDir = normalizePath(initialPath);
    }

    private final List<PreChangeDirectoryObserver> preChangeDirectoryObservers = new ArrayList<>();
    private final List<PostChangeDirectoryObserver> postChangeDirectoryObservers = new ArrayList<>();
    private final List<PathEntryLoadedObserver> postEntryLoadedObservers = new ArrayList<>();

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
        this.preChangeDirectoryObservers.forEach(observer -> observer.changeDirectoryFrom(this.currentDir));
        this.currentDir = normalizePath(dir);
        collectEntries();
        this.postChangeDirectoryObservers.forEach(observer -> observer.changedDirectoryTo(dir));
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

    public void copy(List<Path> entries, CopyDirectoryVisitor.OverwriteConfirmer confirmer) {

        for (Path entry : entries) {
            // TODO バックグラウンド実行を検討
            if (Files.isDirectory(entry)) {
                CopyDirectoryVisitor copyDirectoryVisitor = new CopyDirectoryVisitor(entry,
                    otherFiler.getCurrentDir(), confirmer);
                try {
                    Files.walkFileTree(entry, copyDirectoryVisitor);
                } catch (IOException ex) {
                    Message.error(ex);
                    return;
                }
//                Message.info("copy directories is not implemented yet!");
                continue;
            }
            if (otherFiler.copyFrom(entry) == false) {
                return;
            }
        }
        reload();
        otherFiler.reload();
    }

    public void move(List<Path> entries) {
        for (Path entry : entries) {
            // TODO ディレクトリの移動処理実装
            if (Files.isDirectory(entry)) {
                Message.info("move directories is not implemented yet!");
                continue;
            }
            Message.debug("move target: " + entry.toString());
            if (otherFiler.moveFrom(entry)) {
                break;
            }
        }
        reload();
        otherFiler.reload();
    }

    private boolean moveFrom(Path sourcePath) {
        Path newPath = resolve(sourcePath);
        if (newPath == null) {
            return true;
        }
        if (Files.exists(newPath)) {
            // TODO confirm
            Message.info("destination path " + newPath.toString() + " is already exists.");
            return true;
        }
        Message.info("move " + sourcePath.toString() + " to " + newPath.toString());
        try {
            Files.move(sourcePath, newPath);
            return true;
        } catch (IOException ex) {
            Message.error(ex);
            return false;
        }
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
        // TODO null?
        if (newPath == null) {
            return true;
        }
        Message.info("copy " + sourcePath.toString() + " to " + newPath.toString());
        try {
            Files.copy(sourcePath, newPath);
        } catch (IOException ex) {
            Message.error(ex);
            return false;
        }
        return true;
    }

    private Path resolve(Path sourcePath) {
        Path newPath = currentDir.resolve(sourcePath.getFileName());
        if (Files.exists(newPath)) {
            Message.warn("path " + newPath.toString() + " is already exists.");
            return null;
        }
        return newPath;
    }

    private void collectEntries() {
        Path parent = currentDir.getParent();
        if (parent != null) {
            Path normalizePath = normalizePath(parent);
            this.postEntryLoadedObservers.forEach(observer -> observer.postLoad(normalizePath, true));
        }
        // TODO 権限がない場合真っ白になる
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            StreamSupport.stream(stream.spliterator(), false)
                .sorted(PathComparator.FILE_NAME)
                .forEach(entry -> {
                    this.postEntryLoadedObservers.forEach(observer -> observer.postLoad(entry, false));
                });
        } catch (IOException ex) {
            Message.error(ex);
        }
    }
}
