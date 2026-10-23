package io.quarkiverse.it.amazon.bedrock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import software.amazon.awssdk.services.bedrock.BedrockAsyncClient;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.CreateGuardrailRequest;
import software.amazon.awssdk.services.bedrock.model.CreateGuardrailResponse;
import software.amazon.awssdk.services.bedrock.model.CreateGuardrailVersionResponse;
import software.amazon.awssdk.services.bedrock.model.GetGuardrailResponse;
import software.amazon.awssdk.services.bedrock.model.GuardrailContentFilterType;
import software.amazon.awssdk.services.bedrock.model.GuardrailFilterStrength;
import software.amazon.awssdk.services.bedrock.model.GuardrailSummary;
import software.amazon.awssdk.services.bedrock.model.ListGuardrailsResponse;

@Singleton
public class BedrockManager {
    private static final String BLOCKED_INPUT_MESSAGING = "Sorry, this input is not allowed.";
    private static final String BLOCKED_OUTPUTS_MESSAGING = "Sorry, this answer is not allowed.";

    @Inject
    BedrockClient sync;

    @Inject
    BedrockAsyncClient async;

    /**
     * Creates a guardrail, publishes a version of it, reads that version back, lists the guardrail versions and finally
     * deletes the guardrail.
     */
    public Map<String, Object> guardrailLifecycle(String name) {
        final CreateGuardrailResponse created = sync.createGuardrail(createGuardrailRequest(name));
        final String guardrailId = created.guardrailId();

        final CreateGuardrailVersionResponse version = sync
                .createGuardrailVersion(r -> r.guardrailIdentifier(guardrailId));
        final GetGuardrailResponse guardrail = sync
                .getGuardrail(r -> r.guardrailIdentifier(guardrailId).guardrailVersion(version.version()));
        final List<GuardrailSummary> versions = sync.listGuardrails(r -> r.guardrailIdentifier(guardrailId))
                .guardrails();

        sync.deleteGuardrail(r -> r.guardrailIdentifier(guardrailId));
        final boolean deleted = isDeleted(sync.listGuardrails(r -> {
        }), guardrailId);

        return result(guardrail, versions, deleted);
    }

    public CompletableFuture<Map<String, Object>> guardrailLifecycleAsync(String name) {
        return async.createGuardrail(createGuardrailRequest(name))
                .thenApply(CreateGuardrailResponse::guardrailId)
                .thenCompose(guardrailId -> async.createGuardrailVersion(r -> r.guardrailIdentifier(guardrailId))
                        .thenCompose(version -> async.getGuardrail(
                                r -> r.guardrailIdentifier(guardrailId).guardrailVersion(version.version())))
                        .thenCompose(guardrail -> async.listGuardrails(r -> r.guardrailIdentifier(guardrailId))
                                .thenCompose(versions -> async.deleteGuardrail(r -> r.guardrailIdentifier(guardrailId))
                                        .thenCompose(ignore -> async.listGuardrails(r -> {
                                        }))
                                        .thenApply(all -> result(guardrail, versions.guardrails(),
                                                isDeleted(all, guardrailId))))));
    }

    private static CreateGuardrailRequest createGuardrailRequest(String name) {
        return CreateGuardrailRequest.builder()
                .name(name)
                .blockedInputMessaging(BLOCKED_INPUT_MESSAGING)
                .blockedOutputsMessaging(BLOCKED_OUTPUTS_MESSAGING)
                .contentPolicyConfig(c -> c.filtersConfig(f -> f.type(GuardrailContentFilterType.HATE)
                        .inputStrength(GuardrailFilterStrength.HIGH)
                        .outputStrength(GuardrailFilterStrength.HIGH)))
                .build();
    }

    private static boolean isDeleted(ListGuardrailsResponse response, String guardrailId) {
        return response.guardrails().stream().noneMatch(g -> g.id().equals(guardrailId));
    }

    private static Map<String, Object> result(GetGuardrailResponse guardrail, List<GuardrailSummary> versions,
            boolean deleted) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", guardrail.name());
        result.put("version", guardrail.version());
        result.put("status", guardrail.statusAsString());
        result.put("blockedInputMessaging", guardrail.blockedInputMessaging());
        result.put("contentFilters", guardrail.contentPolicy().filters().stream()
                .map(f -> f.typeAsString()).toList());
        result.put("versions", versions.stream().map(GuardrailSummary::version).toList());
        result.put("deleted", deleted);
        return result;
    }
}
