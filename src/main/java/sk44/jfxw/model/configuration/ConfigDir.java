/*
 *
 *
 *
 */
package sk44.jfxw.model.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author sk
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigDir {

    public static Path get() {
        String configDirPath = Optional.ofNullable(System.getProperty("user.home")).orElse(".");
        return Paths.get(configDirPath).toAbsolutePath();
    }
}
