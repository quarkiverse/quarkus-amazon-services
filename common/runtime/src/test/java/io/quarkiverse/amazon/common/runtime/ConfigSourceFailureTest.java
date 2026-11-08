package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

class ConfigSourceFailureTest {
    @ParameterizedTest
    @CsvSource({ "404,Unknown,MISSING", "400,ResourceNotFoundException,MISSING", "400,ParameterNotFound,MISSING",
            "403,Unknown,ACCESS_DENIED", "400,AccessDeniedException,ACCESS_DENIED",
            "400,KMSAccessDeniedException,ACCESS_DENIED",
            "429,ThrottlingException,TRANSIENT", "503,Unavailable,TRANSIENT", "400,ValidationException,OTHER" })
    void classifiesAwsResponses(int status, String code, ConfigSourceFailure expected) {
        var failure = AwsServiceException.builder().statusCode(status)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).message("unsafe-secret").build();
        assertEquals(expected, ConfigSourceFailure.classify(failure));
    }

    @Test
    void distinguishesEncodingAndTransportFailures() {
        assertEquals(ConfigSourceFailure.INVALID_CONTENT, ConfigSourceFailure.classify(new IllegalArgumentException("secret")));
        assertEquals(ConfigSourceFailure.INVALID_CONTENT,
                ConfigSourceFailure.classify(new UncheckedIOException(new CharacterCodingException())));
        assertEquals(ConfigSourceFailure.TRANSIENT, ConfigSourceFailure.classify(ApiCallTimeoutException.create(1)));
        assertEquals(ConfigSourceFailure.TRANSIENT,
                ConfigSourceFailure.classify(SdkClientException.create("secret", new IOException())));
        assertEquals(ConfigSourceFailure.OTHER, ConfigSourceFailure.classify(new NullPointerException("secret")));
    }

    @Test
    void logsOnlyCategoryAndCallerSuppliedOperation() {
        var records = new ArrayList<LogRecord>();
        var jul = java.util.logging.Logger.getLogger("aws-config-failure-test");
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                records.add(record);
            }

            public void flush() {
            }

            public void close() {
            }
        };
        jul.addHandler(handler);
        try {
            var unsafe = new IllegalArgumentException("unsafe-secret");
            ConfigSourceFailure.classify(unsafe).log(org.jboss.logging.Logger.getLogger(jul.getName()),
                    "Unable to load config");
            assertEquals(1, records.size());
            assertEquals("Unable to load config [INVALID_CONTENT]", records.getFirst().getMessage());
            assertNull(records.getFirst().getThrown());
        } finally {
            jul.removeHandler(handler);
        }
    }
}
