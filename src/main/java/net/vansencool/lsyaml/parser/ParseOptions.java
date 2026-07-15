package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Options for configuring YAML parsing behavior.
 */
@SuppressWarnings("unused")
public final class ParseOptions {

    /**
     * The document size in characters from which validating alongside the parse pays for the thread handoff.
     */
    public static final int DEFAULT_PARALLEL_THRESHOLD = 100 * 1024;

    private final @Nullable YamlValidator validator;
    private final @NotNull DuplicateKeyBehavior duplicateKeyBehavior;
    private final boolean parallelValidation;
    private final int parallelThreshold;

    private ParseOptions(@Nullable YamlValidator validator, @NotNull DuplicateKeyBehavior duplicateKeyBehavior, boolean parallelValidation, int parallelThreshold) {
        this.validator = validator;
        this.duplicateKeyBehavior = duplicateKeyBehavior;
        this.parallelValidation = parallelValidation;
        this.parallelThreshold = parallelThreshold;
    }

    /**
     * Creates default parse options that validate with rich diagnostics.
     * Each call returns a new independent instance.
     *
     * @return default options
     */
    public static @NotNull ParseOptions defaults() {
        return new ParseOptions(StandardYamlValidator.newInstance(), DuplicateKeyBehavior.SILENT, false, DEFAULT_PARALLEL_THRESHOLD);
    }

    /**
     * Creates lenient parse options that skip validation.
     * Each call returns a new independent instance.
     *
     * @return lenient options
     */
    public static @NotNull ParseOptions lenient() {
        return new ParseOptions(null, DuplicateKeyBehavior.SILENT, false, DEFAULT_PARALLEL_THRESHOLD);
    }

    /**
     * Creates strict parse options validated by the given validator.
     * Each call returns a new independent instance.
     *
     * @param validator the validator to run against the document
     * @return strict options
     */
    public static @NotNull ParseOptions strict(@NotNull YamlValidator validator) {
        return new ParseOptions(validator, DuplicateKeyBehavior.SILENT, false, DEFAULT_PARALLEL_THRESHOLD);
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
     * Returns whether validation runs alongside the parse for documents that reach the parallel threshold.
     *
     * @return true if parallel validation is enabled
     */
    public boolean isParallelValidation() {
        return parallelValidation;
    }

    /**
     * Returns the document size in characters from which validation runs alongside the parse.
     *
     * @return the threshold in characters
     */
    public int getParallelThreshold() {
        return parallelThreshold;
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
         * Silently keep the first value, ignoring subsequent duplicates.
         */
        SILENT_AND_KEEP
    }

    /**
     * Builder for {@link ParseOptions}.
     */
    public static final class Builder {
        private @Nullable YamlValidator validator = RichYamlValidator.newInstance();
        private @NotNull DuplicateKeyBehavior duplicateKeyBehavior = DuplicateKeyBehavior.SILENT;
        private boolean parallelValidation;
        private int parallelThreshold = DEFAULT_PARALLEL_THRESHOLD;

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
         * Runs validation alongside the parse for documents that reach the parallel threshold.
         *
         * @param parallelValidation true to validate on another thread
         * @return this builder
         */
        public @NotNull Builder parallelValidation(boolean parallelValidation) {
            this.parallelValidation = parallelValidation;
            return this;
        }

        /**
         * Sets the document size in characters from which validation runs alongside the parse.
         *
         * @param parallelThreshold the threshold in characters
         * @return this builder
         */
        public @NotNull Builder parallelThreshold(int parallelThreshold) {
            this.parallelThreshold = parallelThreshold;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return the options
         */
        public @NotNull ParseOptions build() {
            return new ParseOptions(validator, duplicateKeyBehavior, parallelValidation, parallelThreshold);
        }
    }
}
