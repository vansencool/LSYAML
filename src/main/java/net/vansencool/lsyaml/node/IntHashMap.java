package net.vansencool.lsyaml.node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Chained hash map from string keys to int values with no value boxing.
 */
public final class IntHashMap {

    /**
     * The value returned when a key is absent.
     */
    public static final int ABSENT = -1;

    /**
     * A single chained entry.
     */
    private static final class Node {
        private final int hash;
        private final @NotNull String key;
        private int value;
        private @Nullable Node next;

        private Node(int hash, @NotNull String key, int value, @Nullable Node next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Node @Nullable [] table;
    private int size;
    private int threshold;

    /**
     * Creates an empty map.
     */
    public IntHashMap() {
        this.threshold = 0;
    }

    /**
     * Creates an empty map sized for an expected number of entries.
     */
    public IntHashMap(int expected) {
        this.threshold = tableSizeFor((int) (expected / 0.75f) + 1);
    }

    private static int hash(@NotNull String key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private static int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return n < 0 ? 1 : n + 1;
    }

    /**
     * Returns the value for a key, or ABSENT when the key is not present.
     */
    public int get(@NotNull String key) {
        Node[] tab = table;
        int n;
        if (tab != null && (n = tab.length) > 0) {
            int hash = hash(key);
            Node e = tab[(n - 1) & hash];
            while (e != null) {
                String k;
                if (e.hash == hash && ((k = e.key) == key || k.equals(key))) {
                    return e.value;
                }
                e = e.next;
            }
        }
        return ABSENT;
    }

    /**
     * Returns whether a key is present.
     */
    public boolean containsKey(@NotNull String key) {
        return get(key) != ABSENT;
    }

    /**
     * Associates a value with a key, replacing any existing value.
     */
    public void put(@NotNull String key, int value) {
        Node[] tab = table;
        int n;
        if (tab == null || (n = tab.length) == 0) {
            n = (tab = resize()).length;
        }
        int hash = hash(key);
        int i = (n - 1) & hash;
        Node p = tab[i];
        if (p == null) {
            tab[i] = new Node(hash, key, value, null);
        } else {
            String k;
            if (p.hash == hash && ((k = p.key) == key || k.equals(key))) {
                p.value = value;
                return;
            }
            for (; ; ) {
                Node e = p.next;
                if (e == null) {
                    p.next = new Node(hash, key, value, null);
                    break;
                }
                if (e.hash == hash && ((k = e.key) == key || k.equals(key))) {
                    e.value = value;
                    return;
                }
                p = e;
            }
        }
        if (++size > threshold) {
            resize();
        }
    }

    /**
     * Removes a key and returns its value, or ABSENT when the key is not present.
     */
    public int remove(@NotNull String key) {
        Node[] tab = table;
        int n;
        if (tab == null || (n = tab.length) == 0) {
            return ABSENT;
        }
        int hash = hash(key);
        int i = (n - 1) & hash;
        Node p = tab[i];
        Node prev = null;
        while (p != null) {
            String k;
            if (p.hash == hash && ((k = p.key) == key || k.equals(key))) {
                if (prev == null) {
                    tab[i] = p.next;
                } else {
                    prev.next = p.next;
                }
                size--;
                return p.value;
            }
            prev = p;
            p = p.next;
        }
        return ABSENT;
    }

    private Node @NotNull [] resize() {
        Node[] oldTab = table;
        int oldCap = oldTab == null ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap;
        int newThr;
        if (oldCap > 0) {
            newCap = oldCap << 1;
            newThr = oldThr << 1;
        } else if (oldThr > 0) {
            newCap = oldThr;
            newThr = (int) (newCap * 0.75f);
        } else {
            newCap = 16;
            newThr = 12;
        }
        threshold = newThr;
        Node[] newTab = new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; j++) {
                Node e = oldTab[j];
                while (e != null) {
                    Node nextNode = e.next;
                    int i = (newCap - 1) & e.hash;
                    e.next = newTab[i];
                    newTab[i] = e;
                    e = nextNode;
                }
            }
        }
        return newTab;
    }

    /**
     * Removes all entries.
     */
    public void clear() {
        Node[] tab = table;
        if (tab != null) {
            Arrays.fill(tab, null);
        }
        size = 0;
    }

    /**
     * Returns the number of entries.
     */
    public int size() {
        return size;
    }
}
