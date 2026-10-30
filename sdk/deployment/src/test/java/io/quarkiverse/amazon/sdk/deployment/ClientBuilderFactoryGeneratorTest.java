package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.EcrClientBuilder;

/**
 * Verifies generated sync and async factories return a fresh builder of the expected type on each call.
 */
class ClientBuilderFactoryGeneratorTest {
    static Stream<Arguments> clients() {
        return Stream.of(Arguments.of(EcrClient.class, EcrClientBuilder.class),
                Arguments.of(EcrAsyncClient.class, EcrAsyncClientBuilder.class));
    }

    @ParameterizedTest
    @MethodSource("clients")
    void generatedFactoryCreatesAFreshBuilderOnEachCall(Class<?> client, Class<?> builder) throws Exception {
        List<GeneratedClassBuildItem> generated = new ArrayList<>();
        String factory = ClientBuilderFactoryGenerator.generate(DotName.createSimple(client.getName()),
                DotName.createSimple(builder.getName()), generated::add);
        assertEquals(1, generated.size());
        Class<?> factoryClass = new ClassLoader(getClass().getClassLoader()) {
            Class<?> defineFactory() {
                byte[] bytes = generated.getFirst().getClassData();
                return defineClass(factory, bytes, 0, bytes.length);
            }
        }.defineFactory();
        Supplier<?> supplier = (Supplier<?>) factoryClass.getConstructor().newInstance();
        Object first = supplier.get();
        Object second = supplier.get();
        assertInstanceOf(builder, first);
        assertInstanceOf(builder, second);
        assertNotSame(first, second);
    }
}
