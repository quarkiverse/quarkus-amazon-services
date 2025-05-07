/*
 * Part of this file is derived from the AWS SDK for Java 2.x, which is licensed under the Apache License, Version 2.0.
 * https://github.com/aws/aws-sdk-java-v2/blob/40b8869c5a24ceae5c41e520ab43e7fbfd06187a/codegen/src/test/java/software/amazon/awssdk/codegen/poet/PoetMatchers.java
 */
package io.quarkiverse.amazon.codegen.poet;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static software.amazon.awssdk.codegen.poet.PoetUtils.buildJavaFile;

import java.io.IOException;
import java.io.InputStream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import software.amazon.awssdk.codegen.emitters.CodeTransformer;
import software.amazon.awssdk.codegen.emitters.JavaCodeFormatter;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

public class PoetMatchers {
    private static final CodeTransformer processor = CodeTransformer.chain(new JavaCodeFormatter());
    private static final CodeTransformer formattingDisabledProcessor = CodeTransformer.chain();

    public static Matcher<ClassSpec> generatesTo(String expectedTestFile) {
        return new TypeSafeMatcher<ClassSpec>() {
            @Override
            protected boolean matchesSafely(ClassSpec spec) {
                String expectedClass = getExpectedClass(spec, expectedTestFile, false);
                String actualClass = generateClass(spec);

                return equalToIgnoringWhiteSpace(expectedClass).matches(actualClass);
            }

            @Override
            public void describeMismatchSafely(ClassSpec spec, Description mismatchDescription) {
                String expectedClass = getExpectedClass(spec, expectedTestFile, true);
                String actualClass = generateClass(spec);

                mismatchDescription.appendText("Expected:\n").appendText(expectedClass)
                        .appendText("\nActual:\n").appendText(actualClass);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ClassSpec should generate to " + expectedTestFile);
            }
        };
    }

    private static String getExpectedClass(ClassSpec spec, String testFile, boolean disableFormatting) {
        try {
            InputStream resource = spec.getClass().getResourceAsStream(testFile);
            Validate.notNull(resource, "Failed to load test file " + testFile + " with " + spec.getClass());
            if (disableFormatting) {
                return formattingDisabledProcessor.apply(IoUtils.toUtf8String(resource));
            }
            return processor.apply(IoUtils.toUtf8String(resource));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateClass(ClassSpec spec) {
        StringBuilder output = new StringBuilder();
        try {
            buildJavaFile(spec).writeTo(output);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate class", e);
        }
        return processor.apply(output.toString());
    }
}
