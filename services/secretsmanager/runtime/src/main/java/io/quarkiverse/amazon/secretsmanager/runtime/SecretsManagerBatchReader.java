package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.RawDocument;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;

/** Correlates explicit batch reads for both configuration and credentials; never owns the supplied client. */
final class SecretsManagerBatchReader {
    private SecretsManagerBatchReader() {
    }

    static void fetch(SecretsManagerClient client, List<String> requested, Map<ResourceId, RawDocument> updated,
            Map<ResourceId, FetchFailure> failed) {
        try {
            var response = client.batchGetSecretValue(BatchGetSecretValueRequest.builder().secretIdList(requested).build());
            Set<String> exactIds = new HashSet<>();
            response.secretValues().forEach(value -> {
                exactIds.add(value.arn());
                exactIds.add(value.name());
            });
            // Explicit IDs need no pagination; never publish an incomplete or unrequested response.
            if (response.nextToken() != null
                    || response.secretValues().stream().anyMatch(value -> requested.stream()
                            .noneMatch(id -> matchesValue(id, value, exactIds)))
                    || response.errors().stream().anyMatch(error -> requested.stream()
                            .noneMatch(id -> matches(id, error.secretId(), error.secretId())))) {
                requested.forEach(id -> failed.put(new ResourceId(id), FetchFailure.PARTIAL_RESPONSE));
                return;
            }
            for (String secretId : requested) {
                var id = new ResourceId(secretId);
                var values = response.secretValues().stream()
                        .filter(value -> matchesValue(secretId, value, exactIds)).toList();
                var errors = response.errors().stream()
                        .filter(error -> matches(secretId, error.secretId(), error.secretId())).toList();
                if (values.size() + errors.size() != 1) {
                    failed.put(id, FetchFailure.PARTIAL_RESPONSE);
                } else if (!errors.isEmpty()) {
                    failed.put(id, classify(errors.getFirst().errorCode()));
                } else {
                    SecretValueEntry value = values.getFirst();
                    if ((value.secretString() == null) == (value.secretBinary() == null)) {
                        failed.put(id, FetchFailure.INVALID_CONTENT);
                    } else {
                        updated.put(id, value.secretString() != null
                                ? RawDocument.text(id, value.secretString(), null, value.versionId())
                                : new RawDocument(id, value.secretBinary().asByteArray(), null, value.versionId()));
                    }
                }
            }
        } catch (Exception e) {
            requested.forEach(id -> {
                updated.remove(new ResourceId(id));
                failed.put(new ResourceId(id), ConfigSourceFailure.classify(e));
            });
        }
    }

    private static boolean matchesValue(String requested, SecretValueEntry value, Set<String> exactIds) {
        // A full ARN may also be another secret's partial ARN; only use partial matches without an exact result.
        return requested.equals(value.arn()) || requested.equals(value.name())
                || !exactIds.contains(requested) && matches(requested, value.arn(), value.name());
    }

    private static boolean matches(String requested, String arn, String name) {
        return requested.equals(arn) || requested.equals(name)
                || arn != null && arn.startsWith("arn:") && arn.length() > 7
                        && arn.charAt(arn.length() - 7) == '-' && requested.equals(arn.substring(0, arn.length() - 7));
    }

    private static FetchFailure classify(String code) {
        if (code == null) {
            return FetchFailure.OTHER;
        }
        return switch (code) {
            case "ResourceNotFoundException" -> FetchFailure.MISSING;
            case "AccessDeniedException", "AccessDenied", "KMSAccessDeniedException" -> FetchFailure.ACCESS_DENIED;
            case "InternalServiceError", "InternalServiceErrorException", "ThrottlingException" -> FetchFailure.TRANSIENT;
            default -> FetchFailure.OTHER;
        };
    }
}
