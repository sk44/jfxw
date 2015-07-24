package sk44.jfxw.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk44.jfxw.model.message.Message;
import sk44.jfxw.model.message.MessageLevel;

/**
 *
 * @author sk
 */
@NoArgsConstructor
public class Configuration {

    static Configuration defaultValue() {
        Configuration configuration = new Configuration();
        configuration.logLevel = MessageLevel.defaultLevel().name();
        String defaultPath = new File(".").toPath().normalize().toString();
        configuration.leftPath = defaultPath;
        configuration.rightPath = defaultPath;

        return configuration;
    }

    @Getter
    @Setter
    private Map<String, String> fileAssociations = new HashMap<>();

    @Getter
    @Setter
    private String logLevel;

    @Getter
    @Setter
    private String leftPath;

    @Getter
    @Setter
    private String rightPath;

    @JsonIgnore
    public Path getLeftDir() {
        return Paths.get(leftPath).normalize();
    }

    @JsonIgnore
    public Path getRightDir() {
        return Paths.get(rightPath).normalize();
    }

    @JsonIgnore
    public MessageLevel getMessageLevel() {
        return MessageLevel.ofName(getLogLevel());
    }

//    @JsonIgnore
    public Optional<String> getAssociatedCommandFor(Path file) {
        String fileName = file.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            String extension = fileName.substring(i + 1);
            if (fileAssociations.containsKey(extension)) {
                return Optional.of(fileAssociations.get(extension));
            }
        }
        Message.info("No commands associated.");
        return Optional.empty();
    }
}
