package io.quarkiverse.amazon.common.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.objectweb.asm.Opcodes.ARETURN;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.Capabilities;

class S3MultipartContextPropagationProcessorTest {
    private static final String MODEL = "Lsoftware/amazon/awssdk/services/s3/model/";
    private static final String FUTURE = "Ljava/util/concurrent/CompletableFuture;";

    @Test
    void doesNotTransformApplicationsWithoutVertx() {
        assertNull(new S3MultipartContextPropagationProcessor().transformMultipartResults(new Capabilities(Set.of())));
    }

    @Test
    void transformsTheReleasedMultipartClientIntoVerifiableBytecode() throws Exception {
        String client = S3MultipartContextPropagationProcessor.CLIENT;
        byte[] original = AsyncCompletionContextProcessorTest.read(client);
        assertNull(SdkClassTransformation.check(original, S3MultipartContextPropagationProcessor.CompletionVisitor::new));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        new ClassReader(original).accept(new S3MultipartContextPropagationProcessor.CompletionVisitor(writer), 0);
        byte[] bytes = writer.toByteArray();
        // Initializing the class verifies its bytecode
        Class<?> transformed = Class.forName(client, true, new ClassLoader(getClass().getClassLoader()) {
            {
                defineClass(client, bytes, 0, bytes.length);
            }
        });
        assertEquals(client, transformed.getName());
    }

    @Test
    void reportsChangedMultipartOperationSignatures() {
        var visitor = new S3MultipartContextPropagationProcessor.CompletionVisitor(null);
        visitor.visitMethod(1, "putObject", "()" + FUTURE, null, null);
        visitor.visitEnd();
        assertEquals("expected 3 composite operations, found 0 methods and 0 returns", visitor.mismatch());
    }

    @Test
    void supportsAdditionalReturnSites() {
        var visitor = operations();
        visitor.visitEnd();
        assertNull(visitor.mismatch());
    }

    @Test
    void reportsAdditionalOperations() {
        var visitor = operations();
        visitor.visitMethod(1, "copyObject", "(" + MODEL + "CopyObjectRequest;)" + FUTURE, null, null)
                .visitInsn(ARETURN);
        visitor.visitEnd();
        assertEquals("expected 3 composite operations, found 4 methods and 7 returns", visitor.mismatch());
    }

    private static S3MultipartContextPropagationProcessor.CompletionVisitor operations() {
        var visitor = new S3MultipartContextPropagationProcessor.CompletionVisitor(null);
        for (String[] operation : new String[][] {
                { "putObject", "(" + MODEL + "PutObjectRequest;Lsoftware/amazon/awssdk/core/async/AsyncRequestBody;)" },
                { "getObject", "(" + MODEL + "GetObjectRequest;Lsoftware/amazon/awssdk/core/async/AsyncResponseTransformer;)" },
                { "copyObject", "(" + MODEL + "CopyObjectRequest;)" } }) {
            var method = visitor.visitMethod(1, operation[0], operation[1] + FUTURE, null, null);
            method.visitCode();
            method.visitInsn(ARETURN);
            method.visitInsn(ARETURN);
            method.visitMaxs(0, 0);
        }
        return visitor;
    }
}
