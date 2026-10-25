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

/// Marks a method as the autocomplete handler for an option, which turns on
/// Discord's autocomplete for that option.
///
/// Use this when an option has more possible values than Discord's 25 static
/// choices, or when the values depend on the user. Discord calls the handler as
/// the user types and shows at most 25 of the choices it returns. The user can
/// still submit text that isn't a suggestion, so the command must validate it.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Autocomplete {
    /// The name of the option this method completes. Within one command, each
    /// option name can have only one handler, even if several subcommands use it.
    String value();
}
