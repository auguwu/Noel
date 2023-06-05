package dev.floofy.noel.modules.jda;

import dev.floofy.noel.modules.AbstractNoelModule;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNoelJDAModule extends AbstractNoelModule implements NoelJDAModule {
    private JDA jda;

    /**
     * Configures the current JDA instance that is binded to this abstract module,
     * this can be used to apply event listeners. This will also allow you to bind
     * objects together with Guice
     */
    protected void configure() {}

    @Override
    public synchronized void configure(JDA jda) {
        if (this.jda != null) {
            throw new IllegalStateException("Configuring a JDA module cannot be called more than twice.");
        }

        this.jda = jda;
        try {
            configure();
            super.configure();
        } finally {
            this.jda = null;
        }
    }

    /**
     * Returns the given {@link JDA} instance that was used to bind this module, this can
     * be only used in {@link #configure()}.
     */
    @NotNull
    public JDA getJDA() {
        if (jda == null) {
            throw new IllegalStateException("JDA can only be called in #configure()");
        }

        return jda;
    }
}
