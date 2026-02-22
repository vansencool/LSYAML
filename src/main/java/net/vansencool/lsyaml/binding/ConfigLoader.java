package net.vansencool.lsyaml.binding;

import net.vansencool.lsyaml.LSYAML;
import net.vansencool.lsyaml.binding.watcher.ConfigWatcher;
import net.vansencool.lsyaml.binding.watcher.WatcherOptions;
import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and maps YAML config values into Java static fields.
 * If the config file does not exist, it will be generated automatically using the default field values.
 *
 * <p><b>Supports:</b></p>
 * <ul>
 *     <li>Primitive types and wrappers (String, int, long, boolean, double, etc.)</li>
 *     <li>{@code List<T>} and {@code Set<T>} where T is primitive-like or adapter-supported</li>
 *     <li>{@code Map<String, T>} for dynamic key-value configs</li>
 *     <li>Nested config classes (auto-detected as branches)</li>
 *     <li>Custom types using {@link ConfigAdapter}</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     public static String name = "Server";
 *     public static int port = 25565;
 *
 *     @Key("max_players")
 *     public static int maxPlayers = 100;
 *
 *     @Comment("Enable debug mode")
 *     public static boolean debug = false;
 *
 *     public static Database database = new Database();
 *
 *     public static class Database {
 *         public String host = "localhost";
 *         public int port = 3306;
 *
 *         public Auth auth = new Auth();
 *
 *         public static class Auth {
 *             public String user = "admin";
 *             public String password = "secret";
 *         }
 *     }
 * }
 *
 * // Load the config
 * public static void main(String[] args) {
 *     ConfigLoader.load(MyConfig.class);
 *     System.out.println(MyConfig.name);
 *     System.out.println(MyConfig.database.host);
 * }
 * }</pre>
 *
 * <p><b>Generated YAML:</b></p>
 * <pre>{@code
 * name: Server
 * port: 25565
 * max_players: 100
 * # Enable debug mode
 * debug: false
 * database:
 *   host: localhost
 *   port: 3306
 *   auth:
 *     user: admin
 *     password: secret
 * }</pre>
 */
@SuppressWarnings("unused")
public final class ConfigLoader {

    private static final @NotNull List<Class<?>> loadedClasses = new ArrayList<>();
    private static final @NotNull Map<Class<?>, MapNode> loadedNodes = new ConcurrentHashMap<>();
    private static final @NotNull Map<Class<?>, Map<Field, Object>> defaultValues = new ConcurrentHashMap<>();
    private static final @NotNull Map<Class<?>, Path> configPaths = new ConcurrentHashMap<>();
    private static volatile boolean autoWatchingEnabled = false;

    private ConfigLoader() {
    }

    /**
     * Enables automatic watching of config files.
     * When enabled, all subsequently loaded configs will be automatically watched for file changes.
     * Use this once at startup, before loading configs.
     */
    public static void enableAutoWatching() {
        autoWatchingEnabled = true;
    }

    /**
     * Disables automatic watching of config files.
     * Running watches are not affected by this call.
     */
    public static void disableAutoWatching() {
        autoWatchingEnabled = false;
    }

    /**
     * Returns whether automatic watching is currently enabled.
     *
     * @return true if auto-watching is enabled
     */
    public static boolean isAutoWatchingEnabled() {
        return autoWatchingEnabled;
    }

    /**
     * Registers a custom adapter for a type.
     * Adapters convert custom types to/from YAML nodes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * ConfigLoader.registerAdapter(Duration.class, new ConfigAdapter<Duration>() {
     *     @Override
     *     public Duration fromNode(Node node) {
     *         return Duration.ofMinutes(node.getLong());
     *     }
     *
     *     @Override
     *     public Node toNode(Duration value) {
     *         return new ScalarNode(value.toMinutes());
     *     }
     * });
     * }</pre>
     *
     * @param type    the type class
     * @param adapter the adapter
     * @param <T>     the type
     */
    public static <T> void registerAdapter(@NotNull Class<T> type, @NotNull ConfigAdapter<T> adapter) {
        TypeConverters.registerAdapter(type, adapter);
    }

