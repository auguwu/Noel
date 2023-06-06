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

import dev.floofy.noel.modules.annotations.ModulePriority;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a {@link java.util.ServiceLoader} utility for loading up {@link NoelModule modules}.
 */
public class ModuleLocator {
    private static final ServiceLoader<NoelModule> serviceLoader = ServiceLoader.load(NoelModule.class);
    private static final Logger LOG = LoggerFactory.getLogger(ModuleLocator.class);

    private ModuleLocator() {}

    /**
     * Loads and returns a list of all the modules that were gathered via Java SPI. This makes it easier
     * to locate modules through-out Noel's project source code.
     *
     * @return A list of all loaded modules.
     * @throws InvocationTargetException If the constructor threw an exception
     * @throws InstantiationException If the module class with a zero-arg constructor couldn't be instantiated.
     * @throws IllegalAccessException If the {@link ModuleLocator} doesn't have access to create the module instance.
     */
    public static List<NoelModule> loadModules()
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        LOG.info("Now initializing all Noel modules!");

        final Iterator<ServiceLoader.Provider<NoelModule>> iter =
                serviceLoader.stream().iterator();
        final List<NoelModule> modules = new ArrayList<>();

        while (iter.hasNext()) {
            final ServiceLoader.Provider<NoelModule> mod = iter.next();
            final Class<? extends NoelModule> modCls = mod.type();

            LOG.trace("found module class [{}]", modCls.getName());

            Constructor<? extends NoelModule> constructor;
            try {
                constructor = modCls.getDeclaredConstructor();
            } catch (NoSuchMethodException ignored) {
                LOG.warn(
                        "Constructor for module class [{}] didn't have a no-arg constructor! Skipping...",
                        modCls.getName());

                continue;
            }

            final NoelModule instance = constructor.newInstance();
            instance.onInit();
            modules.add(instance);
        }

        return modules.stream()
                .sorted((a, b) -> {
                    final ModulePriority aPriority = a.getClass().getAnnotation(ModulePriority.class);
                    final ModulePriority bPriority = b.getClass().getAnnotation(ModulePriority.class);

                    return (bPriority == null ? 0 : bPriority.value()) - (aPriority == null ? 0 : aPriority.value());
                })
                .toList();
    }
}
