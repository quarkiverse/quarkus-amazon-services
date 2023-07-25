package io.quarkus.amazon.s3.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

public class S3CrtSubstitutions {

    static final String SOFTWARE_AMAZON_AWSSDK_CRT_PACKAGE = "software.amazon.awssdk.crt";

    static final class IsCrtAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_CRT_PACKAGE));
        }
    }
}

@TargetClass(value = DefaultS3CrtAsyncClient.class, onlyWith = S3CrtSubstitutions.IsCrtAbsent.class)
@Delete
final class Delete_DefaultS3CrtAsyncClient {
}

@TargetClass(value = S3CrtAsyncClient.class, onlyWith = S3CrtSubstitutions.IsCrtAbsent.class)
@Delete
final class Delete_S3CrtAsyncClient {
}
