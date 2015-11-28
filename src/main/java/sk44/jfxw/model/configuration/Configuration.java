package sk44.jfxw.model.configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.message.MessageLevel;

/**
 *
 * @author sk
 */
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Configuration {

    private static final String ARGUMENT_PLACEHOLDER = "$arg";

    static Configuration defaultValue() {

        Configuration configuration = new Configuration();
        configuration.logLevel = MessageLevel.defaultLevel();

        return configuration;
    }

    private static String createDefaultPath() {
        return new File(".").toPath().normalize().toString();
    }

    @Getter
    @Setter
    private Map<String, List<String>> fileAssociations = new HashMap<>();

    @Getter
    @Setter
    private List<String> previewCommand;

    @Getter
    @Setter
    private MessageLevel logLevel;

    @Getter
    @Setter
    private String backgroundImagePath;

    @Setter
    private FilerConfig leftFilerConfig;

    @Setter
    private FilerConfig rightFilerConfig;

    public Optional<Path> backgroundImagePath() {
        if (backgroundImagePath == null || backgroundImagePath.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(backgroundImagePath));
    }

    public FilerConfig getLeftFilerConfig() {
        if (leftFilerConfig == null) {
            leftFilerConfig = FilerConfig.defaultConfig(createDefaultPath());
        }
        return leftFilerConfig;
    }

    public FilerConfig getRightFilerConfig() {
        if (rightFilerConfig == null) {
            rightFilerConfig = FilerConfig.defaultConfig(createDefaultPath());
        }
        return rightFilerConfig;
    }

    public void updateLeftFilerConfig(Filer leftFiler) {
        getLeftFilerConfig().update(leftFiler);
    }

    public void updateRightFilerConfig(Filer rightFiler) {
        getRightFilerConfig().update(rightFiler);
    }

    public Optional<List<String>> getAssociatedCommandFor(Path file) {
        String fileName = file.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            String extension = fileName.substring(i + 1);
            if (fileAssociations.containsKey(extension)) {
                return Optional.of(parseCommand(fileAssociations.get(extension), file));
            }
        }
        if (previewCommand != null && previewCommand.isEmpty() == false) {
            return Optional.of(parseCommand(previewCommand, file));
        }
        Message.info("No commands associated.");
        return Optional.empty();
    }

    private List<String> parseCommand(List<String> commands, Path file) {
        return commands
            .stream()
            .map(param -> ARGUMENT_PLACEHOLDER.equals(param) ? file.toString() : param)
            .collect(Collectors.toList());
    }
}
