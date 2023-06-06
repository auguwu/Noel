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

package dev.floofy.noel.bot.commands.general;

import dev.floofy.noel.discord.commands.AbstractCommand;
import dev.floofy.noel.discord.commands.CommandContext;
import dev.floofy.noel.discord.commands.annotations.Command;
import org.jetbrains.annotations.NotNull;

@Command(name = "help", description = "Shows information about all of Noel's commands")
public class HelpCommand extends AbstractCommand {
    @Override
    public void execute(@NotNull CommandContext context) {}
}
