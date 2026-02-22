package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the config key name for a field.
 * The value is automatically converted to lowercase.
 * Use {@link ExplicitKey} if you need exact casing.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     @Key("SERVER_NAME")
 *     public static String NAME = "MyServer";
 * }
 * }</pre>
 *
 * <p>This will map to {@code server_name: MyServer} in the YAML file.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Key {

    /**
     * The config key name to use instead of the field name.
     * Will be converted to lowercase.
     *
     * @return the key name
     */
    String value();
}
