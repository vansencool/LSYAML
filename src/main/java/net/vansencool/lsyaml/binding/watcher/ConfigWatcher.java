package net.vansencool.lsyaml.binding.watcher;

import net.vansencool.lsyaml.binding.ConfigFile;
import net.vansencool.lsyaml.binding.ConfigLoader;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Watches config files on disk and automatically reacts to file system events.
 * Each config class registered with {@link ConfigLoader} can be independently watched
 * with its own {@link WatcherOptions}.
 *
 * <p>The watcher runs on a background daemon thread and uses a debounce window to
 * collapse rapid successive events into a single dispatch.</p>
 *
 * <p><b>Example - watch with defaults:</b></p>
 * <pre>{@code
 * ConfigLoader.load(MyConfig.class);
 * ConfigWatcher.watch(MyConfig.class);
 * }</pre>
 *
 * <p><b>Example - watch with custom options:</b></p>
 * <pre>{@code
 * ConfigWatcher.watch(MyConfig.class, WatcherOptions.builder()
 *     .debounceMillis(500)
 *     .reloadOnChange(true)
 *     .recreateOnDelete(true)
 *     .loadOnCreate(true)
 *     .listener((file, action) -> System.out.println("Event: " + action + " on " + file))
 *     .build());
 * }</pre>
 *
 * <p>Call {@link #shutdown()} when the watcher is no longer needed to release resources.</p>
 */
@SuppressWarnings("unused")
public final class ConfigWatcher {

    private static final ConcurrentHashMap<Path, ConcurrentHashMap<Class<?>, WatcherOptions>> fileToWatchers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, WatchKey> dirToKey = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, AtomicInteger> dirRefCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, ScheduledFuture<?>> pendingDebounce = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "lsyaml-watcher");
        t.setDaemon(true);
        return t;
    });

    private static volatile WatchService watchService = null;
    private static volatile Thread pollThread = null;
    private static volatile boolean running = false;

    private ConfigWatcher() {
    }

    /**
     * Starts watching the config file for the given class with default {@link WatcherOptions}.
     * The class must be annotated with {@link ConfigFile}.
     * If the class is already being watched, its options are updated.
     *
     * @param cls the config class to watch
     * @throws IllegalArgumentException if the class is not annotated with {@link ConfigFile}
     */
    public static void watch(@NotNull Class<?> cls) {
        watch(cls, WatcherOptions.defaults());
    }

    /**
     * Starts watching the config file for the given class with the given options.
     * The class must be annotated with {@link ConfigFile}.
     * If the class is already being watched, its options are updated.
     *
     * @param cls     the config class to watch
     * @param options the watcher options
     * @throws IllegalArgumentException if the class is not annotated with {@link ConfigFile}
     */
    public static void watch(@NotNull Class<?> cls, @NotNull WatcherOptions options) {
        Path file = resolveFilePath(cls);
        Path dir = file.getParent();

        boolean alreadyWatching = fileToWatchers.containsKey(file) &&
                fileToWatchers.get(file).containsKey(cls);

        fileToWatchers.computeIfAbsent(file, k -> new ConcurrentHashMap<>()).put(cls, options);

        ensureStarted();

        if (!alreadyWatching) {
            ensureDirectoryWatched(dir);
        }
    }

    /**
     * Stops watching the config file for the given class.
     * If no other classes are watching the same file, the directory watch registration is released.
     *
     * @param cls the config class to stop watching
     */
    public static void unwatch(@NotNull Class<?> cls) {
        ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
        if (fileAnn == null) {
            return;
        }

        Path file = Path.of(fileAnn.value()).toAbsolutePath().normalize();
        ConcurrentHashMap<Class<?>, WatcherOptions> watchers = fileToWatchers.get(file);

        if (watchers == null) {
            return;
        }

        watchers.remove(cls);

        if (watchers.isEmpty()) {
            fileToWatchers.remove(file);
            releaseDirectoryRef(file.getParent());
        }
    }

    /**
     * Stops watching all registered config classes and releases all watch registrations.
     * The internal poll thread and scheduler are kept alive for potential future registrations.
     * Call {@link #shutdown()} to fully release all resources.
     */
    public static void unwatchAll() {
        Set<Path> dirs = new java.util.HashSet<>();
        for (Path file : fileToWatchers.keySet()) {
            dirs.add(file.getParent());
        }
        fileToWatchers.clear();
        pendingDebounce.values().forEach(f -> f.cancel(false));
        pendingDebounce.clear();
        for (Path dir : dirs) {
            WatchKey key = dirToKey.remove(dir);
            if (key != null) {
                key.cancel();
            }
            dirRefCount.remove(dir);
        }
    }

    /**
     * Returns whether the given config class is currently being watched.
     *
     * @param cls the config class
     * @return true if the class is registered with this watcher
     */
    public static boolean isWatching(@NotNull Class<?> cls) {
        ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
        if (fileAnn == null) {
            return false;
        }
        Path file = Path.of(fileAnn.value()).toAbsolutePath().normalize();
        ConcurrentHashMap<Class<?>, WatcherOptions> watchers = fileToWatchers.get(file);
        return watchers != null && watchers.containsKey(cls);
    }

    /**
     * Shuts down the watcher, stopping the poll thread and releasing all resources.
     * After calling this, the watcher cannot be restarted.
     * Any pending debounce tasks are cancelled.
     */
    public static void shutdown() {
        running = false;
        unwatchAll();

        scheduler.shutdownNow();

        WatchService ws = watchService;
        if (ws != null) {
            try {
                ws.close();
            } catch (IOException e) {
                // ignore
            }
            watchService = null;
        }

        Thread pt = pollThread;
        if (pt != null) {
            pt.interrupt();
            pollThread = null;
        }
    }

    private static void ensureStarted() {
        if (running) {
            return;
        }

        synchronized (ConfigWatcher.class) {
            if (running) {
                return;
            }

            try {
                watchService = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create WatchService", e);
            }

            running = true;

            Thread thread = new Thread(ConfigWatcher::pollLoop, "lsyaml-watcher-poll");
            thread.setDaemon(true);
            thread.start();
            pollThread = thread;
        }
    }

    private static void ensureDirectoryWatched(@NotNull Path dir) {
        dirRefCount.computeIfAbsent(dir, k -> new AtomicInteger(0)).incrementAndGet();

        dirToKey.computeIfAbsent(dir, k -> {
            try {
                Files.createDirectories(dir);
                WatchService ws = watchService;
                if (ws == null) {
                    ws = FileSystems.getDefault().newWatchService();
                    watchService = ws;
                }
                return dir.register(ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to watch directory: " + dir, e);
            }
        });
    }

    private static void releaseDirectoryRef(@NotNull Path dir) {
        AtomicInteger count = dirRefCount.get(dir);
        if (count == null) {
            return;
        }
        if (count.decrementAndGet() <= 0) {
            dirRefCount.remove(dir);
            WatchKey key = dirToKey.remove(dir);
            if (key != null) {
                key.cancel();
            }
        }
    }

    private static void pollLoop() {
        while (running) {
            WatchService ws = watchService;
            if (ws == null) {
                break;
            }

            WatchKey key;
            try {
                key = ws.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            if (key == null) {
                continue;
            }

            Path dir = (Path) key.watchable();

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Path filename = ((WatchEvent<Path>) event).context();
                Path file = dir.resolve(filename).toAbsolutePath().normalize();

                WatchAction action = toAction(kind);
                if (action == null) {
                    continue;
                }

                if (fileToWatchers.containsKey(file)) {
                    scheduleDebounced(file, action);
                }
            }

            key.reset();
        }
    }

    private static void scheduleDebounced(@NotNull Path file, @NotNull WatchAction action) {
        ConcurrentHashMap<Class<?>, WatcherOptions> watchers = fileToWatchers.get(file);
        if (watchers == null || watchers.isEmpty()) {
            return;
        }

        long minDebounce = watchers.values().stream()
                .mapToLong(WatcherOptions::getDebounceMillis)
                .min()
                .orElse(300);

        pendingDebounce.compute(file, (k, existing) -> {
            if (existing != null) {
                existing.cancel(false);
            }
            return scheduler.schedule(() -> dispatch(file, action), minDebounce, TimeUnit.MILLISECONDS);
        });
    }

    private static void dispatch(@NotNull Path file, @NotNull WatchAction action) {
        pendingDebounce.remove(file);

        ConcurrentHashMap<Class<?>, WatcherOptions> watchers = fileToWatchers.get(file);
        if (watchers == null) {
            return;
        }

        for (Map.Entry<Class<?>, WatcherOptions> entry : watchers.entrySet()) {
            Class<?> cls = entry.getKey();
            WatcherOptions options = entry.getValue();

            applyBuiltInBehaviour(cls, file, action, options);

            FileEventListener listener = options.getListener();
            if (listener != null) {
                try {
                    listener.onEvent(file, action);
                } catch (Exception e) {
                    LSYAMLLogger.error("Exception in file event listener for "
                            + cls.getSimpleName() + ": " + file.getFileName()
                            + ": " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                }
            }
        }
    }

    private static void applyBuiltInBehaviour(
            @NotNull Class<?> cls,
            @NotNull Path file,
            @NotNull WatchAction action,
            @NotNull WatcherOptions options
    ) {
        try {
            switch (action) {
                case MODIFIED -> {
                    if (options.isReloadOnChange()) {
                        ConfigLoader.reload(cls);
                    }
                }
                case DELETED -> {
                    if (options.isRecreateOnDelete()) {
                        ConfigLoader.load(cls);
                    }
                }
                case CREATED -> {
                    if (options.isLoadOnCreate()) {
                        ConfigLoader.reload(cls);
                    }
                }
            }
        } catch (Exception e) {
            LSYAMLLogger.error("Failed to handle " + action + " event for config "
                    + cls.getSimpleName() + " (" + file.getFileName() + "): "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Nullable
    private static WatchAction toAction(@NotNull WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) return WatchAction.CREATED;
        if (kind == ENTRY_MODIFY) return WatchAction.MODIFIED;
        if (kind == ENTRY_DELETE) return WatchAction.DELETED;
        return null;
    }

    @NotNull
    private static Path resolveFilePath(@NotNull Class<?> cls) {
        ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
        if (fileAnn == null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is not annotated with @ConfigFile");
        }
        return Path.of(fileAnn.value()).toAbsolutePath().normalize();
    }
}
