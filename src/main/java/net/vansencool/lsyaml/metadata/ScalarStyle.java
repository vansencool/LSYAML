package net.vansencool.lsyaml.metadata;

/**
 * Enumeration of scalar quoting styles in YAML.
 */
public enum ScalarStyle {

    /**
     * Plain (unquoted) scalar.
     * Example: value
     */
    PLAIN,

    /**
     * Single-quoted scalar.
     * Example: 'value'
     */
    SINGLE_QUOTED,

    /**
     * Double-quoted scalar.
     * Example: "value"
     */
    DOUBLE_QUOTED,

    /**
     * Literal block scalar (preserves newlines).
     * Example: |
     *   line1
     *   line2
     */
    LITERAL,

    /**
     * Folded block scalar (folds newlines to spaces).
     * Example: >
     *   line1
     *   line2
     */
    FOLDED
}
