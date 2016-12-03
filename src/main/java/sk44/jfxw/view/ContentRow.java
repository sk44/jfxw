package sk44.jfxw.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.util.converter.BooleanStringConverter;
import lombok.Getter;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class ContentRow extends FlowPane {

    private static final String MARK_CONTENT_CLASS_NAME = "markContent";
    private static final String CURRENT_ROW_CLASS_NAME = "currentRow";
    private static final String DIRECTORY_ROW_CLASS_NAME = "dirRow";
    private static final String SYM_LINK_ROW_CLASS_NAME = "symlinkRow";
    private static final String MARKED_ROW_CLASS_NAME = "markedRow";
    private static final String PARENT_DIR_NAME = "..";
//    private static final String DIR_NAME_SUFFIX = "/";
    private static final String DIR_NAME_SUFFIX = "";
    private static final String DIR_SIZE_VALUE = "<DIR>";
    private static final String MARK_VALUE = "*";
    private static final String LAST_MODIFIED_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final Color CURSOR_COLOR = Color.LIGHTGREY;
    private static final Image DIRECTORY_ICON;
    private static final Image FILE_ICON;
    private static final double ICON_SIZE = 16;

    static {
        try (InputStream dirStream = ContentRow.class.getResourceAsStream("/images/dir.png");
            InputStream fileStream = ContentRow.class.getResourceAsStream("/images/file.png")) {
            DIRECTORY_ICON = new Image(dirStream, ICON_SIZE, ICON_SIZE, true, true);
            FILE_ICON = new Image(fileStream, ICON_SIZE, ICON_SIZE, true, true);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // TODO シンボリックリンクの表現を考える
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
            return formatFileSize(size);
        } catch (IOException ex) {
            Message.error(ex);
            return null;
        }
    }

    // TODO ここにあるべきか
    public static String formatFileSize(long size) {
        return NumberFormat.getNumberInstance().format(size);
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
        markLabel.prefWidthProperty().set(WIDTH_MARK);
        markLabel.getStyleClass().add(MARK_CONTENT_CLASS_NAME);
        markLabel.textProperty().bindBidirectional(markedProperty, new BooleanStringConverter() {
            @Override
            public String toString(Boolean value) {
                if (value == null) {
                    return "";
                }
                return value ? MARK_VALUE : "";
            }
        });

        final Image iconImage;
        if (isDirectory()) {
            iconImage = DIRECTORY_ICON;
        } else {
            iconImage = FILE_ICON;
        }
        icon.setPreserveRatio(true);
        icon.setImage(iconImage);

        String name = asParent ? PARENT_DIR_NAME : path.getFileName().toString();
        // TODO 色変えるなどだけするほうがスペース的に無理ないかも
        if (isSymbolicLink()) {
            try {
                Path link = Files.readSymbolicLink(path);
                name += "@ -> " + link;
            } catch (IOException ex) {
                Message.error(ex);
            }
        } else if (asParent == false && isDirectory()) {
            name += DIR_NAME_SUFFIX;
        }
        nameLabel = new Label(name);
        nameLabel.prefWidthProperty().bind(widthProperty.multiply(0.4));
        // TODO バインドだとうまく動かない
        /*
        widthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            Message.info("old: " + oldValue + ", new: " + newValue);
//            double w = (double) newValue - WIDTH_MARK - WIDTH_SIZE - WIDTH_LAST_MODIFIED - ICON_SIZE;
//            nameLabel.prefWidthProperty().set(w);
        });
        double a = WIDTH_MARK + WIDTH_SIZE + WIDTH_LAST_MODIFIED + ICON_SIZE * 1;
//        nameLabel.prefWidthProperty().bind(widthProperty.add(a));
         */
        nameLabel.setPadding(new Insets(0, 5, 0, 5));

        sizeLabel = new Label(isDirectory() ? DIR_SIZE_VALUE : formatFileSize(path));
        sizeLabel.getStyleClass().add("sizeContent");
        sizeLabel.prefWidthProperty().set(WIDTH_SIZE);
        sizeLabel.setPadding(new Insets(0, 5, 0, 5));

        lastModified = getLastModified(path);
        lastModifiedLabel = new Label(lastModified == null
            ? ""
            : lastModified.format(DateTimeFormatter.ofPattern(LAST_MODIFIED_DATE_FORMAT)));
        lastModifiedLabel.prefWidthProperty().set(WIDTH_LAST_MODIFIED);

        getChildren().add(markLabel);
        getChildren().add(icon);
        getChildren().add(nameLabel);
        getChildren().add(sizeLabel);
        getChildren().add(lastModifiedLabel);

        if (isSymbolicLink()) {
            getStyleClass().add(SYM_LINK_ROW_CLASS_NAME);
        } else if (isDirectory()) {
            getStyleClass().add(DIRECTORY_ROW_CLASS_NAME);
        }
    }
    private static final int WIDTH_SIZE = 100;
    private static final int WIDTH_LAST_MODIFIED = 150;
    private static final int WIDTH_MARK = 15;

    private final Path path;
    private final boolean asParent;
    private final BooleanProperty markedProperty = new SimpleBooleanProperty(false);
    @Getter
    private final LocalDateTime lastModified;

    private final Label markLabel;
    private final ImageView icon = new ImageView();
    private final Label nameLabel;
    private final Label sizeLabel;
    private final Label lastModifiedLabel;

    public final boolean isDirectory() {
        return Files.isDirectory(path);
    }

    private boolean isSymbolicLink() {
        return Files.isSymbolicLink(path);
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
            Nodes.addStyleClassTo(this, CURRENT_ROW_CLASS_NAME);
        } else {
            Nodes.removeStyleClassFrom(this, CURRENT_ROW_CLASS_NAME);
        }
    }

    public void toggleMark() {
        // TODO 見せ方が微妙
        updateMark(isMarked() == false);
    }

    public void updateMark(boolean mark) {
        if (asParent) {
            return;
        }
        markedProperty.set(mark);
        if (isMarked()) {
            Nodes.addStyleClassTo(this, MARKED_ROW_CLASS_NAME);
        } else {
            Nodes.removeStyleClassFrom(this, MARKED_ROW_CLASS_NAME);
        }
    }

    public boolean isMarked() {
        return markedProperty.get();
    }

}
