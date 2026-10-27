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

package dev.floofy.noel.birthdays.models;

import java.time.*;
import java.util.Date;
import java.util.OptionalInt;
import org.bson.Document;

/// A member's birthday in one guild.
/// @param guild              the guild it is announced in
/// @param user               the member
/// @param day                the birthday
/// @param zone               the member's timezone; `UTC` unless they set one
/// @param nextAt             when the next uncelebrated birthday day starts
/// @param lastCelebratedYear the last year it was celebrated, or `0` if never
public record Birthday(
        long guild, long user, MonthDay day, Integer year, ZoneId zone, Instant nextAt, int lastCelebratedYear) {
    public static String id(long guild, long user) {
        return String.format("%d:%d", guild, user).intern();
    }

    /// The age they turn on the birthday celebrated in `celebratedYear`.
    /// @param celebratedYear the local year of the birthday being celebrated
    /// @return the age, or empty if they didn't share their birth year
    public OptionalInt ageIn(int celebratedYear) {
        return year == null ? OptionalInt.empty() : OptionalInt.of(celebratedYear - year);
    }

    /// Their age today in their own timezone.
    /// @param now the current instant
    /// @return the age, or empty if they didn't share their birth year
    public OptionalInt ageAt(Instant now) {
        if (year == null) {
            return OptionalInt.empty();
        }

        final var today = LocalDate.ofInstant(now, zone);
        return OptionalInt.of(Period.between(day.atYear(year), today).getYears());
    }

    public Document toDocument() {
        return new Document("_id", id(guild, user))
                .append("guild", guild)
                .append("user", user)
                .append("month", day.getMonthValue())
                .append("day", day.getDayOfMonth())
                .append("year", year)
                .append("zone", zone.getId())
                .append("next_at", Date.from(nextAt))
                .append("last_celebrated_year", lastCelebratedYear);
    }

    public static Birthday fromDocument(Document doc) {
        return new Birthday(
                doc.getLong("guild"),
                doc.getLong("user"),
                MonthDay.of(doc.getInteger("month"), doc.getInteger("day")),
                doc.getInteger("year"),
                ZoneId.of(doc.getString("zone")),
                doc.getDate("next_at").toInstant(),
                doc.getInteger("last_celebrated_year", 0));
    }
}
