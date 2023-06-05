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

import dev.floofy.noel.modules.AbstractNoelModule;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNoelJDAModule extends AbstractNoelModule implements NoelJDAModule {
    private JDA jda;

    /**
     * Configures the current JDA instance that is binded to this abstract module,
     * this can be used to apply event listeners. This will also allow you to bind
     * objects together with Guice
     */
    protected void configure() {}

    @Override
    public synchronized void configure(JDA jda) {
        if (this.jda != null) {
            throw new IllegalStateException("Configuring a JDA module cannot be called more than twice.");
        }

        this.jda = jda;
        try {
            configure();
            super.configure();
        } finally {
            this.jda = null;
        }
    }

    /**
     * Returns the given {@link JDA} instance that was used to bind this module, this can
     * be only used in {@link #configure()}.
     */
    @NotNull
    public JDA getJDA() {
        if (jda == null) {
            throw new IllegalStateException("JDA can only be called in #configure()");
        }

        return jda;
    }
}
