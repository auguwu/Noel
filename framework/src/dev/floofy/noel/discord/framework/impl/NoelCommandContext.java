/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2025 Noel Towa <cutie@floofy.dev>
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

package dev.floofy.noel.discord.framework.impl;

import dev.floofy.noel.discord.framework.CommandContext;
import dev.floofy.noel.discord.framework.CommandOption;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ClassCanBeRecord")
public final class NoelCommandContext implements CommandContext {
    private final SlashCommandInteraction interaction;

    public NoelCommandContext(@NotNull SlashCommandInteraction interaction) {
        this.interaction = interaction;
    }

    @Override
    public SlashCommandInteraction getInteraction() {
        return interaction;
    }

    @Override
    public <T> @Nullable T getOption(CommandOption option) {
        final var opt = getInteraction().getOption(option.getInfo().name());
        if (opt == null) {
            return null;
        }

                Object found;
                if (option.getOptionClass() == String.class) {
                    found = opt.getAsString();
                } else if (option.getOptionClass() == long.class) {
                    found = opt.getAsLong();
                } else if (option.getOptionClass() == double.class) {
                    found = opt.getAsDouble();
                } else if (option.getOptionClass() == int.class) {
                    found = opt.getAsInt();
                } else if (option.getOptionClass() == boolean.class) {
                    found = opt.getAsBoolean();
                } else if (option.getOptionClass() == Member.class) {
                    found = opt.getAsMember();
                } else if (option.getOptionClass() == User.class) {
                    found = opt.getAsUser();
                } else if (option.getOptionClass() == IMentionable.class) {
                    found = opt.getAsMentionable();
                } else if (option.getOptionClass() == Message.Attachment.class) {
                    found = opt.getAsAttachment();
                } else if (option.getOptionClass() == Role.class) {
                    found = opt.getAsRole();
                } else {
                    throw new IllegalStateException("Unable to find mapping for class [" + option.getOptionClass() + "]");
                }

                //noinspection unchecked
                return (T)option.getOptionClass().cast(found);
    }
}
