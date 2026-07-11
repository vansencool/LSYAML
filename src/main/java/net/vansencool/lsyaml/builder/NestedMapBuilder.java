package net.vansencool.lsyaml.builder;

import net.vansencool.lsyaml.node.MapNode;
import org.jetbrains.annotations.NotNull;

/**
 * A nested map builder that can return to its parent builder.
 */
@SuppressWarnings("unused")
public class NestedMapBuilder extends MapBuilder {

    private final @NotNull MapBuilder parent;
    private final @NotNull String key;

    NestedMapBuilder(@NotNull MapBuilder parent, @NotNull String key) {
        super();
        this.parent = parent;
        this.key = key;
    }

    /**
     * Returns to the parent MapBuilder after adding this nested map.
     *
     * @return the parent builder
     */
    public @NotNull MapBuilder goBack() {
        parent.put(key, build());
        return parent;
    }

    /**
     * Completes the nested map and returns the root MapNode.
     * This is a convenience method that calls goBack().build().
     *
     * @return the root MapNode
     */
    public @NotNull MapNode done() {
        return goBack().build();
    }
}
