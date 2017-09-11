/*
 *
 *
 *
 */
package sk44.jfxw.controller.modal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * テキストフィールドウィンドウタイトル定義。
 *
 * @author sk
 */
@RequiredArgsConstructor
public enum TextFieldWindowTitle {

    /**
    新規テキストファイル作成。
     */
    NEW_TEXT("NEW TEXT"),
    /**
    新規ディレクトリ作成。
     */
    NEW_DIR("NEW DIRECTORY"),
    /**
    検索。
     */
    SEARCH("SEARCH"),
    /**
    リネーム。
     */
    RENAME("RENAME");

    @Getter
    private final String title;
}
