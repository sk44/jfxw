package sk44.jfxw.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Window;
import lombok.Getter;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.view.ContentRow;
import sk44.jfxw.view.CurrentPathInfoBox;
import sk44.jfxw.view.FilerContents;
import sk44.jfxw.view.Fxml;
import sk44.jfxw.view.ModalWindow;
import sk44.jfxw.view.Nodes;
import sk44.jfxw.view.RenameWindow;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class FilerViewController implements Initializable {

    private static final String CLASS_NAME_PREVIEW_FILER = "previewFiler";
    private static final String CLASS_NAME_CURRENT_FILER = "currentFiler";

    private static void ensureVisible(ScrollPane scrollPane, ContentRow row) {

        // http://stackoverflow.com/questions/15840513/javafx-scrollpane-programmatically-moving-the-viewport-centering-content
        Platform.runLater(() -> {
            // 全体の高さ
            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            // row の位置
            double rowY = (row.getBoundsInParent().getMaxY() + row.getBoundsInParent().getMinY()) / 2.0;
//            double rowY = index * 15 + 7.5;
            // 表示範囲の高さ
            double visibleHeight = scrollPane.getViewportBounds().getHeight();
            // TODO 中央位置に合わせてスクロールしてしまうので、上か下に
            scrollPane.setVvalue(scrollPane.getVmax() * ((rowY - 0.5 * visibleHeight) / (contentHeight - visibleHeight)));
        });
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

    private final FilerContents contents = new FilerContents();

    private ModalWindow<SortWindowController, Void> sortWindow;
    private RenameWindow renameWindow;
    private ModalWindow<TextFieldWindowController, Void> createDirWindow;
    private ModalWindow<TextFieldWindowController, Void> searchWindow;
    // TODO final じゃなくする
    private final ModalWindow<ConfirmWindowController, Boolean> deleteConfirmWindow = new ModalWindow<>();
    private final ModalWindow<ConfirmWindowController, Boolean> copyConfirmWindow = new ModalWindow<>();
    private ModalWindow<JumpWindowController, Void> jumpWindow;

    private String searchText;
    private CurrentPathInfoBox currentPathInfoBox;

    @Getter
    private Filer filer;

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
                openExternalEditor();
                break;
            case G:
                contents.clearCursor();
                if (event.isShiftDown()) {
                    contents.updateIndexToBottom();
                } else {
                    contents.updateIndexToTop();
                }
                updateCursor();
                break;
            case H:
                this.filer.changeDirectoryToParentDir();
                break;
            case J:
                if (event.isShiftDown()) {
                    openJumpWindow();
                } else {
                    next();
                }
                break;
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
                ContentRow currentContent = contents.getCurrentContent();
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
                    searchNext(false);
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
                    openRenameWindow();
                } else {
                    filer.reload();
                }
                break;
            case S:
                if (event.isShiftDown()) {
                    createSymbolicLink();

                } else {
                    openSortOptionWindow();
                }
                break;
            case W:
                updateBackgroundImage();
                break;
            case X:
                openByAssociatedApplication();
                break;
            case Y:
                contents.yankCurrentContent();
                break;
            case Z:
                // TODO 設定画面？
                break;
            case SPACE:
                // Space のデフォルト動作？で勝手にスクロールしてしまうので無効化
                // TODO 全体的にやるべき？
                event.consume();
                contents.getCurrentContent().toggleMark();
                next();
                break;
            case SLASH:
                openSearchTextField();
                break;
            case TAB:
                filer.toggleFocus();
                break;
            case ENTER:
                previewImage();
                break;
            default:
                break;
        }
    }

    private void next() {
        contents.updateIndexToDown();
        updateCursor();
    }

    private void previous() {
        contents.updateIndexToUp();
        updateCursor();
    }

    private void copy() {
        filer.copy(contents.collectMarkedPathes(), this::showCopyConfirmDialog);
        updateCursor();
    }

    private boolean showCopyConfirmDialog(String message) {
        return copyConfirmWindow.showAndWait(Fxml.CONFIRM_WINDOW, getModalWindowOwner(), controller -> {
            controller.updateMessage(message);
            controller.setOkAction(() -> {
                // no-op
                // TODO no-op はわかりづらい
            });
        });
    }

    private void delete() {
        deleteConfirmWindow.showAndWait(Fxml.CONFIRM_WINDOW, getModalWindowOwner(), controller -> {
            controller.updateMessage("Are you sure?");
            controller.setOkAction(() -> {
                filer.delete(contents.collectMarkedPathes());
                updateCursor();
            });
        });
    }

    private void move() {
        // TODO コピーとわける？
        filer.move(contents.collectMarkedPathes(), this::showCopyConfirmDialog);
        updateCursor();
    }

    private void createSymbolicLink() {
        filer.createSymbolicLinks(contents.collectMarkedPathes());
    }

    private void updateCursor() {
        // TODO
        ContentRow currentContent = contents.getCurrentContent();
        currentContent.updateSelected(true);
        ensureVisible(scrollPane, currentContent);
        filer.onCursorChangedTo(currentContent.getPath());
    }

    private void openCreateDirectoryWindow() {
        if (createDirWindow == null) {
            createDirWindow = new ModalWindow<>(Fxml.TEXT_FIELD_WINDOW, getModalWindowOwner(), (controller) -> {
                controller.updateContent("New directory", "");
                controller.setUpdateAction(dirName -> {
                    filer.createDirectory(dirName);
                });
            });
        }
        createDirWindow.showAndWait();
    }

    private void openRenameWindow() {
        if (renameWindow == null) {
            renameWindow = new RenameWindow(getModalWindowOwner(), () -> {
                ContentRow currentContent = contents.getCurrentContent();
                if (currentContent.isParent()) {
                    return Optional.empty();
                }
                return Optional.of(currentContent.getPath());
            }, () -> {
                filer.reload();
            });
        }
        renameWindow.showAndWait();
    }

    private void openJumpWindow() {
        if (jumpWindow == null) {
            jumpWindow = new ModalWindow<>(Fxml.JUMP_WINDOW, getModalWindowOwner(), controller -> {
                controller.setJumpAction(path -> {
                    filer.changeDirectoryTo(path);
                });
            });
        }
        jumpWindow.showAndWait();
    }

    private void openSortOptionWindow() {
        if (sortWindow == null) {
            sortWindow = new ModalWindow<>(Fxml.SORT_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateSortOptions(this.filer.getSortType(),
                    this.filer.getSortOrder(), this.filer.isSortDirectories());
                controller.setUpdateAction(this.filer::updateSortType);
            });
        }
        sortWindow.showAndWait();
    }

    private void openSearchTextField() {
        if (searchWindow == null) {
            searchWindow = new ModalWindow<>(Fxml.TEXT_FIELD_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateContent("Search", searchText);
                controller.addKeyReleasedEventHandler((query, e) -> {
                    switch (e.getCode()) {
                        case ESCAPE:
                            break;
                        case ENTER:
                            if (e.isShiftDown()) {
                                searchText = query;
                                searchPrevious();
                                controller.close();
                            }
                            break;
                        default:
                            searchText = query;
                            searchNext(true);
                            break;
                    }
                });
                controller.setUpdateAction(query -> {
                    searchText = query;
                    searchNext(true);
                });
            });
        }
        searchWindow.showAndWait();
    }

    private Window getModalWindowOwner() {
        return rootPane.getScene().getWindow();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // bind しないと、ウィンドウ幅を変更したとき表示がズレる
//        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        flowPane.prefWidthProperty().bind(scrollPane.widthProperty());
        contents.bindContentWith(flowPane.getChildren());
        // マウス操作でのフォーカス変更に対応する
        // 無効化してしまうのもありかも
        scrollPane.focusedProperty().addListener((arg, oldValue, focused) -> {
            if (focused) {
                filer.focus();
            }
        });
        currentPathInfoBox = new CurrentPathInfoBox();
        currentPathInfoBox.addTo(rootPane);
    }

    public void withFiler(Filer filer) {
        this.filer = filer;
        this.filer.addListenerToPreChangeDirectoryEvent(this::preChangeDirectory);
        this.filer.addListenerToPostChangeDirectoryEvent(this::directoryChanged);
        this.filer.addListenerToPostEntryLoadedEvent(this::postEntryLoaded);
        this.filer.addListenerToPreviewImageEvent(imagePath -> {
            Nodes.addStyleClassTo(flowPane, CLASS_NAME_PREVIEW_FILER);
        });
        this.filer.addListenerToFocusedEvent(() -> {
            focus();
        });
        this.filer.addListenerToLostFocusEvent(() -> {
            onLostFocus();
        });
        this.filer.addListenerToUpdateStatusEvent(currentPathInfoBox::update);
        this.filer.addListenerToPostProcessEvent(pathToProcess -> {
            contents.removeMark(pathToProcess);
        });
        this.contents.setFiler(filer);
    }

    private void preChangeDirectory(Path previousPath) {
        contents.clear();
    }

    private void directoryChanged(Path fromDir, Path toDir) {

        int focusIndex = this.filer.lastFocusedPathIn(toDir)
            .map(focused -> contents.indexOfPath(focused).orElse(0))
            .orElse(0);
        contents.updateIndex(focusIndex);
        // TODO バインド
        currentPathLabel.setText(toDir.toString());
        updateCursor();
    }

    private void focus() {
        // runLater でないと効かない
        Platform.runLater(() -> {
            flowPane.requestFocus();
            Nodes.addStyleClassTo(rootPane, CLASS_NAME_CURRENT_FILER);
            updateCursor();
        });
    }

    private void onLostFocus() {
        Nodes.removeStyleClassFrom(rootPane, CLASS_NAME_CURRENT_FILER);
    }

    private void postEntryLoaded(Path entry, boolean parent, int index) {
        final boolean odd = index % 2 != 0;
        if (parent) {
            contents.add(ContentRow.forParent(entry, scrollPane.widthProperty(), odd));
            return;
        }
        contents.add(ContentRow.create(entry, scrollPane.widthProperty(), odd));
    }

    private void searchNext(boolean keepCurrent) {
        contents.searchNext(searchText, keepCurrent, () -> {
            updateCursor();
        });
    }

    private void searchPrevious() {
        contents.searchPrevious(searchText, () -> {
            updateCursor();
        });
    }

    private void openExternalEditor() {
        Path onCursor = contents.getCurrentContentPath();
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getEditorProcessFor(onCursor)
            .execute();
    }

    private void openByAssociatedApplication() {
        Path onCursor = contents.getCurrentContentPath();
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getAssociatedProcessFor(onCursor)
            .execute();
    }

    private void previewImage() {
        contents.currentImage().ifPresent(image -> {
            filer.previewImage(image);
        });
    }

    public Optional<Path> nextImage() {
        next();
        return contents.currentImage();
    }

    public Optional<Path> previousImage() {
        previous();
        return contents.currentImage();
    }

    public void endPreviewImage() {
        // TODO 対称性がない
        Nodes.removeStyleClassFrom(flowPane, CLASS_NAME_PREVIEW_FILER);
        focus();
    }

    // TODO 実装をどこかに移動
    private final Random random = new Random();

    private void updateBackgroundImage() {
        ModelLocator locator = ModelLocator.INSTANCE;
        locator.getConfigurationStore().getConfiguration().backgroundImageDir().ifPresent(dir -> {
            if (Files.exists(dir) == false || Files.isDirectory(dir) == false) {
                Message.warn(dir + " does not exists or not a directory.");
                return;
            }
            try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(dir, "*.{jpg,jpeg,png,gif}")) {
                List<Path> images = StreamSupport.stream(stream.spliterator(), false)
                    .collect(Collectors.toList());
                if (images.isEmpty()) {
                    Message.warn("no images found in " + dir + ".");
                    return;
                }
                int targetIndex = random.nextInt(images.size() - 1);
                locator.getApplicationEvents().updateBackgroundImage(images.get(targetIndex));

            } catch (IOException ex) {
                Message.error(ex);
            }
        });
    }
}
