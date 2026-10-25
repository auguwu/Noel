/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel;

import dev.floofy.noel.settings.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/// A typed value at a [Key] inside a configuration map.
///
/// A setting is declared once and read from any number of configuration maps:
///
/// ```java
/// static final Setting<String> TOKEN = Setting.string("discord.token");
/// static final Setting<Integer> SHARDS = Setting.integer("discord.shards").orElse(1);
/// static final Setting<List<String>> OWNERS = Setting.list("discord.owners", Setting::string);
/// static final Setting<Map<String, Object>> EXTRA = Setting.map("extra", String.class, Object.class);
///
/// var token = TOKEN.require(config);   // throws if missing
/// var shards = SHARDS.require(config); // 1 if missing
/// ```
///
/// ## Missing versus invalid
///
/// A setting whose path doesn't resolve, or resolves to `null`, is **missing**:
/// [#get(Map)] returns empty and [#require(Map)] throws. A setting whose path
/// resolves to a value of the wrong type is **invalid**, and every accessor throws
/// [SettingException], so a typo in a config file is never silently treated as
/// "not set".
///
/// ## Nested settings
///
/// [#list(String,Function)] and [#map(String,Function)] take a factory from a
/// path to a setting, such as `Setting::string`. Each element is read as its own
/// setting at an indexed path, like `discord.owners[2]` or `limits['guild']`, so
/// elements get the same conversions and error messages that name the exact
/// element. Factories compose:
///
/// ```java
/// Setting<List<List<Integer>>> grid =
///     Setting.list("grid", row -> Setting.list(row, Setting::integer));
/// ```
///
/// Settings are immutable and thread-safe.
///
/// @param <T> the type of the value
public final class Setting<T> {
    private final Key key;
    private final Function<Map<String, ?>, Optional<T>> resolver;

    private Setting(Key key, Function<Map<String, ?>, Optional<T>> resolver) {
        this.key = key;
        this.resolver = resolver;
    }

    /// A string. Only [String] values are accepted; numbers and booleans are
    /// invalid rather than converted.
    ///
    /// @param path the setting's path
    /// @return the setting
    public static Setting<String> string(String path) {
        return scalar(path, "a string", raw -> raw instanceof String s ? s : null);
    }

    /// A boolean. Accepts [Boolean] values and the strings `true` and `false`,
    /// ignoring case.
    ///
    /// @param path the setting's path
    /// @return the setting
    public static Setting<Boolean> bool(String path) {
        return scalar(path, "a boolean", raw -> switch (raw) {
            case Boolean b -> b;
            case String s
            when s.equalsIgnoreCase("true")
                    || s.equalsIgnoreCase("yes")
                    || s.equalsIgnoreCase("enable")
                    || s.equalsIgnoreCase("enabled") -> true;

            case String s
            when s.equalsIgnoreCase("false")
                    || s.equalsIgnoreCase("no")
                    || s.equalsIgnoreCase("disable")
                    || s.equalsIgnoreCase("disabled") -> false;

            default -> null;
        });
    }

    /// A 32-bit integer. Accepts integral numbers that fit in an `int`.
    ///
    /// @param path the setting's path
    /// @return the setting
    public static Setting<Integer> integer(String path) {
        return scalar(path, "an int", raw -> {
            var value = integral(raw);
            return value != null && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE
                    ? (Integer) value.intValue()
                    : null;
        });
    }

    /// A 64-bit integer. Accepts any integral number.
    ///
    /// @param path the setting's path
    /// @return the setting
    public static Setting<Long> longValue(String path) {
        return scalar(path, "a long", Setting::integral);
    }

    /// A floating-point number. Accepts any [Number].
    ///
    /// @param path the setting's path
    /// @return the setting
    public static Setting<Double> doubleValue(String path) {
        return scalar(path, "a number", raw -> raw instanceof Number n ? n.doubleValue() : null);
    }

