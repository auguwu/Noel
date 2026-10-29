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

package dev.floofy.noel.utils;

import java.time.Duration;

public final class Strings {
    private static final String HUMAN_SIZE_UNITS = "KMGT";

    private Strings() {}

    public static String toHumanSize(long bytes) {
        if (bytes < 1024) {
            return String.format("%dB", bytes);
        }

        var unit = -1;
        double value = bytes;
        while (value >= 1024 && unit < HUMAN_SIZE_UNITS.length() - 1) {
            value /= 1024;
            unit++;
        }

        return String.format("%.1f %siB", value, HUMAN_SIZE_UNITS.charAt(unit));
    }

    /// Formats a duration like `1h2m3s4ms`, or `12µs 345ns` below a millisecond.
    /// @param dur the duration
    /// @return the formatted duration, such as `1m30s`, `250ms` or `0ns`
    public static String format(Duration dur) {
        if (dur.isNegative()) {
            return String.format("-%s", format(dur.negated()));
        }

        if (dur.isZero()) {
            return "0ns";
        }

        final var out = new StringBuilder();
        if (dur.toHours() > 0) {
            out.append(dur.toHours()).append('h');
        }

        if (dur.toMinutesPart() > 0) {
            out.append(dur.toMinutesPart()).append('m');
        }

        if (dur.toSecondsPart() > 0) {
            out.append(dur.toSecondsPart()).append('s');
        }

        if (dur.toMillisPart() > 0) {
            out.append(dur.toMillisPart()).append("ms");
        }

        if (out.isEmpty()) {
            final int nanos = dur.toNanosPart();
            final var us = nanos / 1_000;
            final var ns = nanos % 1_000;

            if (us > 0) {
                out.append(us).append("µs").append(' ');
            }

            if (ns > 0) {
                out.append(ns).append("ns");
            }
        }

        return out.toString();
    }
}

