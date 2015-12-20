/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

/**
 *
 * @author sk
 */
@FunctionalInterface
public interface OverwriteFileConfirmer {

    boolean confirm(String message);

}
