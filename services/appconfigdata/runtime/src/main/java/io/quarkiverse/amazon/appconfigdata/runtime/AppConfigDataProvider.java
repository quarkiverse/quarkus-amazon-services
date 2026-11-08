package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkiverse.amazon.common.config.AwsConfigProvider;
import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.FetchResult;
import io.quarkiverse.amazon.common.config.RawDocument;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestException;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestReason;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.InvalidParameterProblem;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

/** Owns AppConfig sessions and single-use tokens, independently of document publication. */
public final class AppConfigDataProvider implements AwsConfigProvider {
    private static final SdkPlugin NO_POLL_RETRIES = configuration -> configuration.overrideConfiguration(
            overrides -> overrides.retryStrategy(StandardRetryStrategy.builder().maxAttempts(1).build()));

    public record Profile(String application, String environment, String configurationProfile,
            Integer requiredMinimumPollIntervalInSeconds) {
        public Profile {
            application = identifier(application);
            environment = identifier(environment);
            configurationProfile = identifier(configurationProfile);
            if (requiredMinimumPollIntervalInSeconds != null
                    && (requiredMinimumPollIntervalInSeconds < 15 || requiredMinimumPollIntervalInSeconds > 86400)) {
                throw new IllegalArgumentException("AppConfig polling minimum must be between 15 and 86400 seconds.");
            }
        }

        public ResourceId id() {
            return new ResourceId("appconfigdata:" + application.length() + ":" + application
                    + environment.length() + ":" + environment
                    + configurationProfile.length() + ":" + configurationProfile);
        }

        private static String identifier(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("AppConfig requires application, environment and profile identifiers.");
            }
            return value.trim();
        }
    }

    private final AppConfigDataClient client;
    private final Map<ResourceId, Session> sessions = new LinkedHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AppConfigDataProvider(AppConfigDataClient client, List<Profile> profiles) {
        this.client = Objects.requireNonNull(client);
        for (Profile profile : profiles) {
            Session session = sessions.computeIfAbsent(profile.id(), id -> new Session(profile));
            int minimum = profile.requiredMinimumPollIntervalInSeconds() == null ? 60
                    : profile.requiredMinimumPollIntervalInSeconds();
            session.minimum = Math.max(session.minimum, minimum);
            session.delay = session.minimum;
        }
    }

    @Override
    public synchronized FetchResult fetchAll() {
        ensureOpen();
        Map<ResourceId, RawDocument> updated = new LinkedHashMap<>();
        Set<ResourceId> unchanged = new HashSet<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        int delay = 0;
        for (var entry : sessions.entrySet()) {
            ensureOpen();
            ResourceId id = entry.getKey();
            Session session = entry.getValue();
            try {
                if (session.token == null) {
                    var response = client.startConfigurationSession(StartConfigurationSessionRequest.builder()
                            .applicationIdentifier(session.profile.application())
                            .environmentIdentifier(session.profile.environment())
                            .configurationProfileIdentifier(session.profile.configurationProfile())
                            .requiredMinimumPollIntervalInSeconds(session.minimum)
                            .build());
                    session.token = response == null ? null : token(response.initialConfigurationToken());
                }
                ensureOpen();
                if (session.token == null) {
                    failed.put(id, FetchFailure.PARTIAL_RESPONSE);
                } else {
                    GetLatestConfigurationResponse response = poll(session);
                    if (response == null) {
                        failed.put(id, FetchFailure.PARTIAL_RESPONSE);
                    } else {
                        session.token = token(response.nextPollConfigurationToken());
                        Integer nextDelay = response.nextPollIntervalInSeconds();
                        if (nextDelay != null && nextDelay > 0) {
                            session.delay = Math.max(session.minimum, nextDelay);
                        }
                        byte[] bytes = response.configuration() == null ? new byte[0]
                                : response.configuration().asByteArray();
                        if (bytes.length == 0 && session.hasDocument) {
                            unchanged.add(id);
                        } else {
                            updated.put(id, new RawDocument(id, bytes, response.contentType(), response.versionLabel()));
                            session.hasDocument = true;
                        }
                    }
                }
            } catch (Exception failure) {
                failed.put(id, ConfigSourceFailure.classify(failure));
            }
            delay = Math.max(delay, session.delay);
        }
        return new FetchResult(updated, unchanged, Set.of(), failed, null, Duration.ofSeconds(delay));
    }

    private GetLatestConfigurationResponse poll(Session session) {
        ensureOpen();
        String currentToken = session.token;
        // A lost response may have consumed the token; never replay it, including inside the SDK.
        session.token = null;
        try {
            return client.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                    .configurationToken(currentToken)
                    .overrideConfiguration(overrides -> overrides.addPlugin(NO_POLL_RETRIES))
                    .build());
        } catch (BadRequestException failure) {
            if (!isTokenFailure(failure)) {
                session.token = currentToken;
            }
            throw failure;
        }
    }

    private static boolean isTokenFailure(BadRequestException failure) {
        return failure.reason() == BadRequestReason.INVALID_PARAMETERS && failure.details() != null
                && failure.details().invalidParameters().entrySet().stream()
                        .anyMatch(parameter -> parameter.getKey().equalsIgnoreCase("ConfigurationToken")
                                && (parameter.getValue().problem() == InvalidParameterProblem.EXPIRED
                                        || parameter.getValue().problem() == InvalidParameterProblem.CORRUPTED));
    }

    private static String token(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("AppConfig provider is closed.");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            client.close();
        }
    }

    private static final class Session {
        final Profile profile;
        int minimum;
        int delay;
        String token;
        boolean hasDocument;

        Session(Profile profile) {
            this.profile = profile;
        }
    }
}
