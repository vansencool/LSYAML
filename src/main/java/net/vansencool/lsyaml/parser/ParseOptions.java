package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Options for configuring YAML parsing behavior.
 */
public final class ParseOptions {

    private static final ParseOptions DEFAULT = new ParseOptions(true);
    private static final ParseOptions LENIENT = new ParseOptions(false);

    private boolean strict;

    private ParseOptions(boolean strict) {
        this.strict = strict;
    }

    /**
     * Creates default parse options (strict mode enabled).
     *
     * @return default options
     */
    @NotNull
    public static ParseOptions defaults() {
        return DEFAULT;
    }

    /**
     * Creates lenient parse options (strict mode disabled).
     *
     * @return lenient options
     */
    @NotNull
    public static ParseOptions lenient() {
        return LENIENT;
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
     * Builder for ParseOptions.
     */
    public static final class Builder {
        private boolean strict = true;

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
         * Builds the options.
         *
         * @return the options
         */
        @NotNull
        public ParseOptions build() {
            return new ParseOptions(strict);
        }
    }
}