    /**
     * Loads a configuration class from its file.
     * If the file does not exist, it will be generated using the default field values.
     *
     * @param cls class annotated with {@link ConfigFile}
     * @throws IllegalArgumentException if the class is not annotated with @ConfigFile
     */
    public static void load(@NotNull Class<?> cls) {
        ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
        if (fileAnn == null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is not annotated with @ConfigFile");
        }

        saveDefaults(cls);

        Path file = Path.of(fileAnn.value());
        configPaths.put(cls, file);

        if (!Files.exists(file)) {
            MapNode built = buildFromDefaults(cls);
            LSYAML.writeToFile(built, file);
            loadedNodes.put(cls, built);
        }

        apply(cls);

        if (!loadedClasses.contains(cls)) {
            loadedClasses.add(cls);
        }

        if (autoWatchingEnabled) {
            ConfigWatcher.watch(cls, WatcherOptions.defaults());
        }
    }

    /**
     * Reloads all previously loaded configuration classes from disk.
     * This resets all fields to their current file values while keeping defaults for missing keys.
     */
    public static void reload() {
        for (Class<?> c : loadedClasses) {
            apply(c);
        }
    }

    /**
     * Reloads a specific configuration class from disk.
     *
     * @param cls the config class to reload
     */
    public static void reload(@NotNull Class<?> cls) {
        if (!loadedClasses.contains(cls)) {
            load(cls);
        } else {
            apply(cls);
        }
    }

    /**
     * Saves the current field values to the config file.
     * This writes all current values from static fields to disk.
     *
     * @param cls the config class to save
     */
    public static void save(@NotNull Class<?> cls) {
        Path file = configPaths.get(cls);
        if (file == null) {
            ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
            if (fileAnn == null) {
                throw new IllegalArgumentException("Class " + cls.getName() + " is not annotated with @ConfigFile");
            }
            file = Path.of(fileAnn.value());
        }

        MapNode node = buildFromFields(cls);
        LSYAML.writeToFile(node, file);
        loadedNodes.put(cls, node);
    }

    /**
     * Returns the parsed root node of a configuration class.
     * The class must have been loaded using {@link #load(Class)} first.
     *
     * @param cls config class
     * @return root {@link MapNode} for this config, or null if not loaded
     */
    @Nullable
    public static MapNode node(@NotNull Class<?> cls) {
        return loadedNodes.get(cls);
    }

    /**
     * Returns the file path for a loaded config class.
     *
     * @param cls the config class
     * @return the config file path, or null if not loaded
     */
    @Nullable
    public static Path path(@NotNull Class<?> cls) {
        return configPaths.get(cls);
    }

    /**
     * Checks if a config class has been loaded.
     *
     * @param cls the config class
     * @return true if loaded
     */
    public static boolean isLoaded(@NotNull Class<?> cls) {
        return loadedClasses.contains(cls);
    }

    /**
     * Unloads a config class, removing it from tracked configs.
     * This does not delete the file.
     *
     * @param cls the config class to unload
     */
    public static void unload(@NotNull Class<?> cls) {
        loadedClasses.remove(cls);
        loadedNodes.remove(cls);
        defaultValues.remove(cls);
        configPaths.remove(cls);
        if (autoWatchingEnabled) {
            ConfigWatcher.unwatch(cls);
        }
    }

    /**
     * Unloads all config classes.
     */
    public static void unloadAll() {
        loadedClasses.clear();
        loadedNodes.clear();
        defaultValues.clear();
        configPaths.clear();
        if (autoWatchingEnabled) {
            ConfigWatcher.unwatchAll();
        }
    }

