package net.vansencool.lsyaml.node;

/**
 * Enumeration of YAML node types.
 */
public enum NodeType {

    /**
     * A scalar value (string, number, boolean, null).
     */
    SCALAR,

    /**
     * A mapping (key-value pairs).
     */
    MAP,

    /**
     * A sequence (ordered list).
     */
    LIST,

    /**
     * An anchor reference.
     */
    ANCHOR,

    /**
     * An alias reference to an anchor.
     */
    ALIAS
}
