package io.quarkiverse.amazon.common.deployment;

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
