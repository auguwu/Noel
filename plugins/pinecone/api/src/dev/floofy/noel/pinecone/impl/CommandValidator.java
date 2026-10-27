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

package dev.floofy.noel.pinecone.impl;

import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.Option;
import dev.floofy.noel.pinecone.api.Subcommand;
import java.util.*;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/// Checks a command against Discord's rules and Pinecone's own, when its plugin
/// registers.
///
/// Without this, most of these mistakes only surface when Pinecone syncs with
/// Discord, and because commands are synced in one bulk request, one bad command
/// would stop **every** command from being registered. Others never surface at
/// all, like an `@Autocomplete` method for a misspelled option.
final class CommandValidator {
    /// Discord's name rule, simplified to the scripts this bot uses: 1 to 32
    /// letters, digits, `-` or `_`, and lowercase where a letter has a case.
    private static final Pattern NAME = Pattern.compile("[-_\\p{L}\\p{N}]{1,32}");

    private static final int MAX_DESCRIPTION = 100;
    private static final int MAX_CHILDREN = 25;

    /// Option types Discord allows autocomplete on.
    private static final Set<OptionType> AUTOCOMPLETABLE =
            Set.of(OptionType.STRING, OptionType.INTEGER, OptionType.NUMBER);

    private CommandValidator() {}

    /// Validates a command and everything under it.
    ///
    /// @param command a metadata instance of the command
    /// @throws IllegalStateException describing the first problem found, starting with where it is, like `/birthday
    /// set: ...`
    static void validate(AbstractSlashCommand command) {
        final var info = command.getInfo();
        final var where = "/" + info.name();

        name(where, "command", info.name());
        description(where, info.description());

        final var options = command.getOptions();
        final var subcommands = command.getSubcommands();
        final var groups = command.getSubcommandGroups();

        if (!options.isEmpty() && !(subcommands.isEmpty() && groups.isEmpty())) {
            fail(
                    where,
                    "has both @Option fields and subcommands, which Discord doesn't allow; "
                            + "move the options onto the subcommands");
        }

        if (subcommands.size() + groups.size() > MAX_CHILDREN) {
            fail(where, "has more than %d subcommands and groups".formatted(MAX_CHILDREN));
        }

        // Every option in the command, by name, for checking @Autocomplete targets.
        final var optionTypes = new HashMap<String, OptionType>();
        options(where, options, optionTypes);

        subcommands.forEach((name, subcommand) -> subcommand(where, name, subcommand, optionTypes));

        for (var group : groups) {
            final var groupName = group.getInfo().name();
            final var groupWhere = where + " " + groupName;

            name(groupWhere, "subcommand group", groupName);
            description(groupWhere, group.getInfo().description());

            if (subcommands.containsKey(groupName)) {
                fail(groupWhere, "is both a subcommand and a subcommand group");
            }

            if (group.getSubcommands().isEmpty()) {
                fail(groupWhere, "has no @Subcommand methods");
            }

            if (group.getSubcommands().size() > MAX_CHILDREN) {
                fail(groupWhere, "has more than %d subcommands".formatted(MAX_CHILDREN));
            }

            group.getSubcommands().forEach((name, subcommand) -> subcommand(groupWhere, name, subcommand, optionTypes));
        }

        if (groups.stream().map(group -> group.getInfo().name()).distinct().count() != groups.size()) {
            fail(where, "has two subcommand groups with the same name");
        }

        for (var entry : command.getAutocompleters().entrySet()) {
            final var optionName = entry.getKey();
            final var type = optionTypes.get(optionName);
            if (type == null) {
                fail(where, "has @Autocomplete(\"%s\"), but no option is named `%s`".formatted(optionName, optionName));
            }

            if (!AUTOCOMPLETABLE.contains(type)) {
                fail(
                        where,
                        "option `%s` is %s, and Discord only allows autocomplete on STRING, INTEGER and NUMBER options"
                                .formatted(optionName, type));
            }

            final var valueType = entry.getValue().getValueType();
            if ((valueType == OptionalLong.class && type != OptionType.INTEGER)
                    || (valueType == OptionalDouble.class && type != OptionType.NUMBER)) {
                fail(
                        where,
                        "@Autocomplete(\"%s\") takes %s, which only fits a %s option, but `%s` is %s"
                                .formatted(
                                        optionName,
                                        valueType.getSimpleName(),
                                        valueType == OptionalLong.class ? "INTEGER" : "NUMBER",
                                        optionName,
                                        type));
            }
        }
    }

    private static void subcommand(
            String parent, String name, Subcommand subcommand, Map<String, OptionType> optionTypes) {
        final var where = parent + " " + name;
        name(where, "subcommand", name);
        description(where, subcommand.getInfo().description());
        options(where, subcommand.getOptions(), optionTypes);
    }

    private static void options(String where, List<Option> options, Map<String, OptionType> optionTypes) {
        if (options.size() > MAX_CHILDREN) {
            fail(where, "has more than %d options".formatted(MAX_CHILDREN));
        }

        final var seen = new HashMap<String, Boolean>();
        boolean sawOptional = false;
        for (Option option : options) {
            final var info = option.getInfo();
            final var optionWhere = where + " (option `" + info.name() + "`)";

            name(optionWhere, "option", info.name());
            description(optionWhere, info.description());

            if (seen.put(info.name(), true) != null) {
                fail(where, "has two options named `%s`".formatted(info.name()));
            }

            if (info.required() && sawOptional) {
                fail(
                        optionWhere,
                        "is required but declared after an optional option; Discord needs required options first");
            }

            if (!info.required()) {
                sawOptional = true;
                if (option.getResolvedClass().isPrimitive()) {
                    fail(
                            optionWhere,
                            "is optional but uses the primitive `%s`, which can't hold \"not given\"; use Optional<%s>"
                                    .formatted(option.getResolvedClass().getName(), boxed(option.getResolvedClass())));
                }
            }

            optionTypes.putIfAbsent(info.name(), info.type());
        }
    }

    private static void name(String where, String kind, String name) {
        if (!NAME.matcher(name).matches() || !name.equals(name.toLowerCase(Locale.ROOT))) {
            fail(
                    where,
                    "%s name `%s` is invalid; Discord needs 1 to 32 lowercase letters, digits, `-` or `_`"
                            .formatted(kind, name));
        }
    }

    private static void description(String where, String description) {
        if (description.isBlank()) {
            fail(where, "has no description; Discord requires one");
        }

        if (description.length() > MAX_DESCRIPTION) {
            fail(
                    where,
                    "description is %d characters; Discord allows at most %d"
                            .formatted(description.length(), MAX_DESCRIPTION));
        }
    }

    private static String boxed(Class<?> primitive) {
        return switch (primitive.getName()) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            default -> primitive.getName();
        };
    }

    private static void fail(String where, String problem) {
        throw new IllegalStateException(where + ": " + problem);
    }
}

// final class CommandValidator {
//
// }
