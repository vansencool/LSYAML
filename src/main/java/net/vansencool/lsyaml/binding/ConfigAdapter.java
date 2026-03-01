package net.vansencool.lsyaml.binding;

import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter interface for converting custom types to and from YAML nodes.
 * Register adapters using {@link ConfigLoader#registerAdapter(Class, ConfigAdapter)}.
 *
 * <p>Example for Duration:</p>
 * <pre>{@code
 * public class DurationAdapter implements ConfigAdapter<Duration> {
 *
 *     @Override
 *     public Duration fromNode(@NotNull YamlNode node) {
 *         return Duration.ofMinutes(node.asScalar().getLong());
 *     }
 *
 *     @Override
 *     public @NotNull YamlNode toNode(@NotNull Duration value) {
 *         return new ScalarNode(String.valueOf(value.toMinutes()));
 *     }
 * }
 *
 * // Register the adapter
 * ConfigLoader.registerAdapter(Duration.class, new DurationAdapter());
 * }</pre>
 *
 * <p>For complex types that need a map structure:</p>
 * <pre>{@code
 * public class LocationAdapter implements ConfigAdapter<Location> {
 *
 *     @Override
 *     public Location fromNode(@NotNull YamlNode node) {
 *         MapNode map = node.asMap();
 *         return new Location(
 *             map.getDouble("x"),
 *             map.getDouble("y"),
 *             map.getDouble("z")
 *         );
 *     }
 *
 *     @Override
 *     public @NotNull YamlNode toNode(@NotNull Location loc) {
 *         return MapBuilder.create()
 *             .put("x", loc.x())
 *             .put("y", loc.y())
 *             .put("z", loc.z())
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type this adapter handles
 */
public interface ConfigAdapter<T> {

    /**
     * Converts a YAML node to the target type.
     *
     * @param node the node to convert
     * @return the converted value, or null if conversion fails
     */
    @Nullable
    T fromNode(@NotNull YamlNode node);

    /**
     * Converts a value to a YAML node.
     *
     * @param value the value to convert
     * @return the node representation
     */
    @NotNull
    YamlNode toNode(@NotNull T value);
}
