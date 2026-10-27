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

package dev.floofy.noel.birthdays.discord;

import com.google.inject.Inject;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.birthdays.api.Calendar;
import dev.floofy.noel.birthdays.api.Store;
import dev.floofy.noel.birthdays.models.Birthday;
import dev.floofy.noel.pinecone.annotations.*;
import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.AutocompleteContext;
import dev.floofy.noel.pinecone.api.Autocompleter;
import dev.floofy.noel.pinecone.api.CommandContext;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

@SlashCommand(name = "birthday", description = "idk yet lol")
@GuildRegistration(BirthdaysGuildScope.class)
public final class BirthdaysCommand extends AbstractSlashCommand {
    /// The earliest birth year accepted. Anything older is a typo.
    private static final int MIN_YEAR = 1900;

    @Inject
    private Settings settings;

    @Inject
    private Clock clock;

    @Inject
    private Store store;

    static final List<String> ZONES = getZones();

    @Override
    public void execute(CommandContext cx) throws Exception {}

    @Subcommand(description = "views the birthday configuration for yourself")
    public void view(CommandContext cx) {
        final var guild = cx.getGuild();
        if (guild == null) {
            cx.reply(":question: **|> this command must be ran in a guild.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (disabledForGuild(this.settings, guild.getIdLong())) {
            cx.reply(":question: **|> birthdays is not enabled for this guild! this is a bug with Pinecone...**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var birthday = this.store.find(guild.getIdLong(), cx.getUser().getIdLong());
        if (birthday.isEmpty()) {
            cx.reply(":question: **|> you didn't set a birthday for me to track! run `/birthday set`.**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var year = new StringBuilder();
        final var age = birthday.get().ageAt(Instant.now());
        if (age.isPresent()) {
            year.append(" - you are ").append(age.getAsInt()).append(" years old!");
        }

        final var md = birthday.get().day();
        cx.replyFormat(
                        ":cake: **|> birthday is set to `%d/%d`%s (next celebration: %s)**",
                        md.getMonth().getValue(),
                        md.getDayOfMonth(),
                        year,
                        TimeFormat.RELATIVE.format(birthday.get().nextAt()))
                .setEphemeral(true)
                .queue();
    }

    @Subcommand(description = "sets your birthday")
    public void set(
            CommandContext cx,
            @Option(
                            name = "month",
                            description = "month that you were born in",
                            type = OptionType.INTEGER,
                            required = true)
                    int month,
            @Option(name = "day", description = "day you were born in", type = OptionType.INTEGER, required = true)
                    int day,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    @Option(name = "year", description = "year you were born in", type = OptionType.INTEGER)
                    Optional<Integer> year,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    @Option(name = "tz", description = "timezone you live in (defaults to UTC)")
                    Optional<String> tz) {
        final var guild = cx.getGuild();
        if (guild == null) {
            cx.reply(":x: **|> birthdays can be only set in a server**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (disabledForGuild(this.settings, guild.getIdLong())) {
            cx.reply(":question: **|> birthdays is not enabled for this guild! this is a bug with Pinecone...**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final MonthDay birthday;
        try {
            birthday = MonthDay.of(month, day);
        } catch (DateTimeException ex) {
            cx.replyFormat(":x: **|> `%d/%d` isn't a date**", month, day)
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var userId = cx.getUser().getIdLong();
        final var existing = store.find(guild.getIdLong(), userId);

        final ZoneId zone;
        if (tz.isPresent()) {
            final var parsed = parseZone(tz.get());
            if (parsed.isEmpty()) {
                cx.replyFormat(
                                ":x: **|> unknown timezone: `%s` - use a timezone like `America/Los_Angeles` or"
                                        + " `Europe/Berlin`**",
                                tz.get())
                        .setEphemeral(true)
                        .queue();

                return;
            }

            zone = parsed.get();
        } else {
            zone = existing.map(Birthday::zone).orElse(ZoneOffset.UTC);
        }

        // An explicit year (0 removes it), else the one already saved.
        final Integer birthYear;
        if (year.isPresent()) {
            birthYear = year.get() == 0 ? null : year.get();
        } else {
            birthYear = existing.map(Birthday::year).orElse(null);
        }

        if (birthYear != null) {
            final var problem = checkYear(birthday, birthYear, LocalDate.now(clock.withZone(zone)));
            if (problem.isPresent()) {
                cx.replyFormat(":x: **|> %s**", problem.get())
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        // Keep the last celebrated year, so changing timezone on the day itself can't announce the birthday a second
        // time.
        final int lastCelebratedYear =
                existing.map(Birthday::lastCelebratedYear).orElse(0);

        final var nextAt = Calendar.next(birthday, zone, clock.instant(), lastCelebratedYear);
        final var saved =
                new Birthday(guild.getIdLong(), userId, birthday, birthYear, zone, nextAt, lastCelebratedYear);
        store.save(saved);

        cx.replyFormat(":cake: **|> saved: %s**", describe(saved, clock.instant()))
                .setEphemeral(true)
                .queue();
    }

    @Subcommand(description = "deletes your birthday from Noel's records")
    public void delete(CommandContext cx) {
        final var guild = cx.getGuild();
        if (guild == null) {
            cx.reply(":x: **|> birthdays can be only set in a server**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (disabledForGuild(this.settings, guild.getIdLong())) {
            cx.reply(":question: **|> birthdays is not enabled for this guild! this is a bug with Pinecone...**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var removed = store.delete(guild.getIdLong(), cx.getUser().getIdLong());
        cx.reply(
                        removed
                                ? ":wave: **|> your birthday was removed**"
                                : ":thinking: **|> you didn't have a birthday set?**")
                .setEphemeral(true)
                .queue();
    }

    @Autocomplete("tz")
    public List<Command.Choice> autocompleteTimeZone(AutocompleteContext ignoredCx, String typed) {
        final var query = typed.strip().toLowerCase(Locale.ROOT).replace(' ', '_');
        return ZONES.stream()
                .filter(id -> query.isEmpty() || id.toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparingInt((String id) -> rank(id, query)).thenComparing(id -> id))
                .limit(Autocompleter.MAX_CHOICES)
                .map(id -> new Command.Choice(label(id, Instant.now()), id))
                .toList();
    }

    boolean disabledForGuild(Settings settings, long guildId) {
        return settings.get(Setting.of("plugins.birthdays['%d']".formatted(guildId), Object.class))
                .isEmpty();
    }

    static Optional<String> checkYear(MonthDay birthday, int year, LocalDate today) {
        if (year < MIN_YEAR || year > today.getYear()) {
            return Optional.of("`%d` doesn't look like a birth year".formatted(year));
        }

        if (!birthday.isValidYear(year)) {
            return Optional.of("February 29 doesn't exist in %d".formatted(year));
        }

        if (birthday.atYear(year).isAfter(today)) {
            return Optional.of("that date hasn't happened yet");
        }

        return Optional.empty();
    }

    static String describe(Birthday birthday, Instant now) {
        final var message = new StringBuilder("**")
                .append(birthday.day().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .append(' ')
                .append(birthday.day().getDayOfMonth());

        if (birthday.year() != null) {
            message.append(", ").append(birthday.year());
        }

        message.append("**");

        final var today = LocalDate.ofInstant(now, birthday.zone());
        final var details = new StringBuilder(birthday.zone().getId());
        birthday.ageAt(now).ifPresent(age -> details.append(", age ").append(age));

        final String when;
        if (birthday.day().equals(MonthDay.from(today))
                || (!birthday.day().isValidYear(today.getYear())
                        && birthday.day().atYear(today.getYear()).equals(today))) {
            final var end = today.plusDays(1).atStartOfDay(birthday.zone()).toInstant();
            when = String.format(":tada: today! (until %s)", TimeFormat.RELATIVE.format(end));
        } else {
            when = String.format(
                    "next celebration: %s (%s)",
                    TimeFormat.DATE_TIME_LONG.format(birthday.nextAt()), TimeFormat.RELATIVE.format(birthday.nextAt()));
        }

        return String.format("%s (%s) - %s", message, details, when);
    }

    static int rank(String id, String query) {
        if (query.isEmpty()) {
            return id.equals("UTC") ? 0 : 1;
        }

        final var lower = id.toLowerCase(Locale.ROOT);
        final var city = lower.substring(lower.lastIndexOf('/') + 1);
        if (lower.equals(query) || city.equals(query)) {
            return 0;
        }

        if (city.startsWith(query)) {
            return 1;
        }

        return lower.startsWith(query) ? 2 : 3;
    }

    private static String label(String id, Instant now) {
        final var offset = ZoneId.of(id).getRules().getOffset(now);
        final var text = offset.equals(ZoneOffset.UTC) ? "UTC" : "UTC" + offset.getId();
        return id.equals("UTC") ? id : "%s (%s)".formatted(id.replace('_', ' '), text);
    }

    static List<String> getZones() {
        final var regions = List.of(
                "Africa/",
                "America/",
                "Antarctica/",
                "Asia/",
                "Atlantic/",
                "Australia/",
                "Europe/",
                "Indian/",
                "Pacific/");

        final var out = new ArrayList<String>();
        out.add("UTC");
        ZoneId.getAvailableZoneIds().stream()
                .filter(id -> regions.stream().anyMatch(id::startsWith))
                .sorted()
                .forEach(out::add);

        return List.copyOf(out);
    }

    static Optional<ZoneId> parseZone(String input) {
        final var name = input.strip();
        try {
            return Optional.of(ZoneId.of(name));
        } catch (DateTimeException ignored) {
            // fall through to a case-insensitive match
        }

        return ZoneId.getAvailableZoneIds().stream()
                .filter(id -> id.equalsIgnoreCase(name))
                .findFirst()
                .map(ZoneId::of);
    }
}
