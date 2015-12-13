/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

/**
 * ファイラーのテキスト入力欄種別定義。
 *
 * @author sk
 */
enum FilerTextFieldType {

    /**
     * 検索。
     */
    SEARCH {
        @Override
        void setUpKeyPressedEventHandler(FilerViewController controller, TextField textField) {
            textField.addEventFilter(KeyEvent.KEY_PRESSED, (javafx.scene.input.KeyEvent e) -> {
                switch (e.getCode()) {
                    case ESCAPE:
                    case ENTER:
                        controller.removeTextField();
                        break;
                    default:
                        String searchText = textField.getText();
                        if (searchText != null && searchText.isEmpty() == false) {
                            controller.setSearchText(searchText);
                            controller.searchNext();
                        }
                        break;
                }
            });
        }
    },
    /**
     * ディレクトリ作成。
     */
    CREATE_DIR {
        @Override
        void setUpKeyPressedEventHandler(FilerViewController controller, TextField textField) {
            textField.addEventFilter(KeyEvent.KEY_PRESSED, (javafx.scene.input.KeyEvent e) -> {
                switch (e.getCode()) {
                    case ESCAPE:
                        controller.removeTextField();
                        break;
                    case ENTER:
                        String text = textField.getText();
                        if (text != null && text.isEmpty() == false) {
                            controller.getFiler().createDirectory(text);
                            controller.removeTextField();
                        }
                        break;
                    default:
                        break;
                }
            });
        }
    };

    abstract void setUpKeyPressedEventHandler(FilerViewController controller, TextField textField);

}
