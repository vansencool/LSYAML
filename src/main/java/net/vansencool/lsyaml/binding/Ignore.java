package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be ignored by the config loader.
 * The field will not be saved to or loaded from the config file.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     public static String name = "Server";
 *
 *     @Ignore
 *     public static String cachedValue = null;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignore {
}
