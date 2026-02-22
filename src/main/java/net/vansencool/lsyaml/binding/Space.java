package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds blank lines before and/or after a field in the generated YAML for readability.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     public static String name = "Server";
 *
 *     @Space(before = 1)
 *     public static int port = 25565;
 * }
 * }</pre>
 *
 * <p>Output:</p>
 * <pre>{@code
 * name: Server
 *
 * port: 25565
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Space {

    /**
     * Number of blank lines to add before this field or section.
     *
     * @return blank lines before
     */
    int before() default 0;

    /**
     * Number of blank lines to add after this field or section.
     *
     * @return blank lines after
     */
    int after() default 0;
}
