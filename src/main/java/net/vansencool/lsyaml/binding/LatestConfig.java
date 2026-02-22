package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the classpath resource path of the "latest" version of a config file.
 * When present, {@link ConfigLoader} will merge the user's on-disk config with this
 * latest config on every load, ensuring that new keys added to the latest config are
 * automatically propagated to the user's file while preserving their existing values.
 *
 * <p>The latest config's <b>comments and spacing</b> always take priority so that
 * documentation stays up-to-date, while the user's <b>values</b> are always preserved.</p>
 *
 * <p>Keys that exist in the user's file but are absent from the latest config are kept
 * as-is.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * @LatestConfig("defaults/config.yml")
 * public class MyConfig {
 *     public static int port = 25565;
 * }
 * }</pre>
 *
 * <p>You can also set the latest config programmatically using
 * {@link ConfigLoader#setLatestConfig(Class, String)}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LatestConfig {

    /**
     * Classpath resource path of the latest config YAML file.
     *
     * @return the resource path
     */
    String value();
}
