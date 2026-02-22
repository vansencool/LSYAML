package net.vansencool.lsyaml.logger;

import org.jetbrains.annotations.NotNull;

/**
 * Central logger for LSYAML.
 * <p>By default, it automatically detects the logging framework to use:
 * if SLF4J is on the classpath it delegates to {@link Slf4jLogAdapter},
 * otherwise it falls back to {@link JulLogAdapter} (Java Util Logging).</p>
 *
 * <p>You can replace the adapter at any time by calling {@link #setAdapter(LogAdapter)}.</p>
 */
@SuppressWarnings("unused")
public final class LSYAMLLogger {

    private static final @NotNull String LOGGER_NAME = "LSYAML";

    private static @NotNull LogAdapter adapter = detectAdapter();

    private LSYAMLLogger() {}

    @NotNull
    private static LogAdapter detectAdapter() {
        try {
            Class.forName("org.slf4j.LoggerFactory");
            return new Slf4jLogAdapter(LOGGER_NAME);
        } catch (ClassNotFoundException ignored) {
            return new JulLogAdapter(LOGGER_NAME);
        }
    }

    /**
     * Replaces the active logging adapter.
     *
     * @param newAdapter the adapter to use
     */
    public static void setAdapter(@NotNull LogAdapter newAdapter) {
        adapter = newAdapter;
    }

    /**
     * Returns the currently active logging adapter.
     *
     * @return the active adapter
     */
    @NotNull
    public static LogAdapter getAdapter() {
        return adapter;
    }

    /**
     * Logs a debug message.
     *
     * @param message the message to log
     */
    public static void debug(@NotNull String message) {
        adapter.debug(message);
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to log
     */
    public static void info(@NotNull String message) {
        adapter.info(message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    public static void warn(@NotNull String message) {
        adapter.warn(message);
    }

    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    public static void error(@NotNull String message) {
        adapter.error(message);
    }

    /**
     * Logs an error message with an associated throwable.
     *
     * @param message   the message to log
     * @param throwable the associated throwable
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        adapter.error(message, throwable);
    }
}
