package sk44.jfxw;

import java.io.IOException;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sk44.jfxw.model.BackgroundImage;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.configuration.ConfigDir;
import sk44.jfxw.model.configuration.Configuration;
import sk44.jfxw.model.configuration.ConfigurationStore;
import sk44.jfxw.model.configuration.FilerConfig;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.persistence.EntityManagerFactoryProvider;
import sk44.jfxw.view.Fxml;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        initializeModelLocator();
        EntityManagerFactoryProvider.init();

        primaryStage = stage;

        Parent root = FXMLLoader.load(getClass().getResource(Fxml.MAIN_WINDOW.getPath()));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        // http://www.torutk.com/projects/swe/wiki/JavaFXとアナログ時計
//        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("JFXW");

        stage.setScene(scene);
        stage.show();
    }

    private void initializeModelLocator() throws IOException {

        ConfigurationStore configurationStore = new ConfigurationStore();
        configurationStore.init(ConfigDir.get());
        Configuration configuration = configurationStore.getConfiguration();

        Message.minLevel(configuration.getLogLevel());

        FilerConfig rightFilerConfig = configuration.getRightFilerConfig();
        FilerConfig leftFilerConfig = configuration.getLeftFilerConfig();

        Filer rightFiler = new Filer(rightFilerConfig.getPath(), rightFilerConfig.getSortType(),
            rightFilerConfig.getSortOrder(), rightFilerConfig.isSortDirectories());
        Filer leftFiler = new Filer(leftFilerConfig.getPath(), leftFilerConfig.getSortType(),
            leftFilerConfig.getSortOrder(), leftFilerConfig.isSortDirectories());
        rightFiler.setOtherFiler(leftFiler);
        leftFiler.setOtherFiler(rightFiler);

        ModelLocator locator = ModelLocator.INSTANCE;

        locator.setConfigurationStore(configurationStore);
        locator.setLeftFiler(leftFiler);
        locator.setRightFiler(rightFiler);
        locator.setBackgroundImage(new BackgroundImage());
    }

    @Override
    public void stop() throws Exception {
        saveConfiguration();
        super.stop();
    }

    private void saveConfiguration() throws IOException {
        ConfigurationStore configurationStore = ModelLocator.INSTANCE.getConfigurationStore();
        Configuration configuration = configurationStore.getConfiguration();

        Filer leftFiler = ModelLocator.INSTANCE.getLeftFiler();
        Filer rightFiler = ModelLocator.INSTANCE.getRightFiler();

        configuration.updateLeftFilerConfig(leftFiler);
        configuration.updateRightFilerConfig(rightFiler);

        configurationStore.save();
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
