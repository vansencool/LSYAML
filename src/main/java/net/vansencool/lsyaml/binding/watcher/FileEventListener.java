package net.vansencool.lsyaml.binding.watcher;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Listener for file system events on a watched config file.
 * Implement this to react to create, modify, or delete events in addition to the
 * built-in automatic reload/recreate behaviour defined in {@link WatcherOptions}.
 */
@FunctionalInterface
public interface FileEventListener {

    /**
     * Called when a file system event occurs on a watched config file.
     * This is invoked after the built-in reload/recreate logic has already run,
     * on a background thread. Implementations must be thread-safe.
     *
     * @param file   the absolute path of the affected file
     * @param action the type of event that occurred
     */
    void onEvent(@NotNull Path file, @NotNull WatchAction action);
}
