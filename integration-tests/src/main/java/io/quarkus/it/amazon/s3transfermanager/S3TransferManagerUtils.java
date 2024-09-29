package io.quarkus.it.amazon.s3transfermanager;

import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;

public class S3TransferManagerUtils {
    public static CopyRequest createCopyObjectRequest(String bucket, String keyValue, String destinationBucket,
            String destinationKeyValue) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(keyValue)
                .destinationBucket(destinationBucket)
                .destinationKey(destinationKeyValue)
                .build();

        return CopyRequest.builder()
                .copyObjectRequest(copyObjectRequest)
                .build();
    }

}
