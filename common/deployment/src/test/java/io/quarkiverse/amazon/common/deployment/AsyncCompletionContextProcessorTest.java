package io.quarkiverse.amazon.common.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.objectweb.asm.Opcodes.ASM9;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.deployment.Capabilities;

class AsyncCompletionContextProcessorTest {

    @Test
    void doesNotTransformApplicationsWithoutVertx() {
        assertNull(new AsyncCompletionContextProcessor().transformCompletionExecutor(new Capabilities(Set.of())));
    }

    @Test
    void transformsBothFinalResultBoundariesInTheReleasedSdk() throws Exception {
        byte[] original = read(AsyncCompletionContextProcessor.HANDLER);
        assertNull(SdkClassTransformation.check(original, AsyncCompletionContextProcessor.CompletionVisitor::new));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(original).accept(new AsyncCompletionContextProcessor.CompletionVisitor(writer), 0);
        AtomicInteger calls = new AtomicInteger();
        new ClassReader(writer.toByteArray()).accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return new MethodVisitor(ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (owner.equals("io/quarkiverse/amazon/common/runtime/AsyncCompletionContext")
                                && name.equals("complete")) {
                            calls.incrementAndGet();
                        }
                    }
                };
            }
        }, 0);
        assertEquals(2, calls.get());
    }

    @Test
    void reportsAChangedSdkCompletionSiteInsteadOfFailingTheBuild() throws Exception {
        String mismatch = SdkClassTransformation.check(read(Object.class.getName()),
                AsyncCompletionContextProcessor.CompletionVisitor::new);
        assertNotNull(mismatch);
        assertEquals("expected 2 execute overloads, found 0 methods and 0 returns", mismatch);
    }

    @Test
    void skipsUnsupportedOrMissingClasses() {
        assertNull(SdkClassTransformation.transform(Object.class.getName(),
                AsyncCompletionContextProcessor.CompletionVisitor::new));
        assertNull(SdkClassTransformation.transform("software.amazon.awssdk.Missing",
                AsyncCompletionContextProcessor.CompletionVisitor::new));
        assertNotNull(SdkClassTransformation.transform(AsyncCompletionContextProcessor.HANDLER,
                AsyncCompletionContextProcessor.CompletionVisitor::new));
    }

    static byte[] read(String className) throws Exception {
        try (InputStream stream = AsyncCompletionContextProcessorTest.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            return stream.readAllBytes();
        }
    }
}
