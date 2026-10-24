package io.quarkiverse.amazon.fake.deployment;

import java.lang.reflect.Field;

import io.quarkus.arc.ClientProxy;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Exposes the configuration the SDK keeps private in the clients built by the extension,
 * so that tests can assert what is actually configured instead of only checking that the application starts.
 */
public final class FakeClientInspector {

    private FakeClientInspector() {
    }

    /**
     * @return the name of the HTTP client used by the sync client (e.g. {@code UrlConnection}, {@code Apache})
     */
    public static String httpClientName(FakeClient client) {
        return configuration(client).option(SdkClientOption.SYNC_HTTP_CLIENT).clientName();
    }

    /**
     * @return the name of the HTTP client used by the async client (e.g. {@code NettyNio}, {@code AwsCommonRuntime})
     */
    public static String httpClientName(FakeAsyncClient client) {
        return configuration(client).option(SdkClientOption.ASYNC_HTTP_CLIENT).clientName();
    }

    /**
     * @return the value of an option (e.g. {@code SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER}) set on the
     *         builder of the HTTP client used by the sync client
     */
    public static <T> T httpClientOption(FakeClient client, AttributeMap.Key<T> option) {
        return standardOptions(configuration(client).option(SdkClientOption.CONFIGURED_SYNC_HTTP_CLIENT_BUILDER)).get(option);
    }

    /**
     * @return the value of an option (e.g. {@code SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER}) set on the
     *         builder of the HTTP client used by the async client
     */
    public static <T> T httpClientOption(FakeAsyncClient client, AttributeMap.Key<T> option) {
        return standardOptions(configuration(client).option(SdkClientOption.CONFIGURED_ASYNC_HTTP_CLIENT_BUILDER))
                .get(option);
    }

    /**
     * @return the configuration the client has been built with
     */
    public static SdkClientConfiguration configuration(SdkClient client) {
        SdkClient unwrapped = ClientProxy.unwrap(client);
        try {
            Field field = unwrapped.getClass().getDeclaredField("clientConfiguration");
            field.setAccessible(true);
            return (SdkClientConfiguration) field.get(unwrapped);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read the configuration of " + unwrapped.getClass(), e);
        }
    }

    /**
     * The options of the SDK HTTP client builders are kept in a private {@code standardOptions} field.
     */
    private static AttributeMap.Builder standardOptions(Object httpClientBuilder) {
        for (Class<?> type = httpClientBuilder.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("standardOptions");
                field.setAccessible(true);
                return (AttributeMap.Builder) field.get(httpClientBuilder);
            } catch (NoSuchFieldException e) {
                // look in the parent class
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to read the options of " + httpClientBuilder.getClass(), e);
            }
        }
        throw new IllegalStateException("No options found in " + httpClientBuilder.getClass());
    }
}
