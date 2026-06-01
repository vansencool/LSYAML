package net.vansencool.lsyaml.binding.adapter;

import net.vansencool.lsyaml.binding.ConfigAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of custom type adapters keyed by their target class.
 */
public final class AdapterRegistry {

    private static final @NotNull Map<Class<?>, ConfigAdapter<?>> adapters = new ConcurrentHashMap<>();

    private AdapterRegistry() {
    }

    /**
     * Registers a custom adapter for a type.
     */
    public static <T> void register(@NotNull Class<T> type, @NotNull ConfigAdapter<T> adapter) {
        adapters.put(type, adapter);
    }

    /**
     * Returns the adapter for a type, or null if none is registered.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> ConfigAdapter<T> adapter(@NotNull Class<T> type) {
        return (ConfigAdapter<T>) adapters.get(type);
    }

    /**
     * Returns whether a type has a registered adapter.
     */
    public static boolean has(@NotNull Class<?> type) {
        return adapters.containsKey(type);
    }

    /**
     * Removes all registered adapters.
     */
    public static void clear() {
        adapters.clear();
    }
}
