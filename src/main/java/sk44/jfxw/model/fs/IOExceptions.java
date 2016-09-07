/*
 *
 *
 *
 */
package sk44.jfxw.model.fs;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 *
 * @author sk
 */
public class IOExceptions {

    public interface RunnableWithIOException {

        void run() throws IOException;
    }

    static void unchecked(RunnableWithIOException action) {

        try {
            action.run();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }
}
