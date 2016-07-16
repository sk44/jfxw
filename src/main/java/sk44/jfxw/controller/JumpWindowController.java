/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;
import sk44.jfxw.model.ModelLocator;
import sk44.jfxw.model.fs.index.DirectoryIndex;
import sk44.jfxw.model.fs.index.DirectoryScanner;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.persistence.DirectoryIndexRepository;
import sk44.jfxw.model.persistence.EntitiesContext;

/**
 * FXML Controller class
 *
 * @author sk
 */
public class JumpWindowController extends ModalWindowController<Void> implements Initializable {

    private static final String STYLE_CLASS_AUTO_COMPLETE = "autoComplete";
    private static final int MAX_COMPLETE_LIMIT = 5;
    @FXML
    private Pane rootPane;
    @FXML
    private TextField textField;

    @Setter
    private Consumer<Path> jumpAction;

    private final ContextMenu completePupup = new ContextMenu();

    private EntitiesContext context;
    private DirectoryIndexRepository repository;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textField.requestFocus();
        this.rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCommandKeyPressed);
        textField.textProperty().addListener((observableValue, s1, s2) -> {
            String query = textField.getText();
            if (query.isEmpty()) {
                completePupup.hide();
                return;
            }
            populateCompletion(query);
        });
        completePupup.getStyleClass().add(STYLE_CLASS_AUTO_COMPLETE);
        completePupup.hide();
    }

    private void populateCompletion(String query) {
        // https://gist.github.com/floralvikings/10290131
        List<CustomMenuItem> newItems = createItems(query);
        completePupup.getItems().clear();
        completePupup.getItems().addAll(newItems);

        if (completePupup.isShowing()) {
            return;
        }
        completePupup.show(textField, Side.BOTTOM, 0, 0);
    }

    private List<CustomMenuItem> createItems(String query) {
        return repository.findByPathLike(query, MAX_COMPLETE_LIMIT)
            .stream()
            .map(path -> {
                CustomMenuItem item = new CustomMenuItem(new Label(path), true);
                item.setOnAction(event -> {
                    textField.setText(path);
                    completePupup.hide();
                    jump();
                });
                return item;
            })
            .collect(Collectors.toList());
    }

    protected void handleCommandKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case R:
                if (event.isControlDown()) {
                    scanIndexes();
                }
                break;
            case ESCAPE:
                close();
                break;
            default:
                break;
        }
    }

    private void scanIndexes() {
        Message.info("updating directory indexes...");
        // TODO 除外設定
        repository.removeAll();
        ModelLocator.INSTANCE.getConfigurationStore().getConfiguration().getIndexDirs().forEach(dir -> {
            Path start = Paths.get(dir);
            try {
                Files.walkFileTree(start, new DirectoryScanner(path -> {
                    DirectoryIndex newIndex = new DirectoryIndex();
                    newIndex.setDirPath(path.toString());
                    repository.add(newIndex);
                    Message.info("add directory: " + path);
                }));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        context.save();
        Message.info("directory indexes updated.");

    }

    @FXML
    void handleTextEnter(Event event) {
        jump();
    }

    private void jump() {
        jumpAction.accept(Paths.get(textField.getText()));
        close();
    }

    @Override
    public void preShown() {
        context = new EntitiesContext();
        repository = context.createDirectoryIndexRepository();
    }

    @Override
    public Void getResult() {
        return null;
    }

    @Override
    protected void close() {
        if (context != null) {
            context.close();
            context = null;
        }
        textField.setText("");
        super.close();
    }

}
