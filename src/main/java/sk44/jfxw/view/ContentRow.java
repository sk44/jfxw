package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import sk44.jfxw.model.Message;

/**
 *
 * @author sk
 */
public class ContentRow extends FlowPane {

    private static final String CURRENT_ROW_CLASS_NAME = "currentRow";
    private static final String PARENT_DIR_NAME = "..";
    private static final String DIR_SIZE_VALUE = "<DIR>";
    private static final String LAST_MODIFIED_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final Color CURSOR_COLOR = Color.LIGHTGREY;

    public static LocalDateTime getLastModified(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(9));
        } catch (IOException ex) {
            Message.error(ex);
            return null;
        }
    }

    public static String formatFileSize(Path path) {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("path: " + path.toString() + " is a directory.");
        }
        try {
            long size = Files.size(path);
            return NumberFormat.getNumberInstance().format(size);
        } catch (IOException ex) {
            Message.error(ex);
            return null;
        }
    }

    public static ContentRow create(Path path, ReadOnlyDoubleProperty widthProperty) {

        ContentRow contentRow = new ContentRow(path, widthProperty, false);
        return contentRow;
    }

    public static ContentRow forParent(Path path, ReadOnlyDoubleProperty wiDoubleProperty) {
        return new ContentRow(path, wiDoubleProperty, true);
    }

    // TODO css class でいいのでは
    private static Background createBackground(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    private ContentRow(Path path, ReadOnlyDoubleProperty widthProperty, boolean asParent) {

        this.path = path;
        prefWidthProperty().bind(widthProperty);

        // TODO いらないかも？
        mark = new Label("");
        mark.prefWidthProperty().set(5);

        String name = asParent ? PARENT_DIR_NAME : path.getFileName().toString();
        if (asParent == false && isDirectory()) {
            name += "/";
        }
        nameLabel = new Label(name);
        nameLabel.prefWidthProperty().bind(widthProperty.multiply(0.45));

        sizeLabel = new Label(isDirectory() ? DIR_SIZE_VALUE : formatFileSize(path));
        sizeLabel.getStyleClass().add("sizeContent");
        // TODO 幅を計算する
        sizeLabel.prefWidthProperty().bind(widthProperty.multiply(0.2));
        sizeLabel.setPadding(new Insets(0, 5, 0, 5));

        LocalDateTime lastModified = getLastModified(path);
        lastModifiedLabel = new Label(lastModified == null
            ? ""
            : lastModified.format(DateTimeFormatter.ofPattern(LAST_MODIFIED_DATE_FORMAT)));
//        lastModifiedLabel.prefWidthProperty().bind(widthProperty.multiply(0.35));
        lastModifiedLabel.maxWidthProperty().set(150);

        getChildren().add(mark);
        getChildren().add(nameLabel);
        getChildren().add(sizeLabel);
        getChildren().add(lastModifiedLabel);
    }

    private final Path path;
    private final Label mark;
    private final Label nameLabel;
    private final Label sizeLabel;
    private final Label lastModifiedLabel;

    public final boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return this.nameLabel.getText();
    }

    public boolean isNameMatch(String text) {
        return getName().contains(text);
    }

    public void updateSelected(boolean selected) {

        if (selected) {
            getStyleClass().add(CURRENT_ROW_CLASS_NAME);
        } else {
            getStyleClass().remove(CURRENT_ROW_CLASS_NAME);
        }
    }

}
