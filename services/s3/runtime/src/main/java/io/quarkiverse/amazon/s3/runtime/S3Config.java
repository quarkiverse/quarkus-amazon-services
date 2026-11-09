package io.quarkiverse.amazon.s3.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.s3")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface S3Config extends HasAmazonClientRuntimeConfig {

    /**
     * S3-specific settings. Named clients inherit each absent setting from the default client.
     */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(ClientUtil.DEFAULT_CLIENT_NAME)
    Map<String, S3ClientConfig> s3Clients();

    default S3ClientConfig s3ConfigFor(String clientName) {
        return s3Clients().getOrDefault(clientName, s3Clients().get(ClientUtil.DEFAULT_CLIENT_NAME));
    }

    @ConfigGroup
    interface S3ClientConfig {

        /**
         * Enable using the accelerate endpoint when accessing S3.
         *
         * <p>
         * Accelerate endpoints allow faster transfer of objects by using Amazon CloudFront's globally distributed edge
         * locations.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> accelerateMode();

        /**
         * Enable doing a validation of the checksum of an object stored in S3.
         */
        @ConfigDocDefault("true")
        Optional<Boolean> checksumValidation();

        /**
         * Enable using chunked encoding when signing the request payload for
         * {@link software.amazon.awssdk.services.s3.model.PutObjectRequest}
         * and {@link software.amazon.awssdk.services.s3.model.UploadPartRequest}.
         */
        @ConfigDocDefault("true")
        Optional<Boolean> chunkedEncoding();

        /**
         * Enable dualstack mode for accessing S3. If you want to use IPv6 when accessing S3, dualstack
         * must be enabled.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> dualstack();

        /**
         * Enable using path style access for accessing S3 objects instead of DNS style access.
         * DNS style access is preferred as it will result in better load balancing when accessing S3.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> pathStyleAccess();

        /**
         * Enable cross-region call to the region specified in the S3 resource ARN different than the region
         * the client was configured with.
         * If this flag is not set to 'true', the cross-region call will throw an exception.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> useArnRegionEnabled();

        /**
         * Define the profile name that should be consulted to determine the default value of {@link #useArnRegionEnabled}.
         * This is not used, if the {@link #useArnRegionEnabled} is configured to 'true'.
         * <p>
         * If not specified, the value in `AWS_PROFILE` environment variable or `aws.profile` system property is used and
         * defaults to `default` name.
         */
        Optional<String> profileName();

        /**
         * Multipart transfer configuration for the standard S3 asynchronous client
         */
        @ConfigDocSection
        MultipartConfig multipart();

        /** AWS CRT-based S3 client configuration. */
        @ConfigDocSection
        S3CrtConfig crtClient();
    }

    /**
     * Sync HTTP transport configurations
     */
    @ConfigDocSection
    SyncHttpClientConfig syncClient();

    /**
     * Async HTTP transport configurations
     */
    @ConfigDocSection
    AsyncHttpClientConfig asyncClient();

    @ConfigGroup
    interface MultipartConfig {

        /**
         * Enable automatic multipart transfers for the standard S3 asynchronous client, with either Netty or the AWS CRT HTTP
         * transport. Includes direct calls and transfers through the Transfer Manager.
         * Named clients inherit absent settings from the default client.
         * Does not apply to the synchronous client or the dedicated {@code @S3Crt} client.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> enabled();

        /**
         * Size above which a transfer uses multipart when enabled. Must be greater than zero.
         * If not set, the AWS SDK default is used.
         */
        @ConfigDocDefault("Same as minimum-part-size-in-bytes")
        Optional<MemorySize> thresholdInBytes();

        /**
         * Minimum part size for multipart transfers when enabled. Must be greater than zero.
         * If not set, the AWS SDK default is used.
         */
        @ConfigDocDefault("8M")
        Optional<MemorySize> minimumPartSizeInBytes();
    }
}
