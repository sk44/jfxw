/*
 *
 *
 *
 */
package sk44.jfxw.model;

import java.util.Arrays;

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

    public static MessageLevel ofName(String name) {
        return Arrays
            .stream(MessageLevel.values())
            .filter(level -> level.name.equalsIgnoreCase(name))
            .findFirst()
            .orElse(DEBUG);
    }

    private final int level;
    private final String name;

    private MessageLevel(int level, String name) {
        this.level = level;
        this.name = name;
    }

    String formatMessage(String message, MessageLevel minLevel) {
        if (level < minLevel.level) {
            return null;
        }
        return "[" + name + "] " + message;
    }

}