    /// An enum constant, matched by name, ignoring case.
    ///
    /// @param path the setting's path
    /// @param type the enum class
    /// @param <E>  the enum type
    /// @return the setting
    public static <E extends Enum<E>> Setting<E> enumValue(String path, Class<E> type) {
        var constants = type.getEnumConstants();
        var expected = "one of "
                + Arrays.stream(constants)
                        .map(c -> c.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.joining(", ", "[", "]"));

        return scalar(path, expected, raw -> {
            if (!(raw instanceof String s)) {
                return null;
            }

            for (var constant : constants) {
                if (constant.name().equalsIgnoreCase(s)) {
                    return constant;
                }
            }

            return null;
        });
    }

    /// Any value that is an instance of `type`, without conversion.
    ///
    /// @param path the setting's path
    /// @param type the expected class
    /// @param <V>  the value type
    /// @return the setting
    public static <V> Setting<V> of(String path, Class<V> type) {
        return scalar(path, "a " + type.getSimpleName(), raw -> type.isInstance(raw) ? type.cast(raw) : null);
    }

    /// A list whose elements are each read by the setting `element` builds for
    /// the element's path, such as `Setting::string`.
    ///
    /// A `null` element is invalid.
    ///
    /// @param path    the setting's path
    /// @param element builds the setting for an element, given its path
    /// @param <E>     the element type
    /// @return the setting; its value is an unmodifiable list
    public static <E> Setting<List<E>> list(String path, Function<String, Setting<E>> element) {
        var key = Key.of(path);
        return new Setting<>(
                key,
                root -> key.evaluate(root).map(raw -> {
                    if (!(raw instanceof List<?> list)) {
                        throw SettingException.invalid(key, "a list", raw);
                    }

                    var out = new ArrayList<E>(list.size());
                    for (var i = 0; i < list.size(); i++) {
                        out.add(element.apply(path + "[" + i + "]").require(root));
                    }

                    return List.copyOf(out);
                }));
    }

    /// A list whose elements are instances of `type`, without conversion.
    ///
    /// @param path the setting's path
    /// @param type the expected element class
    /// @param <E>  the element type
    /// @return the setting; its value is an unmodifiable list
    public static <E> Setting<List<E>> list(String path, Class<E> type) {
        return list(path, elementPath -> of(elementPath, type));
    }

    /// A map whose keys and values are instances of the given classes, without
    /// conversion.
    ///
    /// A `null` value is invalid.
    ///
    /// @param path      the setting's path
    /// @param keyType   the expected key class
    /// @param valueType the expected value class
    /// @param <K>       the key type
    /// @param <V>       the value type
    /// @return the setting; its value is an unmodifiable map, in the source map's
    ///         iteration order
    public static <K, V> Setting<Map<K, V>> map(String path, Class<K> keyType, Class<V> valueType) {
        var key = Key.of(path);
        return new Setting<>(
                key,
                root -> key.evaluate(root).map(raw -> {
                    if (!(raw instanceof Map<?, ?> map)) {
                        throw SettingException.invalid(key, "a map", raw);
                    }

                    var out = new LinkedHashMap<K, V>();
                    for (var entry : map.entrySet()) {
                        if (!keyType.isInstance(entry.getKey())) {
                            throw new SettingException(
                                    key,
                                    String.format(
                                            "expected keys of type %s, got %s",
                                            keyType.getSimpleName(), describe(entry.getKey())));
                        }

                        if (!valueType.isInstance(entry.getValue())) {
                            throw new SettingException(
                                    key,
                                    String.format(
                                            "expected a %s at key [%s], got %s",
                                            valueType.getSimpleName(), entry.getKey(), describe(entry.getValue())));
                        }

                        out.put(keyType.cast(entry.getKey()), valueType.cast(entry.getValue()));
                    }

                    return Collections.unmodifiableMap(out);
                }));
    }

    /// A map with string keys whose values are each read by the setting `value`
    /// builds for the entry's path, such as `Setting::integer`.
    ///
    /// @param path  the setting's path
    /// @param value builds the setting for an entry, given its path
    /// @param <V>   the value type
    /// @return the setting; its value is an unmodifiable map, in the source map's
    ///         iteration order
    public static <V> Setting<Map<String, V>> map(String path, Function<String, Setting<V>> value) {
        var key = Key.of(path);
        return new Setting<>(
                key,
                root -> key.evaluate(root).map(raw -> {
                    if (!(raw instanceof Map<?, ?> map)) {
                        throw SettingException.invalid(key, "a map", raw);
                    }

                    var out = new LinkedHashMap<String, V>();
                    for (var entryKey : map.keySet()) {
                        if (!(entryKey instanceof String name)) {
                            throw new SettingException(key, "expected string keys, got " + describe(entryKey));
                        }

                        out.put(
                                name,
                                value.apply(path + "['" + quote(name) + "']").require(root));
                    }

                    return Collections.unmodifiableMap(out);
                }));
    }

