package net.vansencool.lsyaml.builder;

import net.vansencool.lsyaml.node.MapNode;
import org.jetbrains.annotations.NotNull;

/**
 * A nested list builder that can return to its parent builder.
 */
@SuppressWarnings("unused")
public class NestedListBuilder extends ListBuilder {

    private final @NotNull MapBuilder parent;
    private final @NotNull String key;

    NestedListBuilder(@NotNull MapBuilder parent, @NotNull String key) {
        super();
        this.parent = parent;
        this.key = key;
    }

    /**
     * Returns to the parent MapBuilder after adding this nested list.
     *
     * @return the parent builder
     */
    @NotNull
    public MapBuilder goBack() {
        parent.put(key, build());
        return parent;
    }

    /**
     * Completes the nested list and returns the root MapNode.
     * This is a convenience method that calls goBack().build().
     *
     * @return the root MapNode
     */
    @NotNull
    public MapNode done() {
        return goBack().build();
    }
}