    /**
     * Resets a config class to its default values (the values fields had when first loaded).
     * Saves the defaults to the file.
     *
     * @param cls the config class to reset
     */
    public static void resetToDefaults(@NotNull Class<?> cls) {
        Map<Field, Object> defaults = defaultValues.get(cls);
        if (defaults == null) {
            return;
        }

        for (Map.Entry<Field, Object> entry : defaults.entrySet()) {
            Field field = entry.getKey();
            Object value = entry.getValue();
            try {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) {
                    field.set(null, value);
                }
            } catch (IllegalAccessException e) {
                // Skip
            }
        }

        save(cls);
    }

    private static void saveDefaults(@NotNull Class<?> cls) {
        Map<Field, Object> defaults = new HashMap<>();

        for (Field field : cls.getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(null);
                defaults.put(field, cloneValue(value));
            } catch (IllegalAccessException e) {
                // Skip
            }
        }

        defaultValues.put(cls, defaults);
    }

    @Nullable
    private static Object cloneValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (TypeConverters.isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Set<?> set) {
            return new LinkedHashSet<>(set);
        }
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        return value;
    }

    @NotNull
    private static MapNode buildFromDefaults(@NotNull Class<?> cls) {
        return buildFromFields(cls);
    }

    @NotNull
    private static MapNode buildFromFields(@NotNull Class<?> cls) {
        MapBuilder builder = MapBuilder.create();

        Space classSpace = cls.getAnnotation(Space.class);
        if (classSpace != null && classSpace.before() > 0) {
            builder.emptyLines(classSpace.before());
        }

        List<Field> visibleFields = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (!shouldSkipField(field)) {
                visibleFields.add(field);
            }
        }

        int pendingAfter = 0;
        for (Field field : visibleFields) {
            field.setAccessible(true);
            String key = TypeConverters.getKeyForField(field);

            try {
                Object value = field.get(null);
                YamlNode node = TypeConverters.toNode(value, field);

                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) {
                    for (String line : comment.value()) {
                        node.addCommentBefore(" " + line);
                    }
                }

                Space fieldSpace = field.getAnnotation(Space.class);
                int before = (fieldSpace != null ? fieldSpace.before() : 0);
                int after = (fieldSpace != null ? fieldSpace.after() : 0);

                node.setEmptyLinesBefore(before + pendingAfter);
                pendingAfter = after;

                builder.put(key, node);
            } catch (IllegalAccessException e) {
                // Skip
            }
        }

        return builder.build();
    }

    private static void apply(@NotNull Class<?> cls) {
        Path file = configPaths.get(cls);
        if (file == null) {
            ConfigFile fileAnn = cls.getAnnotation(ConfigFile.class);
            if (fileAnn == null) {
                return;
            }
            file = Path.of(fileAnn.value());
        }

        if (!Files.exists(file)) {
            return;
        }

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config file: " + file, e);
        }

        YamlNode rootNode = LSYAML.parse(content);
        if (!(rootNode instanceof MapNode mapNode)) {
            return;
        }

        loadedNodes.put(cls, mapNode);
        applyNode(cls, mapNode);
    }

    private static void applyNode(@NotNull Class<?> cls, @NotNull MapNode node) {
        for (Field field : cls.getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }

            field.setAccessible(true);
            String key = TypeConverters.getKeyForField(field);
            YamlNode childNode = node.get(key);

            if (childNode != null) {
                Object value = TypeConverters.fromNode(childNode, field);
                if (value != null) {
                    try {
                        field.set(null, value);
                    } catch (IllegalAccessException e) {
                        // Skip
                    }
                }
            }
        }
    }

    private static boolean shouldSkipField(@NotNull Field field) {
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers)) {
            return true;
        }
        if (Modifier.isFinal(modifiers)) {
            return true;
        }
        if (Modifier.isTransient(modifiers)) {
            return true;
        }
        if (field.isAnnotationPresent(Ignore.class)) {
            return true;
        }
        return field.isSynthetic();
    }
}
