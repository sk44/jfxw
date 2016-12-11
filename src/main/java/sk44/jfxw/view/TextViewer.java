/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import sk44.jfxw.controller.FilerViewController;
import sk44.jfxw.model.message.Message;

/**
 * Text viewer.
 *
 * @author sk
 */
public class TextViewer {

    private static final String CSS_CLASS_BACKGROUND = "textPreviewBackground";
    private static final double SCROLL_HEIGHT = 32.0;

    public TextViewer() {
        initialize();
    }

    private final TextArea textArea = new TextArea();
    private Pane basePane;
    private FilerViewController controller;

    public void open(Path textFilePath, Pane basePane, FilerViewController controller) {
        try (Stream<String> line = Files.lines(textFilePath)) {
            line.forEach(text -> textArea.appendText(text + "\n"));
        } catch (IOException ex) {
            Message.error(ex);
        }
        textArea.prefWidthProperty().bind(basePane.widthProperty());
        textArea.prefHeightProperty().bind(basePane.heightProperty());
        basePane.getChildren().add(textArea);
        this.basePane = basePane;
        this.controller = controller;
        textArea.requestFocus();
    }

    private void close() {
        basePane.getChildren().remove(textArea);
        textArea.setText(null);
        controller.onPreviewTextEnd();
        this.basePane = null;
        this.controller = null;
    }

    private void initialize() {
        this.textArea.getStyleClass().add(CSS_CLASS_BACKGROUND);
        this.textArea.setEditable(false);
//        this.textArea.setDisable(true);
//        this.textArea.setMouseTransparent(true);
//        this.textArea.setFocusTraversable(false);
        AnchorPane.setBottomAnchor(textArea, 0.0);
        AnchorPane.setTopAnchor(textArea, 0.0);
        AnchorPane.setLeftAnchor(textArea, 0.0);
        AnchorPane.setRightAnchor(textArea, 0.0);

        this.textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case G:
                    if (e.isShiftDown()) {
                        textArea.setScrollTop(Double.MAX_VALUE);
                    } else {
                        textArea.setScrollTop(0.0);
                    }
                    break;
                case J:
                    // TODO 一番下まで行ってもやり続けると上にいきづらくなる
                    textArea.setScrollTop(textArea.getScrollTop() + SCROLL_HEIGHT);
                    break;
                case K:
                    textArea.setScrollTop(textArea.getScrollTop() - SCROLL_HEIGHT);
                    break;
                case ESCAPE:
                case ENTER:
                    close();
                    break;
                default:
                    break;
            }
        });
    }
}
