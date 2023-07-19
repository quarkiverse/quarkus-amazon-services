package io.quarkus.amazon.s3.runtime;

import java.util.Optional;

import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.s3")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface S3Config {

    /**
     * Enable using the accelerate endpoint when accessing S3.
     *
     * <p>
     * Accelerate endpoints allow faster transfer of objects by using Amazon CloudFront's globally distributed edge locations.
     */
    @WithDefault("false")
    boolean accelerateMode();

    /**
     * Enable doing a validation of the checksum of an object stored in S3.
     */
    @WithDefault("true")
    boolean checksumValidation();

    /**
     * Enable using chunked encoding when signing the request payload for
     * {@link software.amazon.awssdk.services.s3.model.PutObjectRequest}
     * and {@link software.amazon.awssdk.services.s3.model.UploadPartRequest}.
     */
    @WithDefault("true")
    boolean chunkedEncoding();

    /**
     * Enable dualstack mode for accessing S3. If you want to use IPv6 when accessing S3, dualstack
     * must be enabled.
     */
    @WithDefault("false")
    boolean dualstack();

    /**
     * Enable using path style access for accessing S3 objects instead of DNS style access.
     * DNS style access is preferred as it will result in better load balancing when accessing S3.
     */
    @WithDefault("false")
    boolean pathStyleAccess();

    /**
     * Enable cross-region call to the region specified in the S3 resource ARN different than the region
     * the client was configured with.
     * If this flag is not set to 'true', the cross-region call will throw an exception.
     */
    @WithDefault("false")
    boolean useArnRegionEnabled();

    /**
     * Define the profile name that should be consulted to determine the default value of {@link #useArnRegionEnabled}.
     * This is not used, if the {@link #useArnRegionEnabled} is configured to 'true'.
     * <p>
     * If not specified, the value in `AWS_PROFILE` environment variable or `aws.profile` system property is used and
     * defaults to `default` name.
     */
    Optional<String> profileName();

    /**
     * AWS SDK client configurations
     */
    @WithParentName
    @ConfigDocSection
    SdkConfig sdk();

    /**
     * AWS services configurations
     */
    @ConfigDocSection
    AwsConfig aws();

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
}
