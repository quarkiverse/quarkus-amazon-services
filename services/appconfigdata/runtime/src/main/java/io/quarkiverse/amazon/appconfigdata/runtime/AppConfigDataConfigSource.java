package io.quarkiverse.amazon.appconfigdata.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.jboss.logging.Logger;

import io.smallrye.config.common.AbstractConfigSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

/**
 * MicroProfile {@link org.eclipse.microprofile.config.spi.ConfigSource} backed by the AWS AppConfig Data API
 * ({@code StartConfigurationSession} / {@code GetLatestConfiguration}).
 * <p>
 * <strong>Lifecycle</strong>
 * <ul>
 * <li>Opens a configuration session for the given application, environment, and configuration profile.</li>
 * <li>Loads the hosted configuration document once during construction, then optionally on a fixed schedule.</li>
 * <li>Owns the supplied {@link AppConfigDataClient}; callers must {@link #close()} it when the source is discarded.</li>
 * </ul>
 * <p>
 * <strong>Parsing</strong> — The document body is interpreted as either JSON (objects and arrays are flattened to
 * dotted keys and {@code [index]} segments) or Java {@link Properties} format. All values are exposed as strings.
 * <p>
 * <strong>Concurrency</strong> — The latest snapshot is published via {@link AtomicReference}; the poll token is
 * {@code volatile}. {@link #getValue(String)} and {@link #getPropertyNames()} read the current snapshot without
 * blocking the polling thread.
 */
