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

package dev.floofy.noel.pinecone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a class as a subcommand group. All registered methods with the [Subcommand] annotation
/// will be inside of this subcommand group.
///
/// Subcommand groups will be only detectable in nested
// [AbstractSlashCommand][dev.floofy.noel.pinecone.AbstractSlashCommand]s.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubcommandGroup {
    /// The name of this subcommand group
    String name();

    /// A little description about this subcommand group
    String description() default "No description available";
}
