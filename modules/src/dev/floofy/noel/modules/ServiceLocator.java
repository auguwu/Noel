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

package dev.floofy.noel.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A {@link ServiceLoader} for locating and initializing {@link AbstractNoelModule Noel modules}.
 */
public final class ServiceLocator {
    private static final ServiceLoader<AbstractNoelModule> locator =
            ServiceLoader.load(AbstractNoelModule.class);
    private static final Logger LOG = LoggerFactory.getLogger(ServiceLocator.class);

    public static List<AbstractNoelModule> loadModules() {
        LOG.info("initializing all modules...");

        final var iterator = locator.stream().iterator();
        final ArrayList<AbstractNoelModule> modules = new ArrayList<>();

        while (iterator.hasNext()) {
            final var mod = iterator.next();
            final Class<? extends AbstractNoelModule> moduleClass = mod.type();

            LOG.trace("Found module: {}", moduleClass.getName());

            Constructor<? extends AbstractNoelModule> constructor;
            try {
                constructor = moduleClass.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                LOG.warn(
                        "not initializing module {}: no constructor without arguments is present",
                        moduleClass.getName());
                continue;
            }

            AbstractNoelModule instance = null;
            try {
                instance = constructor.newInstance();
            } catch (InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                LOG.error(
                        "(fatal) received exception when trying to construct module {}",
                        moduleClass.getName(),
                        e);
                System.exit(128);
            }

            assert instance != null;
            modules.add(instance);
        }

        LOG.info("Found {} modules", modules.size());
        return modules.stream()
                .sorted(
                        (a, b) -> {
                            final int ap = a.getInfo().priority();
                            final int bp = b.getInfo().priority();

                            return bp - ap;
                        })
                .toList();
    }
}
