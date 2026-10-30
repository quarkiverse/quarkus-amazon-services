package io.quarkiverse.amazon.sdk.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;

import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientOwnershipBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkBuildTimeConfig;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.configuration.ConfigurationException;

public class AmazonSdkProcessor {
    private static final String SERVICE_PACKAGE_PREFIX = "software.amazon.awssdk.services.";
    private static final String SERVICE_PACKAGE_PATH = "software/amazon/awssdk/services";
    private static final DotName AWS_SYNC_CLIENT_BUILDER = DotName
            .createSimple("software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder");

    @BuildStep
    AmazonSdkIndexBuildItem indexSdk(CurateOutcomeBuildItem curate) {
        Indexer indexer = new Indexer();
        curate.getApplicationModel().getRuntimeDependencies().stream()
                .filter(d -> d.getGroupId().equals("software.amazon.awssdk"))
                .forEach(d -> d.getContentTree().walkIfContains(SERVICE_PACKAGE_PATH, visit -> {
                    if (visit.getResourceName().endsWith(".class")) {
                        try (InputStream in = Files.newInputStream(visit.getPath())) {
                            indexer.index(in);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to index " + visit.getResourceName(), e);
                        }
                    }
                }));
        return new AmazonSdkIndexBuildItem(indexer.complete());
    }

    @BuildStep
    void discover(AmazonSdkIndexBuildItem index, List<AmazonClientOwnershipBuildItem> ownership,
            BuildProducer<AmazonSdkServiceBuildItem> services) {
        IndexView sdkIndex = index.index;
        Set<DotName> syncBuilderTypes = sdkIndex.getAllKnownSubinterfaces(AWS_SYNC_CLIENT_BUILDER).stream()
                .map(ClassInfo::name).collect(Collectors.toSet());
        syncBuilderTypes.add(AWS_SYNC_CLIENT_BUILDER);
        Set<DotName> ownedTypes = ownership.stream().flatMap(item -> item.getClientTypes().stream())
                .collect(Collectors.toSet());
        for (ClassInfo client : sdkIndex.getKnownClasses()) {
            if (!isStandardSyncClient(client, syncBuilderTypes)) {
                continue;
            }
            DotName asyncClient = asyncClientName(client.name());
            if (ownedTypes.contains(client.name()) || ownedTypes.contains(asyncClient)) {
                continue;
            }
            MethodInfo syncFactory = builderFactory(client);
            MethodInfo asyncFactory = builderFactory(sdkIndex.getClassByName(asyncClient));
            if (asyncFactory == null) {
                throw new ConfigurationException("Missing standard builder() factory for AWS async client " + asyncClient);
            }
            String service = client.name().packagePrefix().substring(SERVICE_PACKAGE_PREFIX.length());
            services.produce(new AmazonSdkServiceBuildItem(service, client.name(), asyncClient,
                    syncFactory.returnType().name(), asyncFactory.returnType().name()));
        }
    }

    private static boolean isStandardSyncClient(ClassInfo client, Set<DotName> syncBuilderTypes) {
        String name = client.name().toString();
        if (!client.isInterface() || !name.startsWith(SERVICE_PACKAGE_PREFIX)
                || !name.endsWith("Client") || name.endsWith("AsyncClient")) {
            return false;
        }
        MethodInfo factory = builderFactory(client);
        return factory != null && syncBuilderTypes.contains(factory.returnType().name());
    }

    private static MethodInfo builderFactory(ClassInfo client) {
        MethodInfo factory = client == null ? null : client.method("builder");
        return factory != null && Modifier.isPublic(factory.flags()) && Modifier.isStatic(factory.flags()) ? factory : null;
    }

    private static DotName asyncClientName(DotName syncClient) {
        String name = syncClient.toString();
        return DotName.createSimple(name.substring(0, name.length() - "Client".length()) + "AsyncClient");
    }

    @BuildStep
    void prepare(List<AmazonSdkServiceBuildItem> services, AmazonSdkBuildTimeConfig config,
            BuildProducer<AmazonSdkServiceProcessorBuildItem> processors) {
        for (AmazonSdkServiceBuildItem service : services) {
            processors.produce(new AmazonSdkServiceProcessorBuildItem(new GenericServiceProcessor(service, config)));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(List<AmazonSdkServiceProcessorBuildItem> processors, AmazonSdkRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> extensions) {
        for (AmazonSdkServiceProcessorBuildItem item : processors) {
            item.processor.setup(recorder, extensions);
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void builders(List<AmazonSdkServiceProcessorBuildItem> processors, AmazonSdkRecorder recorder,
            RecorderContext context,
            List<RequireAmazonClientInjectionBuildItem> injections,
            BuildProducer<GeneratedClassBuildItem> classes,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builders) {
        for (AmazonSdkServiceProcessorBuildItem item : processors) {
            item.processor.builders(recorder, injections, context, classes, builders);
        }
    }
}
