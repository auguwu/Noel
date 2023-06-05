package dev.floofy.noel.modules.jda;

import dev.floofy.noel.modules.NoelModule;
import net.dv8tion.jda.api.JDA;

/**
 * Represents a {@link NoelModule} that can bind event listeners and modify the current {@link net.dv8tion.jda.api.JDA}
 * instance.
 */
public interface NoelJDAModule extends NoelModule {
    /**
     * Configures the given {@link JDA} instance that modules might
     * need to configure (i.e event listeners).
     *
     * @param jda JDA instance to configure.
     */
    void configure(JDA jda);
}
