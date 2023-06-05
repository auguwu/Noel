package dev.floofy.noel.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a configuration setting that be
 * @param <T> Setting type to use.
 */
public class Setting<T> {
    final Function<Object, T> identifySettingFn;
    final String key;

    /**
     * Creates a new {@link Setting<String>} object with the return value
     * being the raw value as a String.
     *
     * @param key The key to use. This can be in the form of dot-notation (<code>some.key.here</code>) for nested
     *            objects, it will be resolved by the Config to see if it exists.
     * @return {@link Setting<String>} to use.
     */
    public static Setting<String> string(@NotNull String key) {
        return new Setting<>(key, (value) -> {
            if (!(value instanceof String)) {
                throw new IllegalStateException("Value was not a String, received: " + value.getClass());
            }

            return (String)value;
        });
    }

    public static Setting<SecureSetting> secureSetting(@NotNull String key) {
        return new Setting<>(key, (value) -> {
            if (!(value instanceof String)) {
                throw new IllegalStateException("Value was not a String, received: " + value.getClass());
            }

            return new SecureSetting((String)value);
        });
    }

    protected Setting(
        @NotNull String key,
        @NotNull Function<Object, T> identifySettingFn
    ) {
        this.identifySettingFn = Objects.requireNonNull(identifySettingFn, "missing identify resolver for setting");
        this.key = key;
    }
}
