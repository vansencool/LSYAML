package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Options for configuring YAML parsing behavior.
 */
@SuppressWarnings("unused")
public final class ParseOptions {

    private final @Nullable YamlValidator validator;
    private final @NotNull DuplicateKeyBehavior duplicateKeyBehavior;

    private ParseOptions(@Nullable YamlValidator validator, @NotNull DuplicateKeyBehavior duplicateKeyBehavior) {
        this.validator = validator;
        this.duplicateKeyBehavior = duplicateKeyBehavior;
    }

    /**
     * Creates default parse options that validate with rich diagnostics.
     * Each call returns a new independent instance.
     *
     * @return default options
     */
    public static @NotNull ParseOptions defaults() {
        return new ParseOptions(RichYamlValidator.newInstance(), DuplicateKeyBehavior.SILENT);
    }

    /**
     * Creates lenient parse options that skip validation.
     * Each call returns a new independent instance.
     *
     * @return lenient options
     */
    public static @NotNull ParseOptions lenient() {
        return new ParseOptions(null, DuplicateKeyBehavior.SILENT);
    }

    /**
     * Creates strict parse options validated by the given validator.
     * Each call returns a new independent instance.
     *
     * @param validator the validator to run against the document
     * @return strict options
     */
    public static @NotNull ParseOptions strict(@NotNull YamlValidator validator) {
        return new ParseOptions(validator, DuplicateKeyBehavior.SILENT);
    }

    /**
     * Creates a new builder for customizing options.
     *
     * @return new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Returns the validator to run, or null when parsing is lenient.
     *
     * @return the validator, or null
     */
    public @Nullable YamlValidator getValidator() {
        return validator;
    }

    /**
     * @return true if a validator is configured
     */
    public boolean isStrict() {
        return validator != null;
    }

    /**
     * Returns the behavior to apply when a duplicate key is encountered during parsing.
     *
     * @return the duplicate key behavior
     */
    public @NotNull DuplicateKeyBehavior getDuplicateKeyBehavior() {
        return duplicateKeyBehavior;
    }

    /**
     * Determines how the parser reacts when it encounters a key that has already been
     * defined in the same mapping.
     */
    public enum DuplicateKeyBehavior {
        /**
         * Log a warning and override the previous value with the new one.
         */
        WARN_AND_OVERRIDE,
        /**
         * Log a warning and keep the first value, ignoring subsequent duplicates.
         */
        WARN_AND_KEEP,
        /**
         * Silently override the previous value (default behaviour).
         */
        SILENT,
        /**
         * Throw an exception on the first duplicate key encountered.
         */
        ERROR
    }

    /**
     * Builder for {@link ParseOptions}.
     */
    public static final class Builder {
        private @Nullable YamlValidator validator = RichYamlValidator.newInstance();
        private @NotNull DuplicateKeyBehavior duplicateKeyBehavior = DuplicateKeyBehavior.SILENT;

        private Builder() {
        }

        /**
         * Enables rich validation or disables validation entirely.
         *
         * @param strict true to validate with rich diagnostics, false to skip validation
         * @return this builder
         */
        public @NotNull Builder strict(boolean strict) {
            this.validator = strict ? RichYamlValidator.newInstance() : null;
            return this;
        }

        /**
         * Sets the validator to run, or null to skip validation.
         *
         * @param validator the validator, or null
         * @return this builder
         */
        public @NotNull Builder validator(@Nullable YamlValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Sets the behavior to apply when a duplicate key is encountered.
         *
         * @param behavior the duplicate key behavior
         * @return this builder
         */
        public @NotNull Builder duplicateKeyBehavior(@NotNull DuplicateKeyBehavior behavior) {
            this.duplicateKeyBehavior = behavior;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return the options
         */
        public @NotNull ParseOptions build() {
            return new ParseOptions(validator, duplicateKeyBehavior);
        }
    }
}
