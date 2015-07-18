/*
 *
 *
 *
 */
package sk44.jfxw.controller;

import java.nio.file.Path;

/**
 *
 * @author sk
 */
@FunctionalInterface
interface PathExecutor {

    boolean tryExecute(Path path);
}
