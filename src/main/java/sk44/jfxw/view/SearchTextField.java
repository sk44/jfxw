/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.util.function.Consumer;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

/**
 * なぜか IME がきかないので modal window で代替
 *
 * @author sk
 */
@Deprecated
public class SearchTextField {

    private static final String CLASS_NAME_TEXT_INPUT = "filerTextInput";

    private final AnchorPane rootPane;
    private final Runnable closeHandler;

    private TextField textField;

    public SearchTextField(AnchorPane rootPane, Runnable closeHandler) {
        this.rootPane = rootPane;
        this.closeHandler = closeHandler;
    }

    public void open(Consumer<String> searchHandler) {
        // 残っているものがあると永久に消えないのでクリアしておく
        if (textField != null) {
            removeTextField();
        }
        // TODO IME をオンにできない
        // スラッシュが入力されてしまうので都度 new する
        textField = new TextField();
        textField.getStyleClass().add(CLASS_NAME_TEXT_INPUT);
        setUpKeyPressedEventHandler(searchHandler);
        // フォーカスアウトで消す
        textField.focusedProperty().addListener((value, oldValue, newValue) -> {
            if (newValue == false) {
                removeTextField();
            }
        });
        textField.prefWidthProperty().bind(rootPane.widthProperty());
        AnchorPane.setBottomAnchor(textField, 0.0);
        rootPane.getChildren().add(textField);
        textField.requestFocus();

    }

    private void setUpKeyPressedEventHandler(Consumer<String> searchHandler) {
        // KEY_PRESSED だと入力中の文字がスルーされる
        textField.addEventFilter(KeyEvent.KEY_RELEASED, (javafx.scene.input.KeyEvent e) -> {
            switch (e.getCode()) {
                case ESCAPE:
                case ENTER:
                    removeTextField();
                    break;
                default:
                    String searchText = textField.getText();
                    if (searchText != null && searchText.isEmpty() == false) {
                        searchHandler.accept(searchText);
                    }
                    break;
            }
        });
    }

    void removeTextField() {
        rootPane.getChildren().remove(textField);
        closeHandler.run();
    }
}
