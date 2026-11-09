package io.quarkiverse.amazon.s3.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.function.Function;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Recorder
public class S3TransferManagerRecorder {

    public Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager> getTransferManager(String clientName,
            boolean crt) {
        return new Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager>() {
            @Override
            public S3TransferManager apply(SyntheticCreationalContext<S3TransferManager> context) {
                var qualifiers = new ArrayList<Annotation>();
                qualifiers.add(new S3ClientRoute.Literal(clientName, crt));
                if (!ClientUtil.isDefaultClient(clientName)) {
                    qualifiers.add(new AmazonClient.AmazonClientLiteral(clientName));
                }
                if (crt) {
                    qualifiers.add(S3Crt.Literal.INSTANCE);
                }
                return S3TransferManager.builder()
                        .s3Client(context.getInjectedReference(S3AsyncClient.class, qualifiers.toArray(Annotation[]::new)))
                        .build();
            }
        };
    }
}
