package io.quarkiverse.amazon.common.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.amazonaws.xray.interceptors.TracingInterceptor;

import io.quarkus.deployment.index.IndexingUtil;
import software.amazon.awssdk.awscore.AwsClient;

public class InterceptorsDotNamesTest {

    private static Index awsCoreIndex;
    private static Index awsXrayRecorderIndex;

    @BeforeAll
    public static void index() throws IOException {
        awsCoreIndex = IndexingUtil.indexJar(determineJarLocation(AwsClient.class, "aws-core"));
        awsXrayRecorderIndex = IndexingUtil
                .indexJar(determineJarLocation(TracingInterceptor.class, "aws-xray-recorder-sdk-aws-sdk-v2"));
    }

    @Test
    public void testNoMissingSdkInterceptorClasses() {
        Set<String> expectedClasses = new TreeSet<>();
        for (ClassInfo clazz : awsCoreIndex.getKnownClasses()) {
            if (isInterceptor(clazz)) {
                expectedClasses.add(clazz.name().toString());
            }
        }

        // Simulate what happens when we create build items to register classes for reflection
        Set<String> actualClasses = new TreeSet<>();
        for (DotName clazz : AmazonInterceptorDotNames.SDK_INTERCEPTOR_LIST) {
            actualClasses.add(clazz.toString());
        }

        assertThat("No ExecutionInterceptor implementations found in aws-core", expectedClasses,
                not(emptyIterable()));
        assertThat(
                "Hard-coded interceptor list in AmazonInterceptorDotNames.SDK_INTERCEPTOR_LIST is missing interceptors. Update the list with the discovered implementations.",
                actualClasses, containsInAnyOrder(expectedClasses.toArray(new String[0])));
    }

    @Test
    public void testNoMissingXrayInterceptorClasses() {
        Set<String> expectedClasses = new TreeSet<>();
        for (ClassInfo clazz : awsXrayRecorderIndex.getKnownClasses()) {
            if (isInterceptor(clazz)) {
                expectedClasses.add(clazz.name().toString());
            }
        }

        // Simulate what happens when we create build items to register classes for reflection
        Set<String> actualClasses = new TreeSet<>();
        for (DotName clazz : AmazonInterceptorDotNames.XRAY_TRACING_INTERCEPTOR_LIST) {
            actualClasses.add(clazz.toString());
        }

        assertThat("No ExecutionInterceptor implementations found in aws-xray-recorder-sdk", expectedClasses,
                not(emptyIterable()));
        assertThat(
                "Hard-coded interceptor list in AmazonInterceptorDotNames.XRAY_TRACING_INTERCEPTOR_LIST is missing interceptors. Update the list with the discovered implementations.",
                actualClasses, containsInAnyOrder(expectedClasses.toArray(new String[0])));
    }

    private boolean isInterceptor(ClassInfo clazz) {
        return clazz.interfaceNames().stream().anyMatch(i -> i.equals(AmazonInterceptorDotNames.EXECUTION_INTERCEPTOR_NAME));
    }

    private static Path determineJarLocation(Class<?> classFromJar, String jarName) {
        URL url = classFromJar.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException(jarName + " JAR is not a local file? " + url);
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
