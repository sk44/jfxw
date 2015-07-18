package sk44.jfxw.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sk
 */
public class Configuration {

    private static final String CONFIG_FILE_NAME = "jfxw.json";
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    private static Configuration instance;
    private static File configFile;

    public static void initialize(File configFileDir) throws IOException {
        configFile = new File(configFileDir, CONFIG_FILE_NAME);
        instance = read(configFile);
    }

    public static Configuration get() {
        Objects.requireNonNull(instance);
        return instance;
    }

    public static void save() throws IOException {
        mapper.writeValue(configFile, instance);
    }

    private static Configuration read(File sourceFile) throws IOException {
        if (sourceFile.exists() == false) {
            // TODO  デフォルト値
            return new Configuration();
        }

        return mapper.readValue(sourceFile, Configuration.class);
    }

    public Configuration() {
    }

    @Getter
    @Setter
    private Map<String, String> fileAssociations = new HashMap<>();
    @Getter
    @Setter
    private String logLevel;

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
