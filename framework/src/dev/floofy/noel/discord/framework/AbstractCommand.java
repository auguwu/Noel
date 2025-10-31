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

package dev.floofy.noel.discord.framework;

import dev.floofy.noel.discord.framework.annotations.Command;
import dev.floofy.noel.discord.framework.annotations.Option;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCommand {
    private ArrayList<CommandOption> options = new ArrayList<>();
    private Command info;

    public abstract void execute(CommandContext ctx) throws Exception;

    @NotNull
    public Command getInfo() {
        if (info == null) {
            final var annotation = getClass().getAnnotation(Command.class);
            if (annotation == null) {
                throw new IllegalStateException(String.format("@Command annotation is not provided in class %s", getClass()));
            }

            info = annotation;
        }

        return info;
    }

    public List<CommandOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    protected void registerOptions() {
        for (var field: getClass().getDeclaredFields()) {
            final var annotation = field.getAnnotation(Option.class);
            if (annotation == null) {
                continue;
            }

            CommandOption option;
            try {
                field.setAccessible(true);
                option = (CommandOption)field.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            Field infoField;
            try {
                infoField = option.getClass().getDeclaredField("info");
                infoField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            try {
                infoField.set(option, annotation);
                field.set(this, new CommandOption(option.tClass, annotation));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            options.add(option);
        }
    }
}
