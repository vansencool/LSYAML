package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Options for configuring YAML parsing behavior.
 */
@SuppressWarnings("unused")
public final class ParseOptions {

    private final boolean strict;
    private final @NotNull DuplicateKeyBehavior duplicateKeyBehavior;

    private ParseOptions(boolean strict, @NotNull DuplicateKeyBehavior duplicateKeyBehavior) {
        this.strict = strict;
        this.duplicateKeyBehavior = duplicateKeyBehavior;
    }

    /**
     * Creates default parse options (strict mode enabled, duplicate keys silently overridden).
     * Each call returns a new independent instance.
     *
     * @return default options
     */
    @NotNull
    public static ParseOptions defaults() {
        return new ParseOptions(true, DuplicateKeyBehavior.SILENT);
    }

    /**
     * Creates lenient parse options (strict mode disabled, duplicate keys silently overridden).
     * Each call returns a new independent instance.
     *
     * @return lenient options
     */
    @NotNull
    public static ParseOptions lenient() {
        return new ParseOptions(false, DuplicateKeyBehavior.SILENT);
    }

    /**
     * Creates a new builder for customizing options.
     *
     * @return new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return true if strict mode is enabled (default: true)
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Returns the behavior to apply when a duplicate key is encountered during parsing.
     *
     * @return the duplicate key behavior
     */
    @NotNull
    public DuplicateKeyBehavior getDuplicateKeyBehavior() {
        return duplicateKeyBehavior;
    }

    /**
     * Determines how the parser reacts when it encounters a key that has already been
     * defined in the same mapping.
     */
    public enum DuplicateKeyBehavior {
        /** Log a warning and override the previous value with the new one. */
        WARN_AND_OVERRIDE,
        /** Log a warning and keep the first value, ignoring subsequent duplicates. */
        WARN_AND_KEEP,
        /** Silently override the previous value (default behaviour). */
        SILENT,
        /** Throw an exception on the first duplicate key encountered. */
        ERROR
    }

    /**
     * Builder for {@link ParseOptions}.
     */
    public static final class Builder {
        private boolean strict = true;
        private @NotNull DuplicateKeyBehavior duplicateKeyBehavior = DuplicateKeyBehavior.SILENT;

        private Builder() {
        }

        /**
         * Enables or disables strict mode.
         * When enabled, the parser validates YAML structure more rigorously
         * and provides detailed error messages with line/column info.
         *
         * @param strict true to enable strict mode
         * @return this builder
         */
        @NotNull
        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Sets the behavior to apply when a duplicate key is encountered.
         *
         * @param behavior the duplicate key behavior
         * @return this builder
         */
        @NotNull
        public Builder duplicateKeyBehavior(@NotNull DuplicateKeyBehavior behavior) {
            this.duplicateKeyBehavior = behavior;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return the options
         */
        @NotNull
        public ParseOptions build() {
            return new ParseOptions(strict, duplicateKeyBehavior);
        }
    }
}
