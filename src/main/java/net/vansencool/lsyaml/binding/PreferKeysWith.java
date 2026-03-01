package net.vansencool.lsyaml.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the preferred separator for converting camelCase field names to YAML keys.
 * <p>
 * When placed on a class, all fields in that class will use the specified separator
 * to split camelCase names. When placed on a field, it overrides the class-level setting
 * for that particular field.
 * </p>
 *
 * <p>For example, with {@code @PreferKeysWith("-")}:</p>
 * <ul>
 *   <li>{@code maxPlayers} becomes {@code max-players}</li>
 *   <li>{@code HTTPServer} becomes {@code http-server}</li>
 *   <li>{@code MAX_CONNECTIONS} becomes {@code max-connections}</li>
 * </ul>
 *
 * <p>When reading YAML, key resolution tries the preferred separator first, then falls
 * back to underscore, then plain lowercase. This means a YAML file with either
 * {@code max-players} or {@code max_players} will match a field named {@code maxPlayers}.</p>
 *
 * <p>{@link Key} and {@link ExplicitKey} always take priority over this annotation.
 * If either is present on a field, this annotation is ignored for that field and no
 * fallback lookup is performed.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface PreferKeysWith {
    String value();
}
