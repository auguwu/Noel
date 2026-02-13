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

/// Mark a method in a command as a subcommand.
///
/// The structure of a subcommand that is valid is:
/// ```java
/// @Subcommand
/// public void nameThatDoesntMatter(
///     CommandContext context,
///     @Option("name of option", "description about it i guess???") String oneOptionHere,
/// ) {
///     /* body here */
/// }
/// ```
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    /// The name of the subcommand. By default, it'll use the method's name
    /// as the subcommand name.
    String name() default "";

    /// Description of this subcommand.
    String description() default "";
}
