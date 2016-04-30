/*
 *
 *
 *
 */
package sk44.jfxw.model.persistence;

import java.util.List;
import javax.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import sk44.jfxw.model.fs.index.DirectoryIndex;

/**
 *
 * @author sk
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class DirectoryIndexRepository {

    private final EntityManager em;

    public void add(DirectoryIndex index) {
        this.em.persist(index);
    }

    public int removeAll() {
        return this.em.createQuery("DELETE FROM DirectoryIndex").executeUpdate();
    }

    public List<String> findByPathLike(String path, int limit) {
        return em
            .createQuery(
                "SELECT d.dirPath FROM DirectoryIndex d WHERE UPPER(d.dirPath) LIKE UPPER(:path) ORDER BY d.dirPath",
                String.class)
            .setParameter("path", "%" + path + "%")
            .setFirstResult(0)
            .setMaxResults(limit)
            .getResultList();
    }
}
