package net.vansencool.lsyaml.binding.watcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration options for a {@link ConfigWatcher} registration.
 * Use {@link #builder()} to construct instances.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * WatcherOptions options = WatcherOptions.builder()
 *     .debounceMillis(500)
 *     .reloadOnChange(true)
 *     .recreateOnDelete(true)
 *     .loadOnCreate(true)
 *     .listener((file, action) -> System.out.println(file + " -> " + action))
 *     .build();
 *
 * ConfigWatcher.watch(MyConfig.class, options);
 * }</pre>
 */
@SuppressWarnings("unused")
public final class WatcherOptions {

    private final long debounceMillis;
    private final boolean reloadOnChange;
    private final boolean recreateOnDelete;
    private final boolean loadOnCreate;
    private final @Nullable FileEventListener listener;

    private WatcherOptions(@NotNull Builder builder) {
        this.debounceMillis = builder.debounceMillis;
        this.reloadOnChange = builder.reloadOnChange;
        this.recreateOnDelete = builder.recreateOnDelete;
        this.loadOnCreate = builder.loadOnCreate;
        this.listener = builder.listener;
    }

    /**
     * Returns a new builder with sensible defaults.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default options instance.
     * Equivalent to {@code WatcherOptions.builder().build()}.
     *
     * @return default options
     */
    @NotNull
    public static WatcherOptions defaults() {
        return new Builder().build();
    }

    /**
     * Returns the debounce delay in milliseconds.
     * Events within this window after the first event are collapsed into one.
     *
     * @return debounce millis
     */
    public long getDebounceMillis() {
        return debounceMillis;
    }

    /**
     * Returns whether the config class should be reloaded when its file is modified.
     *
     * @return true if reloading on change is enabled
     */
    public boolean isReloadOnChange() {
        return reloadOnChange;
    }

    /**
     * Returns whether the config file should be recreated from defaults when it is deleted.
     *
     * @return true if recreation on delete is enabled
     */
    public boolean isRecreateOnDelete() {
        return recreateOnDelete;
    }

    /**
     * Returns whether the config class should be loaded when its file is created externally.
     *
     * @return true if loading on create is enabled
     */
    public boolean isLoadOnCreate() {
        return loadOnCreate;
    }

    /**
     * Returns the custom event listener, or null if none was registered.
     *
     * @return the listener, or null
     */
    @Nullable
    public FileEventListener getListener() {
        return listener;
    }

    /**
     * Builder for {@link WatcherOptions}.
     */
    public static final class Builder {

        private long debounceMillis = 300;
        private boolean reloadOnChange = true;
        private boolean recreateOnDelete = false;
        private boolean loadOnCreate = true;
        private @Nullable FileEventListener listener = null;

        private Builder() {
        }

        /**
         * Sets the debounce delay.
         * Events that arrive within this many milliseconds of the first event are collapsed.
         * Defaults to {@code 300}.
         *
         * @param debounceMillis debounce window in milliseconds
         * @return this builder
         */
        @NotNull
        public Builder debounceMillis(long debounceMillis) {
            this.debounceMillis = debounceMillis;
            return this;
        }

        /**
         * Sets whether to reload the config when its file is modified on disk.
         * Defaults to {@code true}.
         *
         * @param reloadOnChange true to enable
         * @return this builder
         */
        @NotNull
        public Builder reloadOnChange(boolean reloadOnChange) {
            this.reloadOnChange = reloadOnChange;
            return this;
        }

        /**
         * Sets whether to recreate the config file from defaults when it is deleted.
         * Defaults to {@code false}.
         *
         * @param recreateOnDelete true to enable
         * @return this builder
         */
        @NotNull
        public Builder recreateOnDelete(boolean recreateOnDelete) {
            this.recreateOnDelete = recreateOnDelete;
            return this;
        }

        /**
         * Sets whether to load the config when its file is created externally.
         * Defaults to {@code true}.
         *
         * @param loadOnCreate true to enable
         * @return this builder
         */
        @NotNull
        public Builder loadOnCreate(boolean loadOnCreate) {
            this.loadOnCreate = loadOnCreate;
            return this;
        }

        /**
         * Registers a custom listener that is called after built-in reload/recreate logic runs.
         *
         * @param listener the listener to invoke
         * @return this builder
         */
        @NotNull
        public Builder listener(@NotNull FileEventListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Builds the {@link WatcherOptions} instance.
         *
         * @return the built options
         */
        @NotNull
        public WatcherOptions build() {
            return new WatcherOptions(this);
        }
    }
}
