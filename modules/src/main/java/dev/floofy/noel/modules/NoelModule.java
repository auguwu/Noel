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

package dev.floofy.noel.modules;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that all modules must implement to be represented as a "module."
 */
public interface NoelModule extends Module {
    /**
     * Method to implement when this module is in scope when Noel is bootstrapping.
     */
    default void onInit() {}

    /**
     * Method to implement when this module is being disposed by the
     * shutdown thread.
     *
     * @param injector The injector to use when destroying objects that
     *                 might've been initialized from this module.
     * @throws Exception If any exception might've been thrown when destroying
     * this module.
     */
    default void dispose(@NotNull Injector injector) throws Exception {}

    /**
     * The module name.
     */
    @NotNull
    String name();
}
