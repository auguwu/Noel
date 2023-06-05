package dev.floofy.noel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.floofy.noel.PreviousReferenceIterator;
import dev.floofy.noel.Tuple;
import dev.floofy.utils.java.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Config {
    private static final SetOnce<Config> instance = new SetOnce<>();
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private Map<String, Object> settings;

    public static Config get() {
        if (instance.wasSet()) return instance.getValue();

        final Config config = new Config();
        try {
            config.initialize();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration file", e);
        }

        instance.setValue(config);
        return get();
    }

    @SuppressWarnings({"unchecked"})
    protected void initialize() throws IOException {
        final File configFile = new File("../config.yml");
        if (!configFile.exists()) {
            throw new IllegalStateException("Unable to find ./config.yml file");
        }

        if (!configFile.isFile()) {
            throw new IllegalStateException("Path %s is not a file.".formatted(configFile));
        }

        LOG.info("Loading configuration options in location '{}'", configFile);
        settings = mapper.readValue(configFile, Map.class);

        LOG.trace("Successfully loaded configuration in location [{}]", configFile);
//        for (Map.Entry<String, Object> entry : settings.entrySet()) {
//            final String key = entry.getKey();
//
//            // If the key contains a '.'
//            if (key.contains(".")) {
//                final PreviousReferenceIterator<String> iter = new PreviousReferenceIterator<>(Arrays.stream(key.split("\\.")).toList());
//                while (iter.hasNext()) {
//                    final Tuple<String> item = iter.next();
//                    final String prev = item.first();
//                    final String next = item.second();
//
//                    // should prevent edge-cases like ".[].heck: true" (that shouldn't be even valid YAML to begin with)
//                    if (prev == null && next.length() == 2 && next.charAt(0) == '[' && next.charAt(1) == ']') {
//                        throw new IllegalStateException("Cannot use array indexing without a key with a dot.");
//                    }
//
//                    if (prev != null) {
//                        if (next.length() == 2 && next.charAt(0) == '[' && next.charAt(1) == ']') {
//                            final Object ref = settings.get(prev);
//                            if (!(ref instanceof List<?>)) {
//                                settings.remove(prev);
//                                settings.put(prev, List.of());
//                            }
//
//                            final List<Object> refAgain = (List<Object>)ref;
//                        }
//                    }
//                }
//            }
//        }
    }

    public <T> T get(@NotNull Setting<T> setting) {
        return get(setting.key, setting.identifySettingFn);
    }

    @Nullable
    public <T> T get(@NotNull String key, @NotNull Function<Object, T> resolver) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(resolver);

        return settings
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(key))
            .findAny()
            .map(entry -> resolver.apply(entry.getValue()))
            .orElse(null);
    }
}
