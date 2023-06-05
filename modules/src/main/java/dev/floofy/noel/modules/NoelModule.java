package dev.floofy.noel.modules;

import com.google.inject.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that all modules must implement to be represented as a "module."
 */
public interface NoelModule extends Module {
    /**
     * Method to implement when this module is in scope when Noel is bootstrapping.
     */
    default void onInit() {
    }

    /**
     * Method to implement when this module is being disposed by the
     * shutdown thread.
     */
    default void dispose() {
    }

    /**
     * The module name.
     */
    @NotNull
    String name();
}
