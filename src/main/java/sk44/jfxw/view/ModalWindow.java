/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.AccessLevel;
import lombok.Getter;
import sk44.jfxw.controller.modal.ModalWindowController;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 * @param <C>
 * @param <R>
 */
public class ModalWindow<C extends ModalWindowController<R>, R> {

    private final Stage stage = new Stage();
    @Getter(AccessLevel.PROTECTED)
    private C controller;

    public ModalWindow(Fxml fxml, Window owner, Consumer<C> controllerConfigurer) {
        init(fxml, owner, controllerConfigurer);
    }

    private void init(Fxml fxml, Window owner, Consumer<C> controllerConfigurer) {
        // http://stackoverflow.com/questions/10486731/how-to-create-a-modal-window-in-javafx-2-1
        // http://nodamushi.hatenablog.com/entry/20130910/1378784711
        try {
            stage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml.getPath()));
            // 先にロードしないと controller が取れない
            Parent root = loader.load();

            controller = loader.getController();
            Objects.requireNonNull(controller);

            controller.setCloseAction(this::close);
            controllerConfigurer.accept(controller);

            Scene scene = new Scene(root, Color.TRANSPARENT);
//            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            initStagePosition(owner);

        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    public R showAndWait() {
        controller.preShown();
        this.stage.showAndWait();
        return controller.getResult();
    }

    private void initStagePosition(Window owner) {
        // ウィンドウの中心に表示（これをやらないとディスプレイの中心に出る）
        // この時点では stage.getWidth が NaN なのでリスナーを設定しておく
//            stage.setX(owner.getX() + owner.getWidth() / 2 - stage.getWidth() / 2);
//            stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
        stage.setX(owner.getX() + owner.getWidth() / 2);
        stage.setY(owner.getY() + owner.getHeight() / 2);
        stage.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            stage.setX(stage.getX() - newWidth.doubleValue() / 2);
        });
        stage.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            stage.setY(stage.getY() - newHeight.doubleValue() / 2);
        });

    }

    private void close() {
        // TODO close でいいのか？隠すだけ？
        this.stage.close();
    }
}
