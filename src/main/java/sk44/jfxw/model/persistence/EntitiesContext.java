/*
 *
 *
 *
 */
package sk44.jfxw.model.persistence;

import javax.persistence.EntityManager;

/**
 *
 * @author sk
 */
public class EntitiesContext implements AutoCloseable {

    private final EntityManager em;
    private boolean committed = false;

    public EntitiesContext() {
        // TODO 遅い
        this.em = EntityManagerFactoryProvider.getFactory().createEntityManager();
        // トランザクションを開始しておかないとクエリ発行時にしぬ？
        beginTransaction();
    }

    public DirectoryIndexRepository createDirectoryIndexRepository() {
        return new DirectoryIndexRepository(em);
    }

    private void beginTransaction() {
        em.getTransaction().begin();
    }

    private void rollbackTransaction() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    public void save() {
        em.getTransaction().commit();
        committed = true;
    }

    @Override
    public void close() {
        if (em != null) {
            if (committed == false) {
                rollbackTransaction();
            }
            em.close();
        }
    }
}
