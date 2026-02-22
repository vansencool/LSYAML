package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a comment above the field in the generated YAML config.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     @Comment("The port the server listens on")
 *     public static int port = 25565;
 * }
 * }</pre>
 *
 * <p>Output:</p>
 * <pre>{@code
 * # The port the server listens on
 * port: 25565
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {

    /**
     * The comment text. For multi-line comments, use {@code \n} or an array.
     *
     * @return the comment lines
     */
    String[] value();
}
