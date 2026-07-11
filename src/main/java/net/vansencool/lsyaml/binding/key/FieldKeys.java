package net.vansencool.lsyaml.binding.key;

import net.vansencool.lsyaml.binding.ExplicitKey;
import net.vansencool.lsyaml.binding.Ignore;
import net.vansencool.lsyaml.binding.Key;
import net.vansencool.lsyaml.binding.PreferKeysWith;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Resolution between config fields and their YAML key names.
 */
public final class FieldKeys {

    private FieldKeys() {
    }

    /**
     * Returns the YAML node for a field from a map using key fallback logic.
     */
    public static @Nullable YamlNode resolveNode(@NotNull Field field, @NotNull MapNode map) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return map.get(explicitKeyAnn.value());
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            return map.get(keyAnn.value().toLowerCase());
        }

        String separator = preferredSeparator(field);
        if (separator != null) {
            String preferredKey = camelToSeparated(field.getName(), separator);
            YamlNode node = map.get(preferredKey);
            if (node != null) return node;

            if (!separator.equals("_")) {
                String underscoreKey = camelToSeparated(field.getName(), "_");
                node = map.get(underscoreKey);
                if (node != null) return node;
            }

            return map.get(field.getName().toLowerCase());
        }

        return map.get(field.getName().toLowerCase());
    }

    /**
     * Returns the map key that matches a field, or null when no key matches.
     */
    public static @Nullable String resolveKey(@NotNull Field field, @NotNull MapNode map) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return map.get(explicitKeyAnn.value()) != null ? explicitKeyAnn.value() : null;
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            String key = keyAnn.value().toLowerCase();
            return map.get(key) != null ? key : null;
        }

        String separator = preferredSeparator(field);
        if (separator != null) {
            String preferredKey = camelToSeparated(field.getName(), separator);
            if (map.get(preferredKey) != null) return preferredKey;

            if (!separator.equals("_")) {
                String underscoreKey = camelToSeparated(field.getName(), "_");
                if (map.get(underscoreKey) != null) return underscoreKey;
            }

            String plain = field.getName().toLowerCase();
            return map.get(plain) != null ? plain : null;
        }

        String plain = field.getName().toLowerCase();
        return map.get(plain) != null ? plain : null;
    }

    /**
     * Returns the YAML key name a field writes to.
     */
    public static @NotNull String keyForField(@NotNull Field field) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return explicitKeyAnn.value();
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            return keyAnn.value().toLowerCase();
        }

        String separator = preferredSeparator(field);
        if (separator != null) {
            return camelToSeparated(field.getName(), separator);
        }

        return field.getName().toLowerCase();
    }

    /**
     * Returns the preferred key separator for a field, or null when no {@link PreferKeysWith} applies.
     */
    private static @Nullable String preferredSeparator(@NotNull Field field) {
        PreferKeysWith fieldAnn = field.getAnnotation(PreferKeysWith.class);
        if (fieldAnn != null) return fieldAnn.value();

        Class<?> cls = field.getDeclaringClass();
        while (cls != null) {
            PreferKeysWith classAnn = cls.getAnnotation(PreferKeysWith.class);
            if (classAnn != null) return classAnn.value();
            cls = cls.getEnclosingClass();
        }

        return null;
    }

    /**
     * Returns a camelCase or UPPER_CASE name rewritten as a separated lowercase key.
     */
    public static @NotNull String camelToSeparated(@NotNull String name, @NotNull String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (c == '_') {
                sb.append(separator);
                continue;
            }

            if (Character.isUpperCase(c) && i > 0) {
                char prev = name.charAt(i - 1);
                if (prev != '_' && !Character.isUpperCase(prev)) {
                    sb.append(separator);
                } else if (Character.isUpperCase(prev)
                        && i + 1 < name.length()
                        && name.charAt(i + 1) != '_'
                        && !Character.isUpperCase(name.charAt(i + 1))) {
                    sb.append(separator);
                }
            }

            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * Returns whether a field should be skipped during binding.
     */
    public static boolean shouldIgnore(@NotNull Field field) {
        if (field.isAnnotationPresent(Ignore.class)) {
            return true;
        }
        return field.isSynthetic();
    }
}
