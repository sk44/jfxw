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
        // TODO 設定ファイルと場所を揃える
        String home = System.getProperty("user.home");
        if (home == null) {
            FACTORY = Persistence.createEntityManagerFactory(PU_NAME);
        } else {
            Properties props = new Properties();
            props.setProperty("javax.persistence.jdbc.url", "jdbc:derby:" + home + "/jfxwdb;create=true");
            FACTORY = Persistence.createEntityManagerFactory(PU_NAME, props);
        }
    }

    public static EntityManagerFactory getFactory() {
        return FACTORY;
    }
}
