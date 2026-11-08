package io.quarkiverse.amazon.ssm.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.runtime.AwsCredentials;
import io.quarkiverse.amazon.common.runtime.AwsCredentials.ReadFailure;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import io.quarkus.credentials.CredentialsProvider;
import io.smallrye.common.annotation.Identifier;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

/** Uses the CDI-managed client; decrypts the selected parameters on every consumer request without caching. */
@ApplicationScoped
@Named("ssm-credentials-provider")
// Preserve named lookup without becoming an implicit @Default credentials provider.
@Identifier("ssm-credentials-provider")
public class SsmCredentialsProvider implements CredentialsProvider {
    @Inject
    SsmCredentialsProviderConfig config;

    @Inject
    SsmClient client;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        var entry = credentialsProviderName == null ? null : config.credentialsProvider().get(credentialsProviderName);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown SSM credentials provider.");
        }
        try {
            return AwsCredentials.resolve(entry.parameterName(), entry.parameters(), entry.format(),
                    entry.usernameKey().orElse(null), entry.passwordKey(), names -> read(names, entry.batchRetrieval()));
        } catch (ReadFailure failure) {
            throw new IllegalStateException("Unable to load SSM credentials [" + failure.failure() + "]");
        } catch (RuntimeException failure) {
            // SDK and parser exceptions may contain credentials; never propagate their messages or causes.
            throw new IllegalStateException("Unable to load SSM credentials [" + ConfigSourceFailure.classify(failure) + "]");
        }
    }

    private Map<String, String> read(List<String> names, boolean batch) {
        Map<String, String> values = new LinkedHashMap<>();
        if (batch) {
            var response = client.getParameters(GetParametersRequest.builder().names(names).withDecryption(true).build());
            if (response.invalidParameters().stream().anyMatch(name -> !names.contains(name))
                    || response.parameters().stream().anyMatch(parameter -> names.stream()
                            .noneMatch(name -> matches(name, parameter)))) {
                throw new ReadFailure(FetchFailure.PARTIAL_RESPONSE);
            }
            for (String name : names) {
                var matches = response.parameters().stream().filter(parameter -> matches(name, parameter)).toList();
                long missing = response.invalidParameters().stream().filter(name::equals).count();
                if (matches.size() + missing != 1) {
                    throw new ReadFailure(FetchFailure.PARTIAL_RESPONSE);
                }
                if (missing == 1) {
                    throw new ReadFailure(FetchFailure.MISSING);
                }
                values.put(name, matches.getFirst().value());
            }
        } else {
            for (String name : names) {
                values.put(name, client.getParameter(GetParameterRequest.builder().name(name).withDecryption(true).build())
                        .parameter().value());
            }
        }
        return values;
    }

    private static boolean matches(String requested, Parameter parameter) {
        String selector = parameter.selector() == null ? "" : parameter.selector();
        return requested.equals(parameter.name() + selector)
                || parameter.arn() != null && requested.equals(parameter.arn() + selector);
    }

}
