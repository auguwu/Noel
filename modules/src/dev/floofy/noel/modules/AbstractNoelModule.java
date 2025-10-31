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

package dev.floofy.noel.modules;

import com.google.inject.AbstractModule;
import dev.floofy.noel.modules.annotations.Init;
import dev.floofy.noel.modules.annotations.Module;
import dev.floofy.noel.modules.annotations.Teardown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class AbstractNoelModule extends AbstractModule {
    /// Returns information about this module.
    @NotNull
    public Module getInfo() {
        final Module mod = getClass().getAnnotation(Module.class);
        if (mod == null) {
            throw new RuntimeException("@Module annotation is not attached to this module");
        }

        return mod;
    }

    /**
     * Returns the teardown {@link Method method}, do not overwrite this.
     */
    @Nullable
    public Method getTeardownMethod() {
        return Arrays.stream(getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Teardown.class))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the initializer {@link Method method}, do not overwrite this.
     */
    @Nullable
    public Method getInitMethod() {
        return Arrays.stream(getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Init.class))
                .findFirst()
                .orElse(null);
    }
}
