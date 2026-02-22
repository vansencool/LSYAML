package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a configuration class bound to a YAML file.
 * All public static fields in the class will be automatically mapped to config keys.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ConfigFile("config.yml")
 * public class MyConfig {
 *     public static String name = "Server";
 *     public static int port = 25565;
 * }
 *
 * // Load the config
 * ConfigLoader.load(MyConfig.class);
 * System.out.println(MyConfig.name);
 * }</pre>
 *
 * <p>Field names are used as config keys by default. Use {@link Key} to override.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigFile {

    /**
     * Path to the YAML config file.
     *
     * @return the file path
     */
    String value();
}
