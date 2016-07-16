/*
 *
 *
 *
 */
package sk44.jfxw.model;

import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.configuration.ConfigurationStore;
import sk44.jfxw.model.persistence.EntitiesContext;

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
    private BackgroundImage backgroundImage;

    @Getter
    @Setter
    private EntitiesContext entitiesContext;
}
