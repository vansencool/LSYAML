package net.vansencool.lsyaml.node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Insertion-ordered map entry store backed by an array for iteration and an int hash index for lookup.
 */
public final class EntryMap {

    private final @NotNull List<MapNode.MapEntry> order = new ArrayList<>();
    private final @NotNull IntHashMap index = new IntHashMap();

    /**
     * Returns the entry for a key, or null when absent.
     */
    public @Nullable MapNode.MapEntry get(@NotNull String key) {
        int at = index.get(key);
        return at == IntHashMap.ABSENT ? null : order.get(at);
    }

    /**
     * Returns whether a key is present.
     */
    public boolean containsKey(@NotNull String key) {
        return index.containsKey(key);
    }

    /**
     * Appends an entry, or replaces the value of an existing one with the same key.
     */
    public void put(@NotNull MapNode.MapEntry entry) {
        int at = index.get(entry.getKey());
        if (at == IntHashMap.ABSENT) {
            index.put(entry.getKey(), order.size());
            order.add(entry);
        } else {
            order.set(at, entry);
        }
    }

    /**
     * Appends an entry whose key the caller guarantees is absent, without a lookup.
     */
    public void append(@NotNull MapNode.MapEntry entry) {
        index.put(entry.getKey(), order.size());
        order.add(entry);
    }

    /**
     * Removes and returns the entry for a key, or null when absent.
     */
    public @Nullable MapNode.MapEntry remove(@NotNull String key) {
        int at = index.remove(key);
        if (at == IntHashMap.ABSENT) {
            return null;
        }
        MapNode.MapEntry removed = order.remove(at);
        for (int i = at; i < order.size(); i++) {
            index.put(order.get(i).getKey(), i);
        }
        return removed;
    }

    /**
     * Inserts an entry at a position and shifts the index.
     */
    public void insertAt(int position, @NotNull MapNode.MapEntry entry) {
        order.add(position, entry);
        for (int i = position; i < order.size(); i++) {
            index.put(order.get(i).getKey(), i);
        }
    }

    /**
     * Returns the position of a key, or minus one when absent.
     */
    public int indexOf(@NotNull String key) {
        return index.get(key);
    }

    /**
     * Rebinds an entry that changed its key while keeping its position.
     */
    public void rekey(@NotNull String oldKey, @NotNull String newKey) {
        int at = index.remove(oldKey);
        if (at != IntHashMap.ABSENT) {
            index.put(newKey, at);
        }
    }

    /**
     * Returns the ordered entries.
     */
    public @NotNull List<MapNode.MapEntry> entries() {
        return order;
    }

    /**
     * Returns the number of entries.
     */
    public int size() {
        return order.size();
    }

    /**
     * Returns whether there are no entries.
     */
    public boolean isEmpty() {
        return order.isEmpty();
    }

    /**
     * Removes all entries.
     */
    public void clear() {
        order.clear();
        index.clear();
    }
}
