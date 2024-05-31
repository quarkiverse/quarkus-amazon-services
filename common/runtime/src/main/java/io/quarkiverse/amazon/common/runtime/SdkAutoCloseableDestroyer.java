package io.quarkiverse.amazon.common.runtime;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class SdkAutoCloseableDestroyer implements BeanDestroyer<SdkAutoCloseable> {

    @Override
    public void destroy(SdkAutoCloseable instance, CreationalContext<SdkAutoCloseable> creationalContext,
            Map<String, Object> params) {
        instance.close();
    }
}
