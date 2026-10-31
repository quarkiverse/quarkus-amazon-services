package io.quarkiverse.amazon.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@QuarkusTest
class CoexistenceTest {
    @Inject
    Vertx vertx;

    @Inject
    EcrAsyncClient asyncGeneric;
    @Inject
    EcrClient generic;

    @Inject
    @AmazonClient("archive")
    EcrClient namedGeneric;

    @Inject
    SesV2Client dedicated;

    @Inject
    @AmazonClient("archive")
    SesV2Client namedDedicated;

    @Inject
    @AmazonClient("archive")
    SesV2AsyncClient asyncDedicated;

    @Test
    void genericAndDedicatedClientsKeepTheirOwnContextPropagationModes() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertNull(completionContext(caller, () -> asyncGeneric.describeRepositories()).get(30, TimeUnit.SECONDS));
        assertSame(caller, completionContext(caller, () -> asyncDedicated.getAccount(b -> {
        })).get(30, TimeUnit.SECONDS));
    }

    private CompletableFuture<Context> completionContext(Context caller, Supplier<CompletableFuture<?>> request) {
        CompletableFuture<Context> completed = new CompletableFuture<>();
        caller.runOnContext(ignored -> request.get().whenComplete((response, failure) -> {
            if (failure == null) {
                completed.completeExceptionally(new AssertionError("Expected connection failure"));
            } else {
                completed.complete(Vertx.currentContext());
            }
        }));
        return completed;
    }

    @Test
    void dedicatedClientsKeepTheirConfigurationAndUnclaimedClientsUseTheGenericExtension() {
        assertEquals(Region.EU_WEST_1, generic.serviceClientConfiguration().region());
        assertEquals(Region.AP_SOUTH_1, namedGeneric.serviceClientConfiguration().region());
        assertEquals(Region.EU_CENTRAL_1, dedicated.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, namedDedicated.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, asyncDedicated.serviceClientConfiguration().region());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String prefix = "io/quarkiverse/amazon/sdk/runtime/generated/software/amazon/awssdk/services/";
        assertNotNull(loader.getResource(prefix + "ecr/EcrClientBuilderFactory.class"));
        assertNull(loader.getResource(prefix + "sesv2/SesV2ClientBuilderFactory.class"));
        assertNull(loader.getResource(prefix + "sesv2/SesV2AsyncClientBuilderFactory.class"));
    }
}
