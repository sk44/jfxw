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

    public void show(String resourcePath, Window owner, Consumer<T> controllerConfigurer) {

        // http://stackoverflow.com/questions/10486731/how-to-create-a-modal-window-in-javafx-2-1
        // http://nodamushi.hatenablog.com/entry/20130910/1378784711
        try {
            stage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            // 先にロードしないと controller が取れない
            Parent root = loader.load();

            controllerConfigurer.accept(loader.getController());

            stage.setScene(new Scene(root, Color.TRANSPARENT));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.showAndWait();
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    public void close() {
        this.stage.close();
    }
}
