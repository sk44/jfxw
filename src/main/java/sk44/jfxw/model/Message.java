/*
 *
 *
 *
 */
package sk44.jfxw.model;

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

    private enum Subject {

        INSTANCE;

        private final List<MessageObserver> observers = new ArrayList<>();

        public void notifyObservers(String message) {
            if (message == null || message.length() <= 0) {
                return;
            }
            observers.stream().forEach(observer -> observer.update(message));
        }

        public void addObserver(MessageObserver observer) {
            observers.add(observer);
        }

        public void clearObservers() {
            observers.clear();
        }
    }

    private static MessageLevel minLevel = MessageLevel.INFO;

    public static void minLevel(MessageLevel level) {
        minLevel = level;
    }

    private static void output(String message, MessageLevel level) {
        Subject.INSTANCE.notifyObservers(level.formatMessage(message, minLevel));
    }

    public static void debug(String message) {
        output(message, MessageLevel.DEBUG);
    }

    public static void info(String message) {
        output(message, MessageLevel.INFO);
    }

    public static void warn(String message) {
        output(message, MessageLevel.WARN);
    }

    public static void error(String message) {
        output(message, MessageLevel.ERROR);
    }

    public static void error(Throwable t) {
        output(t.getLocalizedMessage(), MessageLevel.ERROR);
        try {
            output(convertStackTraceToString(t), MessageLevel.ERROR);
        } catch (IOException ex) {
            // TODO うーむ
            output(t.toString(), MessageLevel.ERROR);
            output("An error occured during getting stacktrace from exception.", MessageLevel.ERROR);
            output(ex.getLocalizedMessage(), MessageLevel.ERROR);
        }
    }

    public static void addObserver(MessageObserver observer) {
        Subject.INSTANCE.addObserver(observer);
    }

    public static void clearAllObservers() {
        Subject.INSTANCE.clearObservers();
    }

    private static String convertStackTraceToString(Throwable t) throws IOException {
        try (StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        }
    }

}
