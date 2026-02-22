package net.vansencool.lsyaml.binding.watcher;

/**
 * Describes the type of file system event that triggered a config watcher callback.
 */
public enum WatchAction {

    /**
     * The watched file was created.
     */
    CREATED,

    /**
     * The watched file was modified.
     */
    MODIFIED,

    /**
     * The watched file was deleted.
     */
    DELETED
}
