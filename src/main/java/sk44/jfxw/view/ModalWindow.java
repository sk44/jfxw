/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 * @param <T> controller
 */
public class ModalWindow<T> {

    private final Stage stage = new Stage();

    public void show(Fxml fxml, Window owner, Consumer<T> controllerConfigurer) {

        // http://stackoverflow.com/questions/10486731/how-to-create-a-modal-window-in-javafx-2-1
        // http://nodamushi.hatenablog.com/entry/20130910/1378784711
        try {
            stage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml.getPath()));
            // 先にロードしないと controller が取れない
            Parent root = loader.load();

            controllerConfigurer.accept(loader.getController());

            Scene scene = new Scene(root, Color.TRANSPARENT);
//            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            initStagePosition(owner);
            stage.showAndWait();
        } catch (IOException ex) {
            Message.error(ex);
        }
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

    public void close() {
        this.stage.close();
    }
}
