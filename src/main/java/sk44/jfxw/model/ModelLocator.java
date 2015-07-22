/*
 *
 *
 *
 */
package sk44.jfxw.model;

import lombok.Getter;
import lombok.Setter;

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

}
