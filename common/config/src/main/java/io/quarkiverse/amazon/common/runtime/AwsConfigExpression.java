package io.quarkiverse.amazon.common.runtime;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;

/** Runtime-only handler; the empty source makes its owned client discoverable for shutdown, not for value lookup. */
public final class AwsConfigExpression implements SecretKeysHandlerFactory, ConfigSource, AutoCloseable {
    public interface ValueProvider extends AutoCloseable {
        String fetch(String resource) throws Exception;

        @Override
        void close();
    }

    private record Cached(String value, long received) {
    }

    private final String name;
    private final String service;
    private final Function<ConfigSourceContext, ValueProvider> factory;
    private final Map<String, Cached> cache = new LinkedHashMap<>(16, 0.75f, true);
    private ValueProvider provider;
    private long ttl;
    private boolean closed;
    private boolean initializing;

    public AwsConfigExpression(String name, String service, Function<ConfigSourceContext, ValueProvider> factory) {
        this.name = name;
        this.service = service;
        this.factory = factory;
    }

    @Override
    public SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context) {
        return new SecretKeysHandler() {
            public String decode(String expression) {
                return resolve(context, expression);
            }

            public String getName() {
                return name;
            }
        };
    }

    // shortcut: cold reads serialize per handler; use per-resource coordination if this becomes a measured bottleneck.
    private synchronized String resolve(ConfigSourceContext context, String expression) {
        String resource = expression;
        try {
            if (closed || expression == null || expression.isBlank()) {
                throw new IllegalStateException();
            }
            int separator = expression.indexOf('#');
            resource = separator < 0 ? expression : expression.substring(0, separator);
            if (resource.isBlank() || separator >= 0 && !expression.substring(separator + 1).startsWith("/")) {
                throw new IllegalArgumentException();
            }
            if (provider == null) {
                if (initializing) {
                    throw new IllegalStateException();
                }
                initializing = true;
                try {
                    // Handler contexts already resolve profiles and expressions, but do not expose profiles or source lists.
                    var resolved = resolvedContext(context);
                    var value = context.getValue("quarkus." + service + ".expression.cache-ttl");
                    ttl = (value == null || value.getValue() == null ? Duration.ofMinutes(5)
                            : io.quarkus.runtime.configuration.DurationConverter.parseDuration(value.getValue())).toNanos();
                    if (ttl < 0) {
                        throw new IllegalArgumentException();
                    }
                    provider = factory.apply(resolved);
                } finally {
                    initializing = false;
                }
            }
            Cached cached = cache.get(resource);
            String value;
            if (cached != null && System.nanoTime() - cached.received() < ttl) {
                value = cached.value();
            } else {
                cache.remove(resource);
                value = utf8Text(provider.fetch(resource));
                if (ttl > 0) {
                    cache.put(resource, new Cached(value, System.nanoTime()));
                    if (cache.size() > 256) {
                        cache.remove(cache.keySet().iterator().next());
                    }
                }
            }
            if (separator < 0) {
                return value;
            }
            JsonValue selected = select(value, expression.substring(separator + 1));
            return utf8Text(switch (selected.getValueType()) {
                case STRING -> ((JsonString) selected).getString();
                case NUMBER, TRUE, FALSE -> selected.toString();
                default -> throw new IllegalArgumentException();
            });
        } catch (Exception failure) {
            cache.remove(resource);
            if (provider != null) {
                var failed = provider;
                provider = null;
                try {
                    failed.close();
                } catch (Exception ignored) {
                    // Preserve the sanitized resolution failure, never a cleanup exception containing SDK data.
                }
            }
            // SDK, parser and config conversion exceptions can contain the secret or bootstrap credentials.
            throw new IllegalStateException("Unable to resolve AWS " + name
                    + " expression; check resource, permissions, content and client settings.");
        }
    }

    private static ConfigSourceContext resolvedContext(ConfigSourceContext context) {
        ConfigSource source = new ConfigSourceContext.ConfigSourceContextConfigSource(context);
        return new ConfigSourceContext() {
            public ConfigValue getValue(String name) {
                return context.getValue(name);
            }

            public Iterator<String> iterateNames() {
                return context.iterateNames();
            }

            public List<String> getProfiles() {
                return List.of();
            }

            public List<ConfigSource> getConfigSources() {
                return List.of(source);
            }
        };
    }

    private static String utf8Text(String value) {
        if (value == null || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    private static JsonValue select(String content, String pointer) {
        JsonValue selected = JsonConfigFlattener.parseJson(content);
        for (String token : pointer.substring(1).split("/", -1)) {
            if (token.replaceAll("~[01]", "").contains("~")) {
                throw new IllegalArgumentException();
            }
            token = token.replace("~1", "/").replace("~0", "~");
            if (selected instanceof JsonObject object) {
                selected = object.get(token);
            } else if (selected instanceof JsonArray array && token.matches("0|[1-9][0-9]*")) {
                selected = array.get(Integer.parseInt(token));
            } else {
                throw new IllegalArgumentException();
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException();
        }
        return selected;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cache.clear();
            if (provider != null) {
                var closing = provider;
                provider = null;
                try {
                    closing.close();
                } catch (Exception failure) {
                    throw new IllegalStateException("Unable to close AWS " + name + " expression client.");
                }
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        return null;
    }
}
