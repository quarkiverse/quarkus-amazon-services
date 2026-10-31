package io.quarkiverse.amazon.common.test;

import io.quarkus.test.QuarkusExtensionTest;

/** Common application setup for the dedicated and generic extension fixtures. */
public final class AsyncClientContextTestSupport {
    private AsyncClientContextTestSupport() {
    }

    public static QuarkusExtensionTest configure(String prefix, String awsSuffix,
            Class<? extends AbstractAsyncClientContextResource> resource, Class<?>... fixtures) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClasses(resource, AbstractAsyncClientContextResource.class,
                        CallerContextProbeInterceptor.class, AsyncClientContextTestSupport.class,
                        AbstractAsyncClientVertxContextTest.class, AbstractAsyncClientVertxContextDisabledTest.class)
                        .addClasses(fixtures))
                .overrideConfigKey("quarkus.devservices.enabled", "false")
                .overrideConfigKey(prefix + ".endpoint-override", "http://localhost:1")
                .overrideConfigKey(prefix + awsSuffix + ".region", "us-east-1")
                .overrideConfigKey(prefix + awsSuffix + ".credentials.type", "static")
                .overrideConfigKey(prefix + awsSuffix + ".credentials.static-provider.access-key-id", "test-key")
                .overrideConfigKey(prefix + awsSuffix + ".credentials.static-provider.secret-access-key", "test-secret")
                .overrideConfigKey(prefix + ".interceptors", CallerContextProbeInterceptor.class.getName());
    }
}
