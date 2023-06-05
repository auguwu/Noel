package dev.floofy.noel.config;

import dev.floofy.utils.java.SetOnce;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecureSetting {
    private static final Pattern SECURE_STRING_REGEX = Pattern.compile("[$]\\{([\\w.]+):-?([A-Za-z0-9_\\-\\.]*)?}");
    private final SetOnce<String> resolvedValue = new SetOnce<>();
    private final String rawString;

    public static SecureSetting of(@Nullable String raw) {
        return new SecureSetting(raw);
    }

    SecureSetting(@Nullable String raw) {
        this.rawString = raw;
    }

    public String resolve() {
        // fast path, already cached
        if (resolvedValue.wasSet()) {
            return resolvedValue.getValue();
        }

        // fast path, raw string is null.
        if (rawString == null) {
            resolvedValue.setValue("");
            return resolvedValue.getValue();
        }

        final Matcher matcher = SECURE_STRING_REGEX.matcher(rawString);
        if (!matcher.matches()) {
            resolvedValue.setValue(rawString);
            return resolvedValue.getValue();
        }

        final String key = matcher.group(1);
        final String defaultValue = matcher.group(2);
        final String foundValue = System.getenv(key);
        if (foundValue == null) {
            resolvedValue.setValue(defaultValue == null ? "" : defaultValue);
            return resolvedValue.getValue();
        }

        resolvedValue.setValue(foundValue);
        return resolvedValue.getValue();
    }
}
