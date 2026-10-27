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

package dev.floofy.noel.pinecone.api;

import dev.floofy.noel.Pinecone;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

/// Everything an [`@Autocomplete`][dev.floofy.noel.pinecone.annotations.Autocomplete]
/// handler needs while answering one autocomplete request.
///
/// Discord sends a request each time the user types into an option with
/// autocomplete. A context belongs to that single request; don't keep it after
/// the handler returns.
///
/// Discord drops the request if no answer arrives within **3 seconds**, and the
/// user may type again before then, which sends a new request and makes the old
/// one obsolete. Handlers should be fast and shouldn't make network calls they
/// can avoid.
///
/// Unlike a [CommandContext], this can't reply with messages. The handler's
/// return value is the reply.
public interface AutocompleteContext {
    /// Returns the autocomplete interaction being answered.
    ///
    /// It identifies the command (`getName()`), the subcommand and group, if any
    /// (`getSubcommandName()`, `getSubcommandGroup()`), and who is typing and
    /// where (`getUser()`, `getGuild()`). It also holds the values of the
    /// options the user has already filled in, through `getOption(String)`.
    /// Those values may be partial or not yet valid, so treat them as hints.
    CommandAutoCompleteInteraction getInteraction();

    /// Returns the [Pinecone] instance.
    Pinecone getPinecone();

    /// Returns the option the user is currently typing into: its name, its type,
    /// and the raw text typed so far, which may be empty.
    ///
    /// Shorthand for [CommandAutoCompleteInteraction#getFocusedOption()].
    ///
    /// @return the focused option
    default AutoCompleteQuery getFocusedOption() {
        return getInteraction().getFocusedOption();
    }

    default Guild getGuild() {
        return getInteraction().getGuild();
    }

    default User getUser() {
        return getInteraction().getUser();
    }

    default Member getMember() {
        return getInteraction().getMember();
    }
}
