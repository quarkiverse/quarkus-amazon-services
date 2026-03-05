package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

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

import io.quarkus.deployment.index.IndexingUtil;
import software.amazon.awssdk.enhanced.dynamodb.extensions.AtomicCounterExtension;

public class DynamoDbEnhancedClientExtensionsDotNamesTest {

    private static Index dynamodbEnhancedIndex;
    private static final DotName SDK_INTERNAL_API_ANNOTATION = DotName
            .createSimple("software.amazon.awssdk.annotations.SdkInternalApi");

    @BeforeAll
    public static void index() throws IOException {
        dynamodbEnhancedIndex = IndexingUtil.indexJar(determineJarLocation(AtomicCounterExtension.class, "dynamodb-enhanced"));
    }

    @Test
    public void testNoMissingClientExtensionClasses() {
        Set<String> expectedClasses = new TreeSet<>();
        for (ClassInfo clazz : dynamodbEnhancedIndex.getKnownClasses()) {
            if (isClientExtension(clazz)) {
                expectedClasses.add(clazz.name().toString());
            }
        }

        // Simulate what happens when we discover client extensions
        Set<String> actualClasses = new TreeSet<>();
        for (DotName extension : DynamoDbEnhancedClientExtensionsDotNames.WELL_KNOWN_CLIENT_EXTENSIONS_LIST) {
            actualClasses.add(extension.toString());
        }

        assertThat("No DynamoDbEnhancedClientExtension implementations found in dynamodb-enhanced JAR", expectedClasses,
                not(emptyIterable()));
        assertThat(
                "Hard-coded client extensions list in DynamoDbEnhancedClientExtensionsDotNames.WELL_KNOWN_CLIENT_EXTENSIONS_LIST is missing extensions. Update the list with the discovered implementations.",
                actualClasses, containsInAnyOrder(expectedClasses.toArray(new String[0])));

    }

    private boolean isClientExtension(ClassInfo clazz) {
        // Exclude internal APIs marked with @SdkInternalApi annotation
        if (clazz.hasAnnotation(SDK_INTERNAL_API_ANNOTATION)) {
            return false;
        }
        return clazz.interfaceNames().stream()
                .anyMatch(i -> i.equals(DotNames.DYNAMODB_ENHANCED_CLIENT_EXTENSION_NAME));
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
