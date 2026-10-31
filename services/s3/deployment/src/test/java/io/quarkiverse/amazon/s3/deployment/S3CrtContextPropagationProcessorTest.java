package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkiverse.amazon.s3.runtime.S3CrtContextAwareBuilder;
import io.quarkus.deployment.Capabilities;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

class S3CrtContextPropagationProcessorTest {

    @Test
    void doesNotTransformApplicationsWithoutVertx() {
        assertNull(new S3CrtContextPropagationProcessor().transformCrtBuilder(new Capabilities(Set.of())));
    }

    @Test
    void exposesTheExistingInterceptorRegistrationWithoutReflection() throws Exception {
        S3CrtContextAwareBuilder builder = assertInstanceOf(S3CrtContextAwareBuilder.class,
                TransformedClasses.transform(S3CrtContextPropagationProcessor.BUILDER, 0,
                        S3CrtContextPropagationProcessor.BuilderVisitor::new).getConstructor().newInstance());
        builder.addContextInterceptor(new ExecutionInterceptor() {
        });
    }

    @Test
    void reportsAChangedSdkInterceptorRegistrationInsteadOfFailingTheBuild() {
        var visitor = new S3CrtContextPropagationProcessor.BuilderVisitor(null);
        visitor.visit(61, 0, "Builder", null, "java/lang/Object", new String[0]);
        visitor.visitEnd();
        assertEquals("expected one addExecutionInterceptor method, found 0", visitor.mismatch());
    }

    /** Defines a transformed SDK class in a child class loader, which verifies its bytecode. */
    static final class TransformedClasses {
        static Class<?> transform(String className, int writerFlags,
                java.util.function.Function<ClassVisitor, ? extends ClassVisitor> visitor) throws Exception {
            ClassWriter writer = new ClassWriter(writerFlags);
            try (InputStream stream = TransformedClasses.class.getClassLoader()
                    .getResourceAsStream(className.replace('.', '/') + ".class")) {
                new ClassReader(stream).accept(visitor.apply(writer), 0);
            }
            byte[] bytes = writer.toByteArray();
            return Class.forName(className, true, new ClassLoader(TransformedClasses.class.getClassLoader()) {
                {
                    defineClass(className, bytes, 0, bytes.length);
                }
            });
        }
    }
}
