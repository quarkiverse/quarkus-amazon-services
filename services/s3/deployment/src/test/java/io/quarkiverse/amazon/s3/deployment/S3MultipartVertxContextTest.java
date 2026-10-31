package io.quarkiverse.amazon.s3.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class S3MultipartVertxContextTest extends AbstractS3MultipartVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = configure(true);
}
