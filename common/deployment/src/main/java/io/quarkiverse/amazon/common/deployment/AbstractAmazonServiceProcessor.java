package io.quarkiverse.amazon.common.deployment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;

abstract public class AbstractAmazonServiceProcessor {

    abstract protected String amazonServiceClientName();

    abstract protected String configName();

    abstract protected DotName syncClientName();

    abstract protected Class<?> syncClientBuilderClass();

    abstract protected DotName asyncClientName();

    abstract protected Class<?> asyncClientBuilderClass();

    protected DotName presignerClientName() {
        return null;
    }

    protected Class<?> presignerBuilderClass() {
        return null;
    }

    abstract protected String builtinInterceptorsPath();

    abstract protected HasTransportBuildTimeConfig transportBuildTimeConfig();

    abstract protected HasSdkBuildTimeConfig sdkBuildTimeConfig();

    protected void createExtensionBuilders(AmazonClientRecorder recorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        // requiring named clients can originate from multiple sources and we may have duplicates
        Collection<String> syncClientNames = amazonClientInjections.stream()
                .filter(c -> syncClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> asyncClientNames = amazonClientInjections.stream()
                .filter(c -> asyncClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> presignerClientNames = amazonClientInjections.stream()
                .filter(c -> presignerClientName() != null && presignerClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        for (String clientName : syncClientNames) {
            builderIstances.produce(new AmazonClientExtensionBuilderInstanceBuildItem(clientName,
                    syncClientBuilderClass(), recorder.getSyncBuilder()));
        }

        for (String clientName : asyncClientNames) {
            builderIstances.produce(new AmazonClientExtensionBuilderInstanceBuildItem(clientName,
                    asyncClientBuilderClass(), recorder.getAsyncBuilder()));
        }

        for (String clientName : presignerClientNames) {
            builderIstances.produce(new AmazonClientExtensionBuilderInstanceBuildItem(clientName,
                    presignerBuilderClass(), recorder.createPresignerBuilder()));
        }
    }

    protected void setupExtension(
            AmazonClientRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {
        amazonExtensions.produce(
                new AmazonClientExtensionBuildItem(
                        configName(),
                        amazonServiceClientName(),
                        builtinInterceptorsPath(),
                        syncClientName(),
                        syncClientBuilderClass(),
                        asyncClientName(),
                        asyncClientBuilderClass(),
                        presignerClientName(),
                        presignerBuilderClass(),
                        recorder,
                        transportBuildTimeConfig(),
                        sdkBuildTimeConfig()));
    }

}
