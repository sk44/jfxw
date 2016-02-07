/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class CurrentPathInfoBox {

    private static void executeActionToPath(Path path, Consumer<Stream<Path>> consumer) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            Stream<Path> stream = StreamSupport.stream(ds.spliterator(), false);
            consumer.accept(stream);
        } catch (IOException ex) {
            Message.error(ex);
        }
    }

    private static long countDirectories(Stream<Path> stream) {
        return stream.filter(path -> Files.isDirectory(path)).count();
    }

    private static long countFiles(Stream<Path> stream) {
        return stream.filter(path -> Files.isRegularFile(path)).count();
    }

    private static long sumFileSize(Stream<Path> stream) {
        return stream
            .filter(path -> Files.isDirectory(path) == false)
            .map(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ex) {
                    Message.error(ex);
                    return 0l;
                }
            })
            .mapToLong(s -> s)
            .sum();
    }

    private final HBox container;
    private final Label dirCountLabel;
    private final Label fileCountLabel;
    private final Label fileSizeLabel;

    public CurrentPathInfoBox() {
        this.container = new HBox();
        container.setPrefHeight(20);
        container.setMaxHeight(20);
        container.setAlignment(Pos.CENTER);

        AnchorPane.setBottomAnchor(container, 0.0);
        AnchorPane.setLeftAnchor(container, 0.0);
        AnchorPane.setRightAnchor(container, 0.0);

        this.dirCountLabel = new Label();
        this.fileCountLabel = new Label();
        this.fileSizeLabel = new Label();

        dirCountLabel.prefWidthProperty().bind(container.widthProperty().multiply(0.3));
        fileCountLabel.prefWidthProperty().bind(container.widthProperty().multiply(0.3));
        fileSizeLabel.prefWidthProperty().bind(container.widthProperty().multiply(0.3));

        container.getChildren().addAll(dirCountLabel, fileCountLabel, fileSizeLabel);
    }

    public void addTo(AnchorPane parent) {

        parent.getChildren().add(container);
    }

    public void update(Path currentPath) {

        executeActionToPath(currentPath, path -> {
            updateDirCountLabel(countDirectories(path));
        });
        executeActionToPath(currentPath, path -> {
            updateFileCountLabel(countFiles(path));
        });
        executeActionToPath(currentPath, path -> {
            updateFileSizeLabel(sumFileSize(path));
        });
    }

    private void updateDirCountLabel(long dirCount) {
        dirCountLabel.setText(dirCount + " Dir");
    }

    private void updateFileCountLabel(long fileCount) {
        fileCountLabel.setText(fileCount + " Files");
    }

    private void updateFileSizeLabel(long fileSize) {
        fileSizeLabel.setText(ContentRow.formatFileSize(fileSize) + " Bytes");
    }
}
