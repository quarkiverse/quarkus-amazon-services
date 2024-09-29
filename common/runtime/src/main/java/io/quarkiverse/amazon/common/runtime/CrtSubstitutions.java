package io.quarkiverse.amazon.common.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.zip.Checksum;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.scheme.DefaultAwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

public class CrtSubstitutions {
    static final String SOFTWARE_AMAZON_AWSSDK_CRT_PACKAGE = "software.amazon.awssdk.crt";
    /*
     * software.amazon.awssdk:http-auth-aws-crt dependens on
     * software.amazon.awssdk.crt:aws-crt, both will be present
     */
    static final String SOFTWARE_AMAZON_AWSSDK_HTTP_AUTH_AWS_CRT_PACKAGE = "software.amazon.awssdk.http.auth.aws.crt";

    static final class IsCrtAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_CRT_PACKAGE));
        }
    }

    static final class IsHttpAuthAwsCrtAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_HTTP_AUTH_AWS_CRT_PACKAGE));
        }
    }

    @TargetClass(value = software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32CChecksum.class, onlyWith = IsCrtAbsent.class)
    static final class Target_SignerCrc32CChecksum {

        @Alias
        private Checksum crc32c;

        @Substitute
        public Target_SignerCrc32CChecksum() {
            crc32c = software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkCrc32CChecksum.create();
        }

        @Substitute
        private Checksum cloneChecksum(Checksum checksum) {
            return (Checksum) ((software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkCrc32CChecksum) checksum)
                    .clone();
        }
    }

    @TargetClass(value = software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32Checksum.class, onlyWith = IsCrtAbsent.class)
    static final class Target_SignerCrc32Checksum {

        @Alias
        private Checksum crc32;

        @Substitute
        public Target_SignerCrc32Checksum() {
            crc32 = software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkCrc32Checksum.create();
        }

        @Substitute
        private Checksum cloneChecksum(Checksum checksum) {

            return (Checksum) ((software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkCrc32Checksum) checksum)
                    .clone();
        }
    }

    @TargetClass(value = software.amazon.awssdk.core.checksums.Crc32Checksum.class, onlyWith = IsCrtAbsent.class)
    static final class Target_Crc32Checksum {
        @Alias
        private Checksum crc32;

        @Substitute
        public Target_Crc32Checksum() {
            crc32 = software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32.create();
        }

        @Substitute
        private Checksum cloneChecksum(Checksum checksum) {

            return (Checksum) ((software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32) checksum).clone();
        }
    }

    @TargetClass(value = DefaultAwsCrtV4aHttpSigner.class, onlyWith = IsHttpAuthAwsCrtAbsent.class)
    @Delete
    final class Delete_DefaultAwsCrtV4aHttpSigner {
    }

    @TargetClass(value = AwsV4aHttpSigner.class, onlyWith = IsHttpAuthAwsCrtAbsent.class)
    public interface Target_AwsV4aHttpSigner {

        @Substitute
        static AwsV4aHttpSigner create() {
            throw new RuntimeException(
                    "You must add a dependency on the 'software.amazon.awssdk:http-auth-aws-crt' module to enable the CRT-V4a signing feature");
        }
    }

    @TargetClass(value = DefaultAwsV4aAuthScheme.class, onlyWith = IsCrtAbsent.class)
    @Delete
    final class Target_DefaultAwsV4aAuthScheme {
    }

    @TargetClass(value = AwsV4aAuthScheme.class, onlyWith = IsHttpAuthAwsCrtAbsent.class)
    public interface Target_AwsV4aAuthScheme {

        @Substitute
        static AwsV4aAuthScheme create() {
            return HttpAuthAwsCrtAbsentAwsV4aAuthScheme.DEFAULT;
        }
    }

    /**
     * Provide a substitution for DefaultAwsV4aAuthScheme that throws an exception
     * at runtime without requiring the optional dependency http-auth-aws-crt.
     */
    static class HttpAuthAwsCrtAbsentAwsV4aAuthScheme implements AwsV4aAuthScheme {
        private static final HttpAuthAwsCrtAbsentAwsV4aAuthScheme DEFAULT = new HttpAuthAwsCrtAbsentAwsV4aAuthScheme();

        public static HttpAuthAwsCrtAbsentAwsV4aAuthScheme create() {
            return DEFAULT;
        }

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public AwsV4aHttpSigner signer() {
            return AwsV4aHttpSigner.create(); // will throw
        }
    }
}
