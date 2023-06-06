/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.config;

import dev.floofy.utils.java.SetOnce;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

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
