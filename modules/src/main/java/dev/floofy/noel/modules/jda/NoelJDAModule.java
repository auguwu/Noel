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

package dev.floofy.noel.modules.jda;

import com.google.inject.Injector;
import dev.floofy.noel.modules.NoelModule;
import net.dv8tion.jda.api.JDA;

/**
 * Represents a {@link NoelModule} that can bind event listeners and modify the current {@link net.dv8tion.jda.api.JDA}
 * instance.
 */
public interface NoelJDAModule extends NoelModule {
    /**
     * Configures the given {@link JDA} instance that modules might
     * need to configure (i.e event listeners).
     *
     * @param jda JDA instance to configure.
     */
    void configure(JDA jda, Injector injector);
}
