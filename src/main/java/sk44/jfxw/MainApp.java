package sk44.jfxw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sk44.jfxw.controller.SortWindowController;
import sk44.jfxw.model.Configuration;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.Message;
import sk44.jfxw.model.MessageLevel;
import sk44.jfxw.model.ModelLocator;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        initializeModelLocator();

        primaryStage = stage;

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainWindow.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        // http://www.torutk.com/projects/swe/wiki/JavaFXとアナログ時計
//        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("JFXW");

        initailizeConfig();

        stage.setScene(scene);
        stage.show();
    }

    private void initailizeConfig() throws IOException {

        Configuration.initialize(new File("."));
        Configuration conf = Configuration.get();
        Message.minLevel(MessageLevel.ofName(conf.getLogLevel()));

    }

    private void openSortWindow() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Sort option");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(primaryStage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortWindow.fxml"));
        try {
            Pane pane = loader.load();
            Scene scene = new Scene(pane);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException ex) {
            Message.error(ex);
            return;
        }
        SortWindowController controller = loader.getController();
    }

    private void initializeModelLocator() {

        Path initialPath = getInitialPath();
        Filer rightFiler = new Filer(initialPath);
        Filer leftFiler = new Filer(initialPath);
        rightFiler.setOtherFiler(leftFiler);
        leftFiler.setOtherFiler(rightFiler);

        ModelLocator.INSTANCE.setLeftFiler(leftFiler);
        ModelLocator.INSTANCE.setRightFiler(rightFiler);
    }

    private Path getInitialPath() {
        // TODO どこかに設定
        return new File(".").toPath();
    }

    @Override
    public void stop() throws Exception {
        Configuration.save();
        super.stop();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