public class AppConfigDataConfigSource extends AbstractConfigSource implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(AppConfigDataConfigSource.class);
    private static final String CONFIG_SOURCE_NAME = "quarkus.appconfigdata.config";

    /** Background thread name for scheduled {@link #fetchConfig()} runs. */
    private static final String POLL_THREAD_NAME = "quarkus-amazon-appconfigdata-config-poll";

    /** Current flattened configuration; replaced atomically after each successful parse. */
    private final AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>(Map.of());

    /** AWS client */
    private final AppConfigDataClient client;

    /**
     * Session token for {@link GetLatestConfigurationRequest#configurationToken()}; updated from each response's
     * {@link GetLatestConfigurationResponse#nextPollConfigurationToken()} (including when the payload is unchanged).
     */
    private volatile String token;

    /** Non-null only when {@code updateIntervalMinutes &gt; 0} in the constructor. */
    private ScheduledExecutorService executorService;

    /**
     * @param client client used for the session and polls; closed by {@link #close()}
     * @param applicationIdentifier AppConfig application id or name
     * @param environmentIdentifier AppConfig environment id or name
     * @param configurationProfileIdentifier AppConfig configuration profile id or name
     * @param requiredMinimumPollIntervalInSeconds optional minimum poll interval for the session (AWS)
     * @param updateIntervalMinutes minutes between polls after the initial load; {@code 0} disables polling
     * @param ordinal MicroProfile config source ordinal (precedence)
     */
    public AppConfigDataConfigSource(AppConfigDataClient client, String applicationIdentifier,
            String environmentIdentifier, String configurationProfileIdentifier, Integer requiredMinimumPollIntervalInSeconds,
            int updateIntervalMinutes, int ordinal) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.client = client;

        if (isBlank(applicationIdentifier) || isBlank(environmentIdentifier) || isBlank(configurationProfileIdentifier)) {
            LOG.error(
                    "AppConfig Data ConfigSource requires applicationIdentifier, environmentIdentifier, and configurationProfileIdentifier.");
            return;
        }

        token = startConfigurationSession(applicationIdentifier, environmentIdentifier, configurationProfileIdentifier,
                requiredMinimumPollIntervalInSeconds);
        fetchConfig();

        if (updateIntervalMinutes > 0) {
            LOG.debugf("Config update frequency set for %d minutes", updateIntervalMinutes);
            ThreadFactory threadFactory = runnable -> new Thread(runnable, POLL_THREAD_NAME);
            executorService = new ScheduledThreadPoolExecutor(1, threadFactory);
            executorService.scheduleAtFixedRate(this::fetchConfig, updateIntervalMinutes, updateIntervalMinutes,
                    TimeUnit.MINUTES);
        }
    }

    /** Starts the AppConfig Data session and returns the initial poll token, or {@code null} on failure. */
    private String startConfigurationSession(String applicationIdentifier, String environmentIdentifier,
            String configurationProfileIdentifier, Integer requiredMinimumPollIntervalInSeconds) {
        try {
            LOG.infof("Starting configuration session for: [%s, %s, %s]", applicationIdentifier, environmentIdentifier,
                    configurationProfileIdentifier);

            var sessionBuilder = StartConfigurationSessionRequest.builder()
                    .applicationIdentifier(applicationIdentifier.trim())
                    .environmentIdentifier(environmentIdentifier.trim())
                    .configurationProfileIdentifier(configurationProfileIdentifier.trim());
            if (requiredMinimumPollIntervalInSeconds != null) {
                sessionBuilder.requiredMinimumPollIntervalInSeconds(requiredMinimumPollIntervalInSeconds);
            }

            var session = client.startConfigurationSession(sessionBuilder.build());
            String initialToken = session.initialConfigurationToken();
            LOG.debugf("Got configuration token: %s", initialToken);
            return initialToken;
        } catch (Exception ex) {
            LOG.errorf(ex, "Unable to start configuration session");
            return null;
        }
    }

    /**
     * Fetches the latest configuration using the current {@link #token}, advances the token from the response, and
     * replaces {@link #entriesRef} when new non-empty content is present.
     */
    private void fetchConfig() {
        if (token == null) {
            LOG.warn("Config session token is null - skipping");
            return;
        }

        LOG.debugf("Fetching configuration for token: %s", token);

        GetLatestConfigurationResponse res = client.getLatestConfiguration(
                GetLatestConfigurationRequest.builder().configurationToken(token).build());

        // Always advance the poll token when AWS returns one, even if configuration is null (unchanged document).
        String nextPoll = res.nextPollConfigurationToken();
        if (nextPoll != null) {
            token = nextPoll;
        }

        SdkBytes configuration = res.configuration();
        if (configuration == null) {
            LOG.debug("AppConfig Data returned no configuration payload (unchanged).");
            return;
        }

        String configString = configuration.asUtf8String();
        if (configString.isEmpty()) {
            LOG.debug("No changes on the configuration received");
            return;
        }

        try {
            Map<String, String> parsed = parseConfiguration(configString);
            entriesRef.set(Collections.unmodifiableMap(new LinkedHashMap<>(parsed)));
            LOG.debugf("ConfigSource now exposes %d entries from AWS AppConfig Data.", parsed.size());
        } catch (Exception ex) {
            LOG.errorf(ex, "Unexpected failure reading the configuration");
        }
    }

    /**
     * Parses the hosted configuration body into a flat {@code Map}. If the trimmed content looks like JSON (starts
     * with an object or array), it is parsed as JSON; otherwise as Java {@link Properties}.
     */
    static Map<String, String> parseConfiguration(String content) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyMap();
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try (JsonReader reader = Json.createReader(new StringReader(trimmed))) {
                JsonValue root = reader.readValue();
                Map<String, String> flat = new LinkedHashMap<>();
                flattenJson("", root, flat);
                return flat;
            } catch (Exception e) {
                LOG.warnf(e, "Failed to parse configuration as JSON; treating as empty.");
                return Collections.emptyMap();
            }
        }
        // Use original content so properties files with leading comments or line continuations behave as expected.
        Properties p = new Properties();
        try {
            p.load(new StringReader(content));
        } catch (Exception e) {
            LOG.warnf(e, "Failed to parse configuration as Java properties; treating as empty.");
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : p.stringPropertyNames()) {
            map.put(name, p.getProperty(name));
        }
        return map;
    }

    /**
     * Flattens JSON into string keys: object keys are joined with {@code '.'}; array indices use {@code [n]}.
     * {@code NULL} values are omitted. Scalar values are stored as strings.
     */
    private static void flattenJson(String prefix, JsonValue value, Map<String, String> out) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObject obj = value.asJsonObject();
                for (String key : obj.keySet()) {
                    String p = prefix.isEmpty() ? key : prefix + "." + key;
                    flattenJson(p, obj.get(key), out);
                }
                break;
            case ARRAY:
                JsonArray arr = value.asJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    flattenJson(prefix + "[" + i + "]", arr.get(i), out);
                }
                break;
            case STRING:
                out.put(prefix, ((JsonString) value).getString());
                break;
            case NUMBER:
                out.put(prefix, value.toString());
                break;
            case TRUE:
                out.put(prefix, "true");
                break;
            case FALSE:
                out.put(prefix, "false");
                break;
            case NULL:
            default:
                break;
        }
    }

    private Map<String, String> rawEntries() {
        return entriesRef.get();
    }

    @Override
    public Set<String> getPropertyNames() {
        return rawEntries().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return rawEntries().get(propertyName);
    }

    @Override
    public void close() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        client.close();
    }
}
