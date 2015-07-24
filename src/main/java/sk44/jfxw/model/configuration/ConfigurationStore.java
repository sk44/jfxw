/*
 *
 *
 *
 */
package sk44.jfxw.model.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 *
 * @author sk
 */
public class ConfigurationStore {

    private static final String CONFIG_FILE_NAME = "jfxw.json";
    private static final ObjectMapper mapper;

    private static Configuration read(File sourceFile) throws IOException {
        if (sourceFile.exists() == false) {
            return Configuration.defaultValue();
        }

        return mapper.readValue(sourceFile, Configuration.class);
    }

    static {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private Configuration configuration;
    private File configFile;

    public void init(File configFileDir) throws IOException {
        configFile = new File(configFileDir, CONFIG_FILE_NAME);
        configuration = read(configFile);
    }

    public Configuration getConfiguration() {
        Objects.requireNonNull(configuration);
        return configuration;
    }

    public void save() throws IOException {
        mapper.writeValue(configFile, configuration);
    }
}
