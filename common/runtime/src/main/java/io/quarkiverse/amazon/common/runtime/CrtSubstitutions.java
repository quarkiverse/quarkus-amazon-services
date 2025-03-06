package io.quarkiverse.amazon.common.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.zip.Checksum;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.CrcCloneOnMarkChecksum;
import software.amazon.awssdk.checksums.internal.CrcCombineOnMarkChecksum;
import software.amazon.awssdk.checksums.internal.SdkCrc32CChecksum;
import software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;

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

    static final class IsCrtPresent implements BooleanSupplier {
        private static final IsCrtAbsent IS_CRT_ABSENT = new IsCrtAbsent();

        @Override
        public boolean getAsBoolean() {
            return !IS_CRT_ABSENT.getAsBoolean();
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

    /**
     * aws sdk tries to create a java 9-based, then crt-based, then fallback to sdk-based checksum
     * We can safely use the java 9-based in all cases (java 17 is the minimum since quarkus 3.7)
     */
    @TargetClass(value = software.amazon.awssdk.checksums.internal.CrcChecksumProvider.class)
    @Substitute()
    static final class Target_CrcChecksumProvider {

        @Alias
        private static final String CRT_MODULE = null;
        @Alias
        private static final String CRT_CRC64NVME_PATH = null;

        @Substitute
        public static SdkChecksum crc32cImplementation() {

            return createJavaCrc32C();
        }

        /**
         * optimize the original version which use reflection to create j.u.z.CRC32C
         */
        @Substitute
        static SdkChecksum createJavaCrc32C() {
            return new CrcCombineOnMarkChecksum(new java.util.zip.CRC32C(), SdkCrc32CChecksum::combine);
        }

        @Delete
        static SdkChecksum createCrtCrc32C() {
            return null;
        }

        @Delete
        static SdkChecksum createSdkBasedCrc32C() {
            return null;
        }

        @Substitute
        @TargetElement(onlyWith = IsCrtAbsent.class)
        static SdkChecksum crc64NvmeCrtImplementation() {
            throw new RuntimeException(
                    "Could not load " + CRT_CRC64NVME_PATH + ". Add dependency on '" + CRT_MODULE
                            + "' module to enable CRC64NVME feature.");
        }

        @Substitute
        @TargetElement(name = "crc64NvmeCrtImplementation", onlyWith = IsCrtPresent.class)
        static SdkChecksum crc64NvmeCrtImplementationCrtPresent() {
            return new CrcCloneOnMarkChecksum(new software.amazon.awssdk.crt.checksums.CRC64NVME());
        }
    }

    /**
     * aws sdk tries to create a crt-based, then sdk-based checksum
     * Force the sdk-based when Crt is absent
     */
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
}
