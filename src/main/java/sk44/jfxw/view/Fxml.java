/*
 *
 *
 *
 */
package sk44.jfxw.view;

/**
 *
 * @author sk
 */
public enum Fxml {

    FILER_VIEW("FilerView"),
    MAIN_WINDOW("MainWindow"),
    SORT_WINDOW("SortWindow"),
    TEXT_FIELD_WINDOW("TextFieldWindow");

    private Fxml(String name) {
        this.name = name;
    }

    private final String name;

    public String getPath() {
        return "/fxml/" + name + ".fxml";
    }
}
