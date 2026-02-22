package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies an exact config key name without any case conversion.
 * Unlike {@link Key}, this annotation preserves the exact casing you specify.
 *
 * <p>By default, field names and {@link Key} values are converted to lowercase.
 * Use this annotation when you need exact control over the key name.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     @ExplicitKey("serverName")
 *     public static String NAME = "MyServer";
 *
 *     @ExplicitKey("API_KEY")
 *     public static String API_KEY = "secret";
 * }
 * }</pre>
 *
 * <p>This will map to exactly {@code serverName: MyServer} and {@code API_KEY: secret}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExplicitKey {

    /**
     * The exact config key name (no case conversion applied).
     *
     * @return the exact key name
     */
    String value();
}
