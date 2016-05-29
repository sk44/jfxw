/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.nio.file.Path;

/**
 *
 * @author sk
 */
public class FilerEvents {

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
    public interface PreviewImageListener {

        void preview(Path imagePath);
    }

    @FunctionalInterface
    public interface PostProcessListener {

        void postProcess(Path pathToProcess);
    }
    private final EventSource<Runnable> focused = new EventSource<>();
    private final EventSource<Runnable> lostFocus = new EventSource<>();
    private final EventSource<PreChangeDirectoryListener> directoryWillChange = new EventSource<>();
    private final EventSource<PostChangeDirectoryListener> directoryChanged = new EventSource<>();
    private final EventSource<PathEntryLoadedListener> filerEntryLoaded = new EventSource<>();
    private final EventSource<CursorChangedListener> cursorChanged = new EventSource<>();
    private final EventSource<PreviewImageListener> imageShowing = new EventSource<>();
    private final EventSource<PostProcessListener> markedEntryProcessed = new EventSource<>();

    public void addListenerToFocused(Runnable listener) {
        focused.addListener(listener);
    }

    void raiseFocused() {
        focused.raiseEvent(Runnable::run);
    }

    public void addListenerToLostFocus(Runnable listener) {
        lostFocus.addListener(listener);
    }

    void raiseLostFocused() {
        lostFocus.raiseEvent(Runnable::run);
    }

    public void addListenerToCursorChanged(CursorChangedListener listener) {
        cursorChanged.addListener(listener);
    }

    void raiseCursorChanged(Path newPath) {
        cursorChanged.raiseEvent(listener -> listener.changedTo(newPath));
    }

    public void addListenerToImageShowing(PreviewImageListener listener) {
        imageShowing.addListener(listener);
    }

    void raiseImageShowing(Path imagePath) {
        imageShowing.raiseEvent(listener -> listener.preview(imagePath));
    }

    public void addListenerToMarkedEntryProcessed(PostProcessListener listener) {
        markedEntryProcessed.addListener(listener);
    }

    void raiseMarkedEntryProcessed(Path pathToProcess) {
        markedEntryProcessed.raiseEvent(listener -> listener.postProcess(pathToProcess));
    }

    // TODO remove の仕組みが必要かなー
    public void addListenerToDirectoryWillChange(PreChangeDirectoryListener listener) {
        this.directoryWillChange.addListener(listener);
    }

    void raiseDirectoryWillChange(Path fromDir) {
        this.directoryWillChange.raiseEvent(listener -> listener.changeDirectoryFrom(fromDir));
    }

    public void addListenerToDirectoryChanged(PostChangeDirectoryListener listener) {
        this.directoryChanged.addListener(listener);
    }

    void raiseDirectoryChanged(Path fromDir, Path toDir) {
        this.directoryChanged.raiseEvent(listener -> listener.directoryChanged(fromDir, toDir));
    }

    public void addListenerToFilerEntryLoaded(PathEntryLoadedListener listener) {
        this.filerEntryLoaded.addListener(listener);
    }

    void raiseFilerEntryLoaded(Path entry, boolean parent, int index) {
        filerEntryLoaded.raiseEvent(listener -> listener.postLoad(entry, parent, index));
    }

}
