package io.quarkus.amazon.s3.runtime;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Recorder
public class S3CrtTransferManagerRecorder {

    public Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager> getS3CrtTransferManager() {
        return new Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager>() {
            @Override
            public S3TransferManager apply(SyntheticCreationalContext<S3TransferManager> context) {
                return S3TransferManager.builder()
                        .s3Client(context.getInjectedReference(S3AsyncClient.class, S3Crt.Literal.INSTANCE)).build();
            }
        };
    }
}
