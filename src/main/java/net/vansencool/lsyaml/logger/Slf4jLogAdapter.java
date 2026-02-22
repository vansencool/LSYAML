package net.vansencool.lsyaml.logger;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LogAdapter} implementation backed by SLF4J.
 * This class is only instantiated when SLF4J is present on the classpath.
 */
public final class Slf4jLogAdapter implements LogAdapter {

    private final @NotNull Logger logger;

    /**
     * Creates a new SLF4J adapter for the given logger name.
     *
     * @param name the logger name
     */
    public Slf4jLogAdapter(@NotNull String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public void debug(@NotNull String message) {
        logger.debug(message);
    }

    @Override
    public void info(@NotNull String message) {
        logger.info(message);
    }

    @Override
    public void warn(@NotNull String message) {
        logger.warn(message);
    }

    @Override
    public void error(@NotNull String message) {
        logger.error(message);
    }

    @Override
    public void error(@NotNull String message, @NotNull Throwable throwable) {
        logger.error(message, throwable);
    }
}
