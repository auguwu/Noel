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

package dev.floofy.noel.github.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/// A request that hasn't been sent yet, in the style of JDA's `RestAction`.
/// Nothing happens until you call one of the terminal methods:
/// - [#submit()] sends it and returns a [CompletableFuture].
/// - [#complete()] sends it and blocks until it finishes.
///
/// Each terminal call sends a new request, so calling `queue()` twice sends it
/// twice.
/// @param <T> what the request completes with
@FunctionalInterface
public interface RestAction<T> {
    /// Sends the request.
    /// Cancelling the returned future cancels the HTTP call, if it's still in
    /// flight.
    ///
    /// @return a future completed with the result, or with the failure: a
    ///         [GitHubException] for an error response, or an `IOException` if
    ///         GitHub couldn't be reached
    CompletableFuture<T> submit();

    /// Sends the request and waits for it.
    ///
    /// Blocks the calling thread, so only use it where blocking is fine, such
    /// as a slash command handler. Never call it from another action's callback:
    /// that blocks one of the threads the result has to arrive on.
    ///
    /// @return the result
    /// @throws GitHubException if GitHub answered with an error
    /// @throws java.io.UncheckedIOException if GitHub couldn't be reached
    default T complete() throws GitHubException {
        try {
            return submit().join();
        } catch (CompletionException ex) {
            throw RestActions.rethrowable(RestActions.unwrap(ex));
        }
    }

    /// Transforms the result once it arrives. Lazy, like the action itself.
    ///
    /// @param mapper the transformation
    /// @param <R>    the new result type
    /// @return an action that completes with the transformed result
    default <R> RestAction<R> map(Function<? super T, ? extends R> mapper) {
        return () -> submit().thenApply(mapper);
    }

    /// Chains another request that needs this one's result, for example getting
    /// a user, then their repositories.
    ///
    /// @param next builds the next action from this one's result
    /// @param <R>  the next action's result type
    /// @return an action that completes with the next action's result
    default <R> RestAction<R> flatMap(Function<? super T, ? extends RestAction<R>> next) {
        return () -> submit().thenCompose(value -> next.apply(value).submit());
    }
}

/// Helpers for [RestAction]'s default methods, which interfaces can't keep privately.
final class RestActions {
    private RestActions() {}

    static Throwable unwrap(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }

        return error;
    }

    static RuntimeException rethrowable(Throwable error) {
        return switch (error) {
            case RuntimeException runtime -> runtime;
            case Error fatal -> throw fatal;
            case java.io.IOException io -> new java.io.UncheckedIOException(io);
            default -> new RuntimeException(error);
        };
    }
}
