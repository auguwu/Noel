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

package dev.floofy.noel.pinecone;

import java.util.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/// Read-only information about a registered slash command.
///
/// This is what [dev.floofy.noel.Pinecone] hands out instead of command
/// instances, so nothing outside the framework can execute a command or see
/// another invocation's options. Values are immutable snapshots; [#guilds()]
/// reflects the configuration as of the last sync with Discord.
///
/// @param name        the command's name, without the `/`
/// @param description the command's description
/// @param pluginId    the id of the plugin that registered it
/// @param options     the command's own options; empty if it has subcommands
/// @param subcommands its subcommands, by name, sorted
/// @param groups      its subcommand groups, by name, sorted
/// @param guilds      the guilds it is registered in; empty if it is global
public record CommandInfo(
        String name,
        String description,
        String pluginId,
        List<OptionInfo> options,
        Map<String, SubcommandInfo> subcommands,
        Map<String, GroupInfo> groups,
        List<Long> guilds) {
    /// An option of a command or subcommand.
    ///
    /// @param name        the option's name
    /// @param description the option's description
    /// @param type        the option's Discord type
    /// @param required    whether the option must be given
    public record OptionInfo(String name, String description, OptionType type, boolean required) {}

    /// A subcommand, either directly under a command or in a group.
    ///
    /// @param name        the subcommand's name
    /// @param description the subcommand's description
    /// @param options     the subcommand's options
    public record SubcommandInfo(String name, String description, List<OptionInfo> options) {}

    /// A subcommand group.
    ///
    /// @param name        the group's name
    /// @param description the group's description
    /// @param subcommands its subcommands, by name, sorted
    public record GroupInfo(String name, String description, Map<String, SubcommandInfo> subcommands) {}

    /// Copies every collection, so a `CommandInfo` never changes after it is
    /// created, and sorts subcommands and groups by name.
    public CommandInfo {
        options = List.copyOf(options);
        subcommands = sorted(subcommands);
        groups = sorted(groups);
        guilds = List.copyOf(guilds);
    }

    /// Returns whether this command is registered globally.
    /// @return `true` if it is available in every guild and in DMs
    public boolean isGlobal() {
        return guilds.isEmpty();
    }

    /// Returns whether this command can be used in a guild.
    /// @param guildId the guild's ID
    /// @return `true` if it is global or registered in that guild
    public boolean isAvailableIn(long guildId) {
        return isGlobal() || guilds.contains(guildId);
    }

    /// Builds the information for a registered command.
    /// @param command  a metadata instance of the command; never executed
    /// @param pluginId the owning plugin's id
    /// @param guilds   the guilds it is registered in; empty if global
    public static CommandInfo from(AbstractSlashCommand command, String pluginId, long[] guilds) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(guilds, "guilds");

        final var info = command.getInfo();
        final var subcommands = new LinkedHashMap<String, SubcommandInfo>();
        command.getSubcommands().forEach((name, sub) -> subcommands.put(name, subcommand(name, sub)));

        final var groups = new LinkedHashMap<String, GroupInfo>();
        for (var group : command.getSubcommandGroups()) {
            final var groupSubcommands = new LinkedHashMap<String, SubcommandInfo>();
            group.getSubcommands().forEach((name, sub) -> groupSubcommands.put(name, subcommand(name, sub)));

            final var groupInfo = group.getInfo();
            groups.put(
                    groupInfo.name(),
                    new GroupInfo(groupInfo.name(), groupInfo.description(), sorted(groupSubcommands)));
        }

        return new CommandInfo(
                info.name(),
                info.description(),
                pluginId,
                options(command.getOptions()),
                subcommands,
                groups,
                Arrays.stream(guilds).boxed().toList());
    }

    private static SubcommandInfo subcommand(String name, Subcommand sub) {
        return new SubcommandInfo(name, sub.getInfo().description(), options(sub.getOptions()));
    }

    private static List<OptionInfo> options(List<Option> options) {
        return options.stream()
                .map(Option::getInfo)
                .map(opt -> new OptionInfo(opt.name(), opt.description(), opt.type(), opt.required()))
                .toList();
    }

    private static <V> Map<String, V> sorted(Map<String, V> map) {
        return Collections.unmodifiableMap(new TreeMap<>(map));
    }
}
