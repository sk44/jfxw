/*
 *
 *
 *
 */
package sk44.jfxw.model.persistence;

import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import sk44.jfxw.model.configuration.ConfigDir;

/**
 *
 * @author sk
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityManagerFactoryProvider {

    private static final String PU_NAME = "jfxwPU";
    private static EntityManagerFactory FACTORY;

    public static void init() {
        Properties props = new Properties();
        props.setProperty("javax.persistence.jdbc.url", "jdbc:sqlite:" + ConfigDir.get().toString() + "/jfxwdb.sqlite");
        FACTORY = Persistence.createEntityManagerFactory(PU_NAME, props);
    }

    static EntityManagerFactory getFactory() {
        return FACTORY;
    }
}
