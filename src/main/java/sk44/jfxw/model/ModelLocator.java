/*
 *
 *
 *
 */
package sk44.jfxw.model;

import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.configuration.ConfigurationStore;
import sk44.jfxw.model.fs.FileSystem;

/**
 *
 * @author sk
 */
public enum ModelLocator {

    INSTANCE;

    @Getter
    @Setter
    private ConfigurationStore configurationStore;

    @Getter
    @Setter
    private Filer rightFiler;

    @Getter
    @Setter
    private Filer leftFiler;

    @Getter
    @Setter
    private FileSystem fileSystem;

    @Getter
    @Setter
    private BackgroundImage backgroundImage;
}
