package net.vansencool.lsyaml.logger;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link LogAdapter} implementation backed by Java Util Logging.
 * Used as the default fallback when no other logging backend is detected.
 */
public final class JulLogAdapter implements LogAdapter {

    private final @NotNull Logger logger;

    /**
     * Creates a new JUL adapter for the given logger name.
     *
     * @param name the logger name
     */
    public JulLogAdapter(@NotNull String name) {
        this.logger = Logger.getLogger(name);
    }

    @Override
    public void debug(@NotNull String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    public void info(@NotNull String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    public void warn(@NotNull String message) {
        logger.log(Level.WARNING, message);
    }

    @Override
    public void error(@NotNull String message) {
        logger.log(Level.SEVERE, message);
    }

    @Override
    public void error(@NotNull String message, @NotNull Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
