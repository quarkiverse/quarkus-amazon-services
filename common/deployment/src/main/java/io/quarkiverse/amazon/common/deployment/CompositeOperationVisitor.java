package io.quarkiverse.amazon.common.deployment;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.function.BiPredicate;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkiverse.amazon.common.runtime.SdkInternalCalls;

/**
 * Transforms the operations of an SDK client that assemble their future from other calls of the client, e.g. S3's
 * multipart operations. The original methods are equivalent to:
 *
 * <pre>{@code
 * public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
 *     return helper.operation(request, body);
 * }
 * }</pre>
 *
 * The transformed methods are equivalent to:
 *
 * <pre>{@code
 * public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
 *     SdkInternalCalls.enterComposite(this);
 *     try {
 *         return SdkInternalCalls.exitComposite(helper.operation(request, body));
 *     } catch (Throwable t) {
 *         SdkInternalCalls.abortComposite();
 *         throw t;
 *     }
 * }
 * }</pre>
 *
 * The caller's context is captured once, on entry, for the composite result. The calls the helper makes synchronously,
 * e.g. the creation of a multipart upload, are internal.
 */
public class CompositeOperationVisitor extends SdkClassTransformation.FinalResultVisitor {
    private static final String CALLS = Type.getInternalName(SdkInternalCalls.class);
    private static final String FUTURE = "Ljava/util/concurrent/CompletableFuture;";

    private final BiPredicate<String, String> operations;
    private Label start;

    /**
     * @param operations matches the name and descriptor of the composite operations
     */
    public CompositeOperationVisitor(ClassVisitor visitor, int expectedOperations, BiPredicate<String, String> operations) {
        super(visitor, expectedOperations, "composite operations");
        this.operations = operations;
    }

    @Override
    protected boolean matches(String name, String descriptor) {
        return operations.test(name, descriptor);
    }

    @Override
    protected void enter(MethodVisitor visitor) {
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESTATIC, CALLS, "enterComposite", "(Lsoftware/amazon/awssdk/core/SdkClient;)V", false);
        start = new Label();
        visitor.visitLabel(start);
    }

    @Override
    protected void wrap(MethodVisitor visitor) {
        visitor.visitMethodInsn(INVOKESTATIC, CALLS, "exitComposite", "(" + FUTURE + ")" + FUTURE, false);
    }

    @Override
    protected void exit(MethodVisitor visitor) {
        Label handler = new Label();
        visitor.visitLabel(handler);
        visitor.visitMethodInsn(INVOKESTATIC, CALLS, "abortComposite", "()V", false);
        visitor.visitInsn(ATHROW);
        // Visited last, so the handlers of the original method keep their precedence
        visitor.visitTryCatchBlock(start, handler, handler, null);
    }
}
