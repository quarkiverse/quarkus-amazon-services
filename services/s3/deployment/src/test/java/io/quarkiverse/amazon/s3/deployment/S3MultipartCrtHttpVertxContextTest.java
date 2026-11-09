package io.quarkiverse.amazon.s3.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class S3MultipartCrtHttpVertxContextTest extends AbstractS3MultipartVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = configure(true)
            .overrideConfigKey("quarkus.s3.async-client.type", "aws-crt");
}
