package dev.floofy.noel.modules;

import com.google.inject.AbstractModule;

/**
 * Represents a {@link NoelModule} that can be used with {@link AbstractModule} to create
 * the injector that Noel uses to bind modules with Guice.
 */
public abstract class AbstractNoelModule extends AbstractModule implements NoelModule {
}
