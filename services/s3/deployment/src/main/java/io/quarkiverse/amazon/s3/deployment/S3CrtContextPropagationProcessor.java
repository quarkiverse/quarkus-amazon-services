package io.quarkiverse.amazon.s3.deployment;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.Arrays;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkiverse.amazon.common.deployment.CompositeOperationVisitor;
import io.quarkiverse.amazon.common.deployment.SdkClassTransformation;
import io.quarkiverse.amazon.s3.runtime.S3CrtContextAwareBuilder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

/**
 * Exposes the SDK builder's existing package-private interceptor registration through an extension-owned interface.
 * The original builder contains:
 *
 * <pre>{@code
 * class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
 *     DefaultS3CrtClientBuilder addExecutionInterceptor(ExecutionInterceptor interceptor) { ... }
 * }
 * }</pre>
 *
 * The transformation retains that method and adds:
 *
 * <pre>{@code
 * class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder, S3CrtContextAwareBuilder {
 *     public void addContextInterceptor(ExecutionInterceptor interceptor) {
 *         addExecutionInterceptor(interceptor);
 *     }
 * }
 * }</pre>
 *
 * The recorder can therefore install the capturing interceptor without reflection or replacing the SDK's interceptor list.
 * If the SDK changed the builder, the transformation is skipped and the recorder does not install the interceptor.
 * <p>
 * The client's {@code copyObject} is also a composite operation, assembled from {@code headObject} and multipart copy calls,
 * see {@link CompositeOperationVisitor}.
 */
public class S3CrtContextPropagationProcessor {

    static final String CLIENT = "software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient";
    static final String BUILDER = "software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient$DefaultS3CrtClientBuilder";
    private static final String INTERCEPTOR = "Lsoftware/amazon/awssdk/core/interceptor/ExecutionInterceptor;";

    @BuildStep(onlyIf = S3CrtProcessor.IsAmazonCrtS3ClientPresent.class)
    BytecodeTransformerBuildItem transformCrtBuilder(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.VERTX)) {
            return null;
        }
        return SdkClassTransformation.transform(BUILDER, BuilderVisitor::new);
    }

    @BuildStep(onlyIf = S3CrtProcessor.IsAmazonCrtS3ClientPresent.class)
    BytecodeTransformerBuildItem transformCrtCopy(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.VERTX)) {
            return null;
        }
        return SdkClassTransformation.transform(CLIENT, CopyVisitor::new);
    }

    static final class CopyVisitor extends CompositeOperationVisitor {

        CopyVisitor(ClassVisitor visitor) {
            super(visitor, 1, (name, descriptor) -> name.equals("copyObject") && descriptor.equals(
                    "(Lsoftware/amazon/awssdk/services/s3/model/CopyObjectRequest;)Ljava/util/concurrent/CompletableFuture;"));
        }
    }

    static final class BuilderVisitor extends SdkClassTransformation.CheckedVisitor {
        private int interceptorMethods;

        BuilderVisitor(ClassVisitor visitor) {
            super(visitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            String[] extended = Arrays.copyOf(interfaces, interfaces.length + 1);
            extended[interfaces.length] = Type.getInternalName(S3CrtContextAwareBuilder.class);
            super.visit(version, access, name, signature, superName, extended);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals("addExecutionInterceptor") && descriptor.equals("(" + INTERCEPTOR + ")L"
                    + BUILDER.replace('.', '/') + ";")) {
                interceptorMethods++;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        protected String mismatch() {
            return interceptorMethods == 1 ? null : "expected one addExecutionInterceptor method, found " + interceptorMethods;
        }

        @Override
        public void visitEnd() {
            // Reuse the SDK's existing interceptor list and registration logic, without reflection or new SDK API.
            MethodVisitor method = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "addContextInterceptor",
                    "(" + INTERCEPTOR + ")V", null, null);
            if (method != null) {
                method.visitCode();
                method.visitVarInsn(ALOAD, 0);
                method.visitVarInsn(ALOAD, 1);
                method.visitMethodInsn(INVOKEVIRTUAL, BUILDER.replace('.', '/'), "addExecutionInterceptor",
                        "(" + INTERCEPTOR + ")L" + BUILDER.replace('.', '/') + ";", false);
                method.visitInsn(POP);
                method.visitInsn(RETURN);
                method.visitMaxs(2, 2);
                method.visitEnd();
            }
            super.visitEnd();
        }
    }
}
