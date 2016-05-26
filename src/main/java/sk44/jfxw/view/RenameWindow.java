/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.stage.Window;
import sk44.jfxw.controller.TextFieldWindowController;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class RenameWindow extends ModalWindow<TextFieldWindowController, Void> {

    private final Supplier<Optional<Path>> renameTargetSupplier;
    private final Runnable finishAction;

    public RenameWindow(Window owner, Supplier<Optional<Path>> renameTargetSupplier, Runnable finishAction) {
        super(Fxml.TEXT_FIELD_WINDOW, owner, (controller) -> {
            controller.updateTitle("Rename");
        });
        this.renameTargetSupplier = renameTargetSupplier;
        this.finishAction = finishAction;
    }

    @Override
    public Void showAndWait() {
        Optional<Path> targetOpt = renameTargetSupplier.get();
        if (targetOpt.isPresent() == false) {
            return null;
        }
        Path target = targetOpt.get();
        getController().updateText(target.getFileName().toString());
        getController().setUpdateAction(newName -> {
            try {
                // TODO パス区切り文字が入っている場合とか
                // TODO ディレクトリとファイルが同名の場合とか
                Path newPath = target.resolveSibling(newName);
                if (Files.exists(newPath)) {
                    Message.warn(newPath + " is already exists.");
                    return;
                }
                Files.move(target, newPath);
                Message.info("rename " + target + " to " + newPath);
                this.finishAction.run();
            } catch (IOException ex) {
                Message.error(ex);
            }
        });
        return super.showAndWait();

    }
}
