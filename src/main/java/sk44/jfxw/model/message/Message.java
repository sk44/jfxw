/*
 *
 *
 *
 */
package sk44.jfxw.model.message;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sk
 */
public class Message {

    private enum Handlers {

        INSTANCE;

        private final List<MessageWriter> writers = new ArrayList<>();
        private final List<Runnable> clearHandlers = new ArrayList<>();

        public void write(String message) {
            if (message == null || message.isEmpty()) {
                return;
            }
            writers.forEach(writer -> writer.write(message));
        }

        public void clear() {
            if (clearHandlers == null || clearHandlers.isEmpty()) {
                return;
            }
            clearHandlers.forEach(handler -> handler.run());
        }

        public void addWriter(MessageWriter writer) {
            writers.add(writer);
        }

        public void addClearHandler(Runnable clearer) {
            clearHandlers.add(clearer);
        }

    }

    private static MessageLevel minLevel = MessageLevel.INFO;

    public static void minLevel(MessageLevel level) {
        minLevel = level;
    }

    private static void write(String message, MessageLevel level) {
        Handlers.INSTANCE.write(level.formatMessage(message, minLevel));
    }

    public static void clear() {
        Handlers.INSTANCE.clear();
        ready();
    }

    public static void ready() {
        info("Ready.");
    }

    public static void trace(String message) {
        write(message, MessageLevel.TRACE);
    }

    public static void debug(String message) {
        write(message, MessageLevel.DEBUG);
    }

    public static void info(String message) {
        write(message, MessageLevel.INFO);
    }

    public static void warn(String message) {
        write(message, MessageLevel.WARN);
    }

    public static void error(String message) {
        write(message, MessageLevel.ERROR);
    }

    public static void error(Throwable t) {
        write(t.getLocalizedMessage(), MessageLevel.ERROR);
        try {
            write(convertStackTraceToString(t), MessageLevel.ERROR);
        } catch (IOException ex) {
            // TODO うーむ
            write(t.toString(), MessageLevel.ERROR);
            write("An error occured during getting stacktrace from exception.", MessageLevel.ERROR);
            write(ex.getLocalizedMessage(), MessageLevel.ERROR);
        }
    }

    public static void addWriter(MessageWriter writer) {
        Handlers.INSTANCE.addWriter(writer);
    }

    public static void addClearHandler(Runnable clearHandler) {
        Handlers.INSTANCE.addClearHandler(clearHandler);
    }

    private static String convertStackTraceToString(Throwable t) throws IOException {
        try (StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        }
    }

}
