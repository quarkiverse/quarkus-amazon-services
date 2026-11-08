package io.quarkiverse.amazon.appconfigdata.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
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
 * <li>Owns the supplied {@link AppConfigDataClient}. Without polling, it is closed right after the initial load;
 * otherwise {@link #close()} closes it, which {@link AppConfigDataConfigSourceCloser} calls on shutdown.</li>
 * </ul>
 * <p>
 * <strong>Parsing</strong> — The document body is interpreted as either JSON (objects and arrays are flattened to
 * dotted keys and {@code [index]} segments) or Java properties format. All values are exposed as strings.
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

    private final AtomicBoolean closed = new AtomicBoolean();

    /** AWS client */
    private final AppConfigDataClient client;
    private final ConfigSourceFormat format;

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
     * @param updateInterval interval between polls after the initial load; Greater than {@code 0s} enables polling
     * @param ordinal MicroProfile config source ordinal (precedence)
     */
    public AppConfigDataConfigSource(AppConfigDataClient client, String applicationIdentifier,
            String environmentIdentifier, String configurationProfileIdentifier, Integer requiredMinimumPollIntervalInSeconds,
            Duration updateInterval, int ordinal) {
        this(client, applicationIdentifier, environmentIdentifier, configurationProfileIdentifier,
                requiredMinimumPollIntervalInSeconds, updateInterval, ordinal, ConfigSourceFormat.AUTO);
    }

    public AppConfigDataConfigSource(AppConfigDataClient client, String applicationIdentifier,
            String environmentIdentifier, String configurationProfileIdentifier, Integer requiredMinimumPollIntervalInSeconds,
            Duration updateInterval, int ordinal, ConfigSourceFormat format) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.client = client;
        this.format = format;

        if (isBlank(applicationIdentifier) || isBlank(environmentIdentifier) || isBlank(configurationProfileIdentifier)) {
            LOG.error(
                    "AppConfig Data ConfigSource requires applicationIdentifier, environmentIdentifier, and configurationProfileIdentifier.");
            close();
            return;
        }

        token = startConfigurationSession(applicationIdentifier, environmentIdentifier, configurationProfileIdentifier,
                requiredMinimumPollIntervalInSeconds);
        fetchConfig();

        if (updateInterval.toSeconds() > 0) {
            LOG.debugf("Config update frequency set for %s", updateInterval);
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, POLL_THREAD_NAME);
                thread.setDaemon(true);
                return thread;
            };
            executorService = new ScheduledThreadPoolExecutor(1, threadFactory);
            executorService.scheduleAtFixedRate(this::fetchConfig, updateInterval.toSeconds(), updateInterval.toSeconds(),
                    TimeUnit.SECONDS);
        } else {
            // nothing will use the client again
            close();
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
            return initialToken;
        } catch (Exception ex) {
            ConfigSourceFailure.classify(ex).log(LOG, "Unable to start AppConfig configuration session");
            return null;
        }
    }

    /**
     * Fetches the latest configuration using the current {@link #token}, advances the token from the response, and
     * replaces {@link #entriesRef} when new non-empty content is present. Never throws, so a failed scheduled run
     * does not cancel subsequent polls.
     */
    void fetchConfig() {
        if (closed.get()) {
            return;
        }
        if (token == null) {
            LOG.warn("Config session token is null - skipping");
            return;
        }

        try {
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

            Map<String, String> parsed = format == ConfigSourceFormat.AUTO ? parseConfiguration(configString, res.contentType())
                    : format.decode("", configString);
            entriesRef.set(Collections.unmodifiableMap(new LinkedHashMap<>(parsed)));
            LOG.debugf("ConfigSource now exposes %d entries from AWS AppConfig Data.", parsed.size());
        } catch (Exception ex) {
            ConfigSourceFailure.classify(ex).log(LOG,
                    "Unable to fetch or parse AppConfig configuration; keeping the previous snapshot");
        }
    }

    /**
     * Parses the hosted configuration body into a flat {@code Map}. If the trimmed content looks like JSON (starts
     * with an object or array), it is parsed as JSON; otherwise as Java properties.
     * Parser diagnostics are omitted because they may contain configuration values.
     *
     * @throws IllegalArgumentException if the content cannot be parsed
     */
    static Map<String, String> parseConfiguration(String content) {
        return parseConfiguration(content, null);
    }

    static Map<String, String> parseConfiguration(String content, String contentType) {
        String trimmed = content.trim();
        String mime = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean yaml = mime.equals("application/yaml") || mime.equals("application/x-yaml")
                || mime.equals("text/yaml") || mime.equals("text/x-yaml") || mime.endsWith("+yaml");
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return ConfigSourceFormat.JSON.decode("", trimmed);
            } catch (IllegalArgumentException e) {
                if (!yaml) {
                    throw e;
                }
            }
        }
        if (yaml) {
            return ConfigSourceFormat.YAML.decode("", content);
        }
        // Use original content so properties files with leading comments or line continuations behave as expected.
        return ConfigSourceFormat.PROPERTIES.decode("", content);
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
        if (!closed.compareAndSet(false, true)) {
            return;
        }
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