    /// Reads this setting from `root`.
    ///
    /// @param root the configuration map
    /// @return the value, or empty if the setting is missing
    /// @throws SettingException if the value is present but invalid
    public Optional<T> get(Map<String, ?> root) {
        return resolver.apply(Objects.requireNonNull(root, "root"));
    }

    /// Reads this setting from `root`, requiring it to be present.
    ///
    /// @param root the configuration map
    /// @return the value
    /// @throws SettingException if the setting is missing or invalid
    public T require(Map<String, ?> root) {
        return get(root).orElseThrow(() -> new SettingException(key, "is required but not set"));
    }

    /// Reads this setting from `root`, falling back to `fallback` if it is missing.
    ///
    /// @param root     the configuration map
    /// @param fallback the value to use if the setting is missing
    /// @return the value, or `fallback`
    /// @throws SettingException if the value is present but invalid
    public T getOrDefault(Map<String, ?> root, T fallback) {
        return get(root).orElse(fallback);
    }

    /// Returns a setting that reads as `fallback` when this one is missing, so
    /// [#get(Map)] is never empty. An invalid value still throws.
    ///
    /// @param fallback the default value
    /// @return the defaulted setting
    public Setting<T> orElse(T fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return new Setting<>(key, root -> Optional.of(get(root).orElse(fallback)));
    }

    /// Returns a setting whose value is this one's passed through `fn`, for
    /// parsing or validation.
    ///
    /// If `fn` throws, the exception is wrapped in a [SettingException] naming
    /// this setting's path, so a validation message reads in context:
    ///
    /// ```java
    /// Setting<Duration> timeout = Setting.string("http.timeout").transform(Duration::parse);
    /// Setting<Integer> port = Setting.integer("http.port").transform(p -> {
    ///     if (p < 1 || p > 65535) throw new IllegalArgumentException("port out of range: " + p);
    ///     return p;
    /// });
    /// ```
    ///
    /// @param fn  converts the value; must not return `null`
    /// @param <R> the new value type
    /// @return the transformed setting
    public <R> Setting<R> transform(Function<? super T, ? extends R> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Setting<>(
                key,
                root -> get(root).<R>map(value -> {
                    try {
                        return Objects.requireNonNull(fn.apply(value), "transform returned null");
                    } catch (SettingException ex) {
                        throw ex;
                    } catch (RuntimeException ex) {
                        throw new SettingException(key, "is invalid: " + ex.getMessage(), ex);
                    }
                }));
    }

    /// Returns the key this setting reads.
    ///
    /// @return the key
    public Key key() {
        return key;
    }

    @Override
    public String toString() {
        return "Setting[" + key.path() + "]";
    }

    private static <V> Setting<V> scalar(String path, String expected, Function<Object, V> convert) {
        var key = Key.of(path);
        return new Setting<>(
                key,
                root -> key.evaluate(root).map(raw -> {
                    var value = convert.apply(raw);
                    if (value == null) {
                        throw SettingException.invalid(key, expected, raw);
                    }

                    return value;
                }));
    }

    /// Returns the value of an integral number as a `long`, or `null` for anything
    /// else, including floating-point numbers.
    private static Long integral(Object raw) {
        return switch (raw) {
            case Long l -> l;
            case Integer i -> i.longValue();
            case Short s -> s.longValue();
            case Byte b -> b.longValue();
            default -> null;
        };
    }

    /// Escapes a map key for use inside `['...']` in a [Key] path.
    private static String quote(String name) {
        return name.replace("\\", "\\\\").replace("'", "\\'");
    }

    static String describe(Object value) {
        if (value == null) {
            return "null";
        }

        var text = String.valueOf(value);
        if (text.length() > 40) {
            text = text.substring(0, 37) + "...";
        }

        return text + " (" + value.getClass().getSimpleName() + ")";
    }
}
