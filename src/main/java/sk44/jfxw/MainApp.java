package sk44.jfxw;

import java.io.IOException;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sk44.jfxw.model.BackgroundImage;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.configuration.ConfigDir;
import sk44.jfxw.model.configuration.Configuration;
import sk44.jfxw.model.configuration.ConfigurationStore;
import sk44.jfxw.model.configuration.FilerConfig;
import sk44.jfxw.model.fs.FileSystem;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.persistence.EntityManagerFactoryProvider;
import sk44.jfxw.view.Fxml;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        EntityManagerFactoryProvider.init();
        initializeModelLocator();

        Parent root = FXMLLoader.load(getClass().getResource(Fxml.MAIN_WINDOW.getPath()));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        // http://www.torutk.com/projects/swe/wiki/JavaFXとアナログ時計
//        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("JFXW");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        updateWindow(stage);

        primaryStage = stage;

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

        FileSystem fileSystem = new FileSystem();
        Filer rightFiler = new Filer(rightFilerConfig.getPath(), rightFilerConfig.getSortType(),
            rightFilerConfig.getSortOrder(), rightFilerConfig.isSortDirectories(), fileSystem);
        Filer leftFiler = new Filer(leftFilerConfig.getPath(), leftFilerConfig.getSortType(),
            leftFilerConfig.getSortOrder(), leftFilerConfig.isSortDirectories(), fileSystem);
        rightFiler.setOtherFiler(leftFiler);
        leftFiler.setOtherFiler(rightFiler);

        ModelLocator locator = ModelLocator.INSTANCE;

        locator.setConfigurationStore(configurationStore);
        locator.setLeftFiler(leftFiler);
        locator.setRightFiler(rightFiler);
        locator.setBackgroundImage(new BackgroundImage());
        locator.setFileSystem(fileSystem);
    }

    private void updateWindow(Stage stage) {
        ConfigurationStore configurationStore = ModelLocator.INSTANCE.getConfigurationStore();
        Configuration configuration = configurationStore.getConfiguration();

        stage.setWidth(configuration.getWindowWidth());
        stage.setHeight(configuration.getWindowHeight());
        stage.setX(configuration.getWindowX());
        stage.setY(configuration.getWindowY());
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
        configuration.setWindowX(primaryStage.getX());
        configuration.setWindowY(primaryStage.getY());
        configuration.setWindowWidth(primaryStage.getWidth());
        configuration.setWindowHeight(primaryStage.getHeight());

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
