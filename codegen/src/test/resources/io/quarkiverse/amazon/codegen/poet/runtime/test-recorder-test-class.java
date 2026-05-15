package io.quarkiverse.amazon.ecr;

import io.quarkiverse.amazon.ecr.runtime.EcrSyntheticBean;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Generated("io.quarkiverse.amazon:codegen")
@Recorder
public class EcrTestRecorder {
    public EcrTestRecorder() {
    }

    public Function<SyntheticCreationalContext<EcrSyntheticBean>, EcrSyntheticBean> createSyntheticBean() {
        return context -> {
            Instance<EcrAsyncClient> asyncRef = context.getInjectedReference(new TypeLiteral<Instance<EcrAsyncClient>>() {
            });
            Instance<EcrClient> syncRef = context.getInjectedReference(new TypeLiteral<Instance<EcrClient>>() {
            });
            return new EcrSyntheticBean(asyncRef, syncRef);
        };
    }
}