package net.vansencool.lsyaml.metadata;

/**
 * Enumeration of collection styles in YAML.
 */
public enum CollectionStyle {

    /**
     * Block style collection.
     * Example:
     * key1: value1
     * key2: value2
     */
    BLOCK,

    /**
     * Flow style collection (JSON-like).
     * Example: {key1: value1, key2: value2}
     */
    FLOW
}
