package dev.floofy.noel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Tuple<T>(@Nullable T first, @NotNull T second) {
}
