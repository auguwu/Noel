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

package dev.floofy.noel.pinecone.commands;

import com.google.inject.Inject;
import dev.floofy.Noel;
import dev.floofy.noel.pinecone.annotations.*;
import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.AutocompleteContext;
import dev.floofy.noel.pinecone.api.CommandContext;
import dev.floofy.noel.pinecone.interceptors.AdminOnly;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.PluginException;
import dev.floofy.noel.plugins.PluginManager;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

@SlashCommand(name = "plugins", description = "plugin management")
@Intercept(AdminOnly.class)
public final class Plugins extends AbstractSlashCommand {
    @Inject
    private PluginManager plugins;

    @Inject
    private Descriptor self;

    @Override
    public void execute(CommandContext cx) throws Exception {}

    @Subcommand(description = "lists all loaded and enabled plugins")
    public void list(CommandContext cx) {
        final var loadedPlugins = plugins.getLoadedPlugins();
        final var enabledPlugins = plugins.getEnabledPlugins();
        final var container = Container.of(
                TextDisplay.ofFormat(
                        "## Plugins [enabled %d / loaded %d]", enabledPlugins.size(), loadedPlugins.size()),
                loadedPlugins.stream()
                        .map(plugin -> {
                            var enabled = enabledPlugins.contains(plugin);
                            return TextDisplay.ofFormat(
                                    "~> %s **%s**: **%s**", enabled ? "✅" : "❌", plugin.id(), plugin.description());
                        })
                        .toArray(ContainerChildComponent[]::new));

        cx.reply(new MessageCreateBuilder()
                        .useComponentsV2()
                        .addComponents(container.withAccentColor(Noel.EMBED_COLOUR))
                        .build())
                .setEphemeral(true)
                .queue();
    }

    @Subcommand(description = "enables a plugin, and any of its dependencies that are off")
    public void enable(CommandContext cx, @Option(name = "id", description = "plugin id", required = true) String id) {
        deferReply(cx, () -> String.format("✅ **Enabled Plugins**:\n%s", render(plugins.enable(id))));
    }

    @Subcommand(description = "disables a plugin, and every plugin that depends on it")
    public void disable(CommandContext cx, @Option(name = "id", description = "plugin id", required = true) String id) {
        deferReply(
                cx,
                () -> String.format(
                        "✅ **Disabled Plugins (and its dependents)**:\n%s",
                        render(plugins.disable(id, Set.of(self.id())))));
    }

    @Autocomplete("id")
    public List<Command.Choice> completeId(AutocompleteContext cx, String typed) {
        final var enabling = "enable".equals(cx.getInteraction().getSubcommandName());
        final var enabled = Set.copyOf(plugins.getEnabledPlugins());
        final var query = typed.strip().toLowerCase(Locale.ROOT);

        return plugins.getLoadedPlugins().stream()
                .filter(desc -> enabled.contains(desc) != enabling)
                .filter(desc -> desc.id().toLowerCase(Locale.ROOT).contains(query))
                .limit(25)
                .map(desc -> new Command.Choice(desc.id(), desc.id()))
                .toList();
    }

    void deferReply(CommandContext cx, Supplier<String> action) {
        // Each action can take more than 3 seconds if it needs to, and we also need
        // to re-sync slash commands as well.
        cx.deferReply(true).queue();

        final var hook = cx.getInteraction().getHook();
        final String message;
        try {
            message = action.get();
        } catch (PluginException ex) {
            hook.sendMessageFormat(":x: **|> %s**", ex.getMessage()).queue();
            return;
        }

        hook.sendMessage(":timer: **|> Re-syncing slash commands! this may take a few seconds to a few minutes~**")
                .setEphemeral(true)
                .queue();
        cx.getPinecone().resync().whenComplete((_, error) -> {
            if (error != null) {
                hook.editOriginalFormat(
                                "%s\n:warning: **|> syncing slash commands for plugin to Discord had failed. :(**",
                                message)
                        .queue();
            } else {
                hook.editOriginal(message).queue();
            }
        });
    }

    String render(List<Descriptor> plugins) {
        return plugins.stream().map(desc -> String.format("* `%s`", desc.id())).collect(Collectors.joining("\n"));
    }
}
