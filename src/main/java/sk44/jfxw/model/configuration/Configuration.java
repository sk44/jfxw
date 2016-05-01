package sk44.jfxw.model.configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sk44.jfxw.model.Filer;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.message.MessageLevel;
import sk44.jfxw.model.process.ExternalProcess;

/**
 *
 * @author sk
 */
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Configuration {

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
    private List<String> indexDirs;

    @Getter
    @Setter
    private MessageLevel logLevel;

    @Getter
    @Setter
    private String backgroundImagePath;

    @Getter
    @Setter
    private String backgroundImageDir;

    @Getter
    @Setter
    private List<String> editorCommand;

    @Getter
    @Setter
    private String mainFont;

    @Setter
    private FilerConfig leftFilerConfig;

    @Setter
    private FilerConfig rightFilerConfig;

    public Optional<String> mainFont() {
        if (mainFont == null || mainFont.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mainFont);
    }

    public Optional<Path> backgroundImagePath() {
        if (backgroundImagePath == null || backgroundImagePath.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(backgroundImagePath));
    }

    public Optional<Path> backgroundImageDir() {
        if (backgroundImageDir == null || backgroundImageDir.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(backgroundImageDir));
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

    public ExternalProcess getAssociatedProcessFor(Path file) {
        Optional<String> extension = Filer.extensionOf(file);
        return extension
            .map(ext -> {
                if (fileAssociations.containsKey(ext)) {
                    return ExternalProcess.of(fileAssociations.get(ext), file);
                }
                // 拡張子が定義されてなければデフォルトプレビュー実行
                if (previewCommand != null && previewCommand.isEmpty() == false) {
                    return ExternalProcess.of(previewCommand, file);
                }
                Message.info("No commands associated.");
                return ExternalProcess.empty();
            })
            .orElse(ExternalProcess.empty());
    }

    public ExternalProcess getEditorProcessFor(Path file) {
        if (editorCommand != null && editorCommand.isEmpty() == false) {
            return ExternalProcess.of(editorCommand, file);
        }
        Message.info("Editor command not specified.");
        return ExternalProcess.empty();
    }

}
