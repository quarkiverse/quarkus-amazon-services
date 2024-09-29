package io.quarkiverse.it.amazon.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class S3ModifyResponseProducer {

    @Produces
    @ApplicationScoped
    S3ModifyResponse produce() {
        return new S3ModifyResponse("INTERCEPTED");
    }
}
