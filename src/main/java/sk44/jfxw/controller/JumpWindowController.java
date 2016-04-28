/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

    // TODO パスをどこかにおさえておく
    private final List<String> pathes = Arrays.asList("foo", "bar", "buzz", "テスト");

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
        // TODO case insensitive
        List<CustomMenuItem> newItems = pathes.stream()
            .filter(path -> path.contains(query))
            .limit(MAX_COMPLETE_LIMIT)
            .map(path -> {
                CustomMenuItem item = new CustomMenuItem(new Label(path), true);
                item.setOnAction(event -> {
                    textField.setText(path);
                    completePupup.hide();
                });
                return item;
            })
            .collect(Collectors.toList());
        completePupup.getItems().clear();
        completePupup.getItems().addAll(newItems);

        if (completePupup.isShowing()) {
            return;
        }
        completePupup.show(textField, Side.BOTTOM, 0, 0);
    }

    protected void handleCommandKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                close();
                break;
            default:
                break;
        }

    }

    @FXML
    void handleTextEnter(Event event) {
//        update();
        // TODO 絞り込む？
        jumpAction.accept(Paths.get(textField.getText()));
        close();
    }

    @Override
    public Void getResult() {
        return null;
    }
}
