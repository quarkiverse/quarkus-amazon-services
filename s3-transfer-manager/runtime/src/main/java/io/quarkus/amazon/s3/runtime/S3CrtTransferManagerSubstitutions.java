package io.quarkus.amazon.s3.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.TransferManagerConfiguration;
import software.amazon.awssdk.transfer.s3.internal.TransferManagerFactory;
import software.amazon.awssdk.utils.Logger;

@TargetClass(value = TransferManagerFactory.class, onlyWith = IsS3CrtAbsent.class)
final class Target_TransferManagerFactory {
    @Alias
    private static Logger log;

    @Substitute
    public static S3TransferManager createTransferManager(TransferManagerFactory.DefaultBuilder tmBuilder) {
        TransferManagerConfiguration transferConfiguration = resolveTransferManagerConfiguration(tmBuilder);
        S3AsyncClient s3AsyncClient = ((Target_TransferManagerFactory_DefaultBuilder) (Object) tmBuilder).s3AsyncClient;
        boolean isDefaultS3AsyncClient;

        if (s3AsyncClient == null) {
            isDefaultS3AsyncClient = true;
            s3AsyncClient = defaultS3AsyncClient().get();
        } else {
            isDefaultS3AsyncClient = false;
        }

        return (S3TransferManager) (Object) new Target_GenericS3TransferManager(transferConfiguration,
                s3AsyncClient, isDefaultS3AsyncClient);
    }

    @Alias
    private static TransferManagerConfiguration resolveTransferManagerConfiguration(
            TransferManagerFactory.DefaultBuilder tmBuilder) {
        return null;
    }

    @Substitute
    private static Supplier<S3AsyncClient> defaultS3AsyncClient() {
        return S3AsyncClient::create;
    }

    @TargetClass(value = TransferManagerFactory.DefaultBuilder.class, onlyWith = IsS3CrtAbsent.class)
    public static final class Target_TransferManagerFactory_DefaultBuilder {
        @Alias
        private S3AsyncClient s3AsyncClient;
    }
}

@TargetClass(className = "software.amazon.awssdk.transfer.s3.internal.GenericS3TransferManager", onlyWith = IsS3CrtAbsent.class)
final class Target_GenericS3TransferManager implements S3TransferManager {

    @Alias
    Target_GenericS3TransferManager(TransferManagerConfiguration transferConfiguration,
            S3AsyncClient s3AsyncClient,
            boolean isDefaultS3AsyncClient) {
    }

    @Alias
    @Override
    public void close() {
    }
}

final class IsS3CrtAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return !Arrays.asList(Package.getPackages()).stream()
                .map(p -> p.getName())
                .anyMatch(p -> p.equals("software.amazon.awssdk.crt.s3"));
    }
}
