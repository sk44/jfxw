/*
 *
 *
 *
 */
package sk44.jfxw.model.process;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExternalProcess {

    private static final String ARGUMENT_PLACEHOLDER = "$arg";
    private static final ExternalProcess EMPTY = new ExternalProcess(null);

    private final List<String> commands;

    public static ExternalProcess empty() {
        return EMPTY;
    }

    public static ExternalProcess of(List<String> commands, Path file) {
        List<String> parsedCommands = commands
            .stream()
            .map(param -> ARGUMENT_PLACEHOLDER.equals(param) ? file.toString() : param)
            .collect(Collectors.toList());
        return new ExternalProcess(parsedCommands);
    }

    public void execute() {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Message.info("exec: " + commands);
        try {
            // TODO 終了を非同期で取れるようにしたい
            // waitFor してもすぐ 0 が返る
            new ProcessBuilder(commands).start();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
