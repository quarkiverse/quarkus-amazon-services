package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.common.runtime.AbstractAmazonClientTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SecretsManagerConfigBootstrapRecorder {

    public void configure(RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder,
            Optional<List<String>> interceptors) {
        SecretsManagerBootstrapHolder.setup(transportRecorder.getValue(), interceptors);
    }

    public RuntimeValue<AbstractAmazonClientTransportRecorder> createUrlConnectionTransportRecorder() {
        return new RuntimeValue<AbstractAmazonClientTransportRecorder>(new AmazonClientUrlConnectionTransportRecorder());
    }

    public RuntimeValue<AbstractAmazonClientTransportRecorder> createApacheTransportRecorder() {
        return new RuntimeValue<AbstractAmazonClientTransportRecorder>(new AmazonClientApacheTransportRecorder());
    }

    public RuntimeValue<AbstractAmazonClientTransportRecorder> createNettyTransportRecorder() {
        return new RuntimeValue<AbstractAmazonClientTransportRecorder>(new AmazonClientNettyTransportRecorder());
    }

    public RuntimeValue<AbstractAmazonClientTransportRecorder> createAwsCrtTransportRecorder() {
        return new RuntimeValue<AbstractAmazonClientTransportRecorder>(new AmazonClientAwsCrtTransportRecorder());
    }
}
