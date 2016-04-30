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
class EntityManagerFactoryProvider {

    private static final String PU_NAME = "jfxwPU";
    private static final EntityManagerFactory FACTORY;

    // TODO 初期化が遅いので、起動時にやってしまうのを検討
    static {
        Properties props = new Properties();
        props.setProperty("javax.persistence.jdbc.url", "jdbc:derby:" + ConfigDir.get().toString() + "/jfxwdb;create=true");
        FACTORY = Persistence.createEntityManagerFactory(PU_NAME, props);
    }

    public static EntityManagerFactory getFactory() {
        return FACTORY;
    }
}
