package io.quarkiverse.amazon.common.deployment;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkiverse.amazon.common.runtime.AsyncCompletionContext;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

/**
 * Transforms both async execute overloads, including streaming responses, at their final-result boundary.
 * The original SDK code is equivalent to:
 *
 * <pre>{@code
 * return measureApiCall(params, () -> executeRequest(params));
 * }</pre>
 *
 * The transformed code is equivalent to:
 *
 * <pre>{@code
 * return AsyncCompletionContext.complete(
 *         measureApiCall(params, () -> executeRequest(params)), params.executionAttributes());
 * }</pre>
 *
 * Initial interceptors populate these attributes before execute returns. Wrapping the final future also covers
 * timeouts and failures before the HTTP attempt. The HTTP pipeline and its configured attempt-completion executor
 * remain unchanged; generated service completion callbacks can still execute on the selected context.
 * <p>
 * Every API call goes through this method, including the calls made by SDK helpers. Those are not captured, see
 * {@code SdkInternalCalls}.
 */
public class AsyncCompletionContextProcessor {
    static final String HANDLER = "software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler";
    private static final String PARAMS = "software/amazon/awssdk/core/client/handler/ClientExecutionParams";
    private static final String FUTURE = "Ljava/util/concurrent/CompletableFuture;";
    private static final String ATTRIBUTES = "Lsoftware/amazon/awssdk/core/interceptor/ExecutionAttributes;";

    @BuildStep
    BytecodeTransformerBuildItem transformCompletionExecutor(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.VERTX)) {
            return null;
        }
        return SdkClassTransformation.transform(HANDLER, CompletionVisitor::new);
    }

    static final class CompletionVisitor extends SdkClassTransformation.FinalResultVisitor {

        CompletionVisitor(ClassVisitor visitor) {
            super(visitor, 2, "execute overloads");
        }

        @Override
        protected boolean matches(String name, String descriptor) {
            return name.equals("execute") && (descriptor.equals("(L" + PARAMS + ";)" + FUTURE)
                    || descriptor.equals(
                            "(L" + PARAMS + ";Lsoftware/amazon/awssdk/core/async/AsyncResponseTransformer;)" + FUTURE));
        }

        @Override
        protected void wrap(MethodVisitor visitor) {
            visitor.visitVarInsn(ALOAD, 1);
            visitor.visitMethodInsn(INVOKEVIRTUAL, PARAMS, "executionAttributes", "()" + ATTRIBUTES, false);
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(AsyncCompletionContext.class), "complete",
                    "(" + FUTURE + ATTRIBUTES + ")" + FUTURE, false);
        }
    }
}
