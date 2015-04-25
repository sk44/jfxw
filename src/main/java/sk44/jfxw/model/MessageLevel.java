/*
 *
 *
 *
 */
package sk44.jfxw.model;

/**
 *
 * @author sk
 */
public enum MessageLevel {

    TRACE(1, "TRACE"),
    DEBUG(2, "DEBUG"),
    INFO(3, "INFO"),
    WARN(4, "WARN"),
    ERROR(5, "ERROR");

    private final int level;
    private final String prefix;

    private MessageLevel(int level, String prefix) {
        this.level = level;
        this.prefix = prefix;
    }

    String formatMessage(String message, MessageLevel minLevel) {
        if (level < minLevel.level) {
            return null;
        }
        return "[" + prefix + "] " + message;
    }

}
