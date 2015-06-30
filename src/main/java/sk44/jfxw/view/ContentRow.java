package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.util.converter.BooleanStringConverter;
import lombok.Getter;
import sk44.jfxw.model.Message;

/**
 *
 * @author sk
 */
public class ContentRow extends FlowPane {

    private static final String CURRENT_ROW_CLASS_NAME = "currentRow";
    private static final String DIRECTORY_ROW_CLASS_NAME = "dirRow";
    private static final String MARKED_ROW_CLASS_NAME = "markedRow";
    private static final String PARENT_DIR_NAME = "..";
    private static final String DIR_NAME_SUFFIX = "/";
    private static final String DIR_SIZE_VALUE = "<DIR>";
    private static final String MARK_VALUE = "*";
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

    public static ContentRow forParent(Path path, ReadOnlyDoubleProperty widthProperty) {
        return new ContentRow(path, widthProperty, true);
    }

    private ContentRow(Path path, ReadOnlyDoubleProperty widthProperty, boolean asParent) {

        this.path = path;
        prefWidthProperty().bind(widthProperty);
        this.asParent = asParent;

        markLabel = new Label();
        markLabel.prefWidthProperty().set(15);
        markLabel.getStyleClass().add("markContent");
        markLabel.textProperty().bindBidirectional(markedProperty, new BooleanStringConverter() {
            @Override
            public String toString(Boolean value) {
                if (value == null) {
                    return "";
                }
                return value ? MARK_VALUE : "";
            }
        });

        String name = asParent ? PARENT_DIR_NAME : path.getFileName().toString();
        if (asParent == false && isDirectory()) {
            name += DIR_NAME_SUFFIX;
        }
        nameLabel = new Label(name);
        nameLabel.prefWidthProperty().bind(widthProperty.multiply(0.45));

        sizeLabel = new Label(isDirectory() ? DIR_SIZE_VALUE : formatFileSize(path));
        sizeLabel.getStyleClass().add("sizeContent");
        // TODO 幅を計算する
        sizeLabel.prefWidthProperty().bind(widthProperty.multiply(0.2));
        sizeLabel.setPadding(new Insets(0, 5, 0, 5));

        lastModified = getLastModified(path);
        lastModifiedLabel = new Label(lastModified == null
            ? ""
            : lastModified.format(DateTimeFormatter.ofPattern(LAST_MODIFIED_DATE_FORMAT)));
//        lastModifiedLabel.prefWidthProperty().bind(widthProperty.multiply(0.35));
        lastModifiedLabel.maxWidthProperty().set(150);

        getChildren().add(markLabel);
        getChildren().add(nameLabel);
        getChildren().add(sizeLabel);
        getChildren().add(lastModifiedLabel);

        if (isDirectory()) {
            getStyleClass().add(DIRECTORY_ROW_CLASS_NAME);
        }
    }

    private final Path path;
    private final boolean asParent;
    private final BooleanProperty markedProperty = new SimpleBooleanProperty(false);
    @Getter
    private final LocalDateTime lastModified;

    private final Label markLabel;
    private final Label nameLabel;
    private final Label sizeLabel;
    private final Label lastModifiedLabel;

    public final boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public boolean isParent() {
        return asParent;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return this.nameLabel.getText();
    }

    public boolean isNameMatch(String text) {
        if (text == null) {
            return false;
        }
        return getName().toLowerCase().contains(text.toLowerCase());
    }

    public void updateSelected(boolean selected) {

        if (selected) {
            getStyleClass().add(CURRENT_ROW_CLASS_NAME);
        } else {
            getStyleClass().remove(CURRENT_ROW_CLASS_NAME);
        }
    }

    public void toggleMark() {
        if (asParent) {
            return;
        }
        // TODO 見せ方が微妙
        markedProperty.set(markedProperty.get() == false);
        if (markedProperty.get()) {
            getStyleClass().add(MARKED_ROW_CLASS_NAME);
        } else {
            getStyleClass().remove(MARKED_ROW_CLASS_NAME);
        }
    }

    public boolean isMarked() {
        return markedProperty.get();
    }

}
