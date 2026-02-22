package net.vansencool.lsyaml.logger;

import org.jetbrains.annotations.NotNull;

/**
 * Abstraction over a logging backend used by LSYAML.
 * Implement this interface to route LSYAML log output to any logging system.
 *
 * @see LSYAMLLogger
 */
public interface LogAdapter {

    /**
     * Logs a debug message.
     *
     * @param message the message to log
     */
    void debug(@NotNull String message);

    /**
     * Logs an informational message.
     *
     * @param message the message to log
     */
    void info(@NotNull String message);

    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    void warn(@NotNull String message);

    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    void error(@NotNull String message);

    /**
     * Logs an error message with an associated throwable.
     *
     * @param message   the message to log
     * @param throwable the associated throwable
     */
    void error(@NotNull String message, @NotNull Throwable throwable);
}
