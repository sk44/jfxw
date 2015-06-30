/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.nio.file.Path;

/**
 *
 * @author sk
 */
@FunctionalInterface
public interface PathExecutor {

    boolean tryExecute(Path path);
}
