package sk44.jfxw.controller;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
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
import sk44.jfxw.model.FilerEvents;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.fs.FileSystem;
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
    private ModalWindow<TextFieldWindowController, Void> inputNewTextFileNameWindow;
    private ModalWindow<ConfirmWindowController, Boolean> confirmOnDeletingWindow;
    private ModalWindow<ConfirmWindowController, Boolean> confirmOnOverwritingWindow;
    private ModalWindow<JumpWindowController, Void> jumpWindow;

    private String searchText;
    private CurrentPathInfoBox currentPathInfoBox;

    @Getter
    private Filer filer;

    @FXML
    protected void handleCommandKeyPressed(KeyEvent event) {
//        Message.info(event.getCode().toString());
        switch (event.getCode()) {
            case C:
                copyMarkedPathesToOtherFiler();
                break;
            case D:
                deleteMarkedPathes();
                break;
            case E:
                if (event.isShiftDown()) {
                    openInputNewTextFileNameWindow();
                } else {
                    contents.openExternalEditor();
                }
                break;
            case G:
                if (event.isShiftDown()) {
                    contents.updateIndexToBottom();
                } else {
                    contents.updateIndexToTop();
                }
                break;
            case H:
                this.filer.changeDirectoryToParentDir();
                break;
            case J:
                if (event.isShiftDown()) {
                    openJumpWindow();
                } else {
                    contents.updateIndexToDown();
                }
                break;
            case DOWN:
                // down
                contents.updateIndexToDown();
                break;
            case K:
            case UP:
                // up
                contents.updateIndexToUp();
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
                    moveMarkedPathesToOtherFiler();
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
            case U:
                contents.extractArchive();
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
                contents.updateIndexToDown();
                break;
            case SLASH:
                openSearchTextField();
                break;
            case TAB:
                filer.toggleFocus();
                break;
            case ENTER:
                contents.preview();
                break;
            case COLON:
            case QUOTE:
                // COLON のつもりだったが、 MBP では QUOTE になる模様
                openJumpWindow();
                break;
            default:
                break;
        }
    }

    private void copyMarkedPathesToOtherFiler() {
        filer.copyToOtherFiler(contents.collectMarkedPathes(), this::showConfirmOnOverwritingDialog);
    }

    private boolean showConfirmOnOverwritingDialog(String message) {
        if (confirmOnOverwritingWindow == null) {
            confirmOnOverwritingWindow = new ModalWindow<>(Fxml.CONFIRM_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateMessage(message);
                controller.setOkAction(() -> {
                    // no-op
                    // TODO no-op はわかりづらい
                });
            });
        }
        return confirmOnOverwritingWindow.showAndWait();
    }

    private void deleteMarkedPathes() {
        if (confirmOnDeletingWindow == null) {
            confirmOnDeletingWindow = new ModalWindow<>(Fxml.CONFIRM_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateMessage("Marked pathes will be deleted! Are you sure?");
                controller.setOkAction(() -> {
                    filer.delete(contents.collectMarkedPathes());
                });
            });
        }
        confirmOnDeletingWindow.showAndWait();
    }

    private void moveMarkedPathesToOtherFiler() {
        // TODO コピーとわける？
        filer.moveToOtherFiler(contents.collectMarkedPathes(), this::showConfirmOnOverwritingDialog);
    }

    private void createSymbolicLink() {
        filer.createSymbolicLinks(contents.collectMarkedPathes());
    }

    private void openCreateDirectoryWindow() {
        if (createDirWindow == null) {
            createDirWindow = new ModalWindow<>(Fxml.TEXT_FIELD_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateContent("New directory", "");
                controller.setUpdateAction(dirName -> {
                    filer.createDirectory(dirName);
                });
            });
        }
        createDirWindow.showAndWait();
    }

    private void openInputNewTextFileNameWindow() {
        if (inputNewTextFileNameWindow == null) {
            inputNewTextFileNameWindow = new ModalWindow<>(Fxml.TEXT_FIELD_WINDOW, getModalWindowOwner(), controller -> {
                controller.updateContent("New text", "");
                controller.setUpdateAction(textFileName -> {
                    contents.openExternalEditorFor(textFileName);
                });
            });
        }
        inputNewTextFileNameWindow.showAndWait();
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
        FilerEvents filerEvents = filer.getEvents();
        filerEvents.addListenerToDirectoryWillChange(this::preChangeDirectory);
        filerEvents.addListenerToDirectoryChanged(this::directoryChanged);
        filerEvents.addListenerToFilerEntryLoaded(this::postEntryLoaded);
        filerEvents.addListenerToImageShowing(imagePath -> {
            Nodes.addStyleClassTo(flowPane, CLASS_NAME_PREVIEW_FILER);
        });
        filerEvents.addListenerToFocused(() -> {
            focus();
        });
        filerEvents.addListenerToLostFocus(() -> {
            onLostFocus();
        });
        filerEvents.addListenerToMarkedEntryProcessed(pathToProcess -> {
            contents.removeMark(pathToProcess);
        });
        this.filer = filer;
        this.contents.setFiler(filer);
        this.contents.setScrollPane(scrollPane);
        FileSystem fileSystem = ModelLocator.INSTANCE.getFileSystem();
        fileSystem.addDirectoryDeleted(dir -> {
            contents.onDirectoryDeleted(dir);
        });
        fileSystem.addFileDeleted(file -> {
            contents.removePathIfContains(file);
        });
    }

    private void preChangeDirectory(Path previousPath) {
        contents.clear();
    }

    private void directoryChanged(Path fromDir, Path toDir) {

        contents.onDirectoryChangedTo(toDir);
        // TODO バインド
        currentPathLabel.setText(toDir.toString());
        currentPathInfoBox.update(toDir);
    }

    private void focus() {
        // runLater でないと効かない
        Platform.runLater(() -> {
            flowPane.requestFocus();
            Nodes.addStyleClassTo(rootPane, CLASS_NAME_CURRENT_FILER);
        });
    }

    private void onLostFocus() {
        Nodes.removeStyleClassFrom(rootPane, CLASS_NAME_CURRENT_FILER);
    }

    private void postEntryLoaded(Path entry, boolean parent, int index) {
        if (parent) {
            contents.add(ContentRow.forParent(entry, scrollPane.widthProperty()));
            return;
        }
        contents.add(ContentRow.create(entry, scrollPane.widthProperty()));
    }

    private void searchNext(boolean keepCurrent) {
        contents.searchNext(searchText, keepCurrent);
    }

    private void searchPrevious() {
        contents.searchPrevious(searchText);
    }

    private void openByAssociatedApplication() {
        Path onCursor = contents.getCurrentContentPath();
        ModelLocator.INSTANCE
            .getConfigurationStore()
            .getConfiguration()
            .getAssociatedProcessFor(onCursor)
            .execute();
    }

    public Optional<Path> nextImage() {
        contents.updateIndexToDown();
        return contents.currentImage();
    }

    public Optional<Path> previousImage() {
        contents.updateIndexToUp();
        return contents.currentImage();
    }

    public void onPreviewTextEnd() {
        focus();
    }

    public void onPreviewImageEnd() {
        // TODO 対称性がない
        Nodes.removeStyleClassFrom(flowPane, CLASS_NAME_PREVIEW_FILER);
        focus();
    }

    private void updateBackgroundImage() {
        ModelLocator locator = ModelLocator.INSTANCE;
        locator.getConfigurationStore().getConfiguration().backgroundImageDir().ifPresent(dir -> {
            locator.getBackgroundImage().updateRandom(dir);
        });
    }
}
