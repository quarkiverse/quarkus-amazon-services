package io.quarkiverse.amazon.common.deployment;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

/**
 * Covers the composite futures assembled above the HTTP handler by S3's Java multipart client, see
 * {@link CompositeOperationVisitor}.
 * <p>
 * Registration lives in common deployment so generic SDK clients receive the same transformation without a dedicated S3
 * extension.
 */
public class S3MultipartContextPropagationProcessor {
    static final String CLIENT = "software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient";
    private static final String MODEL = "Lsoftware/amazon/awssdk/services/s3/model/";
    private static final String FUTURE = "Ljava/util/concurrent/CompletableFuture;";

    @BuildStep
    BytecodeTransformerBuildItem transformMultipartResults(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.VERTX)) {
            return null;
        }
        return SdkClassTransformation.transform(CLIENT, CompletionVisitor::new);
    }

    static final class CompletionVisitor extends CompositeOperationVisitor {

        CompletionVisitor(ClassVisitor visitor) {
            super(visitor, 3, (name, descriptor) -> switch (name) {
                case "putObject" -> descriptor.equals("(" + MODEL + "PutObjectRequest;"
                        + "Lsoftware/amazon/awssdk/core/async/AsyncRequestBody;)" + FUTURE);
                case "getObject" -> descriptor.equals("(" + MODEL + "GetObjectRequest;"
                        + "Lsoftware/amazon/awssdk/core/async/AsyncResponseTransformer;)" + FUTURE);
                case "copyObject" -> descriptor.equals("(" + MODEL + "CopyObjectRequest;)" + FUTURE);
                default -> false;
            });
        }
    }
}
