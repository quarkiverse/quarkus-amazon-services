package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.amazon.common.config.FetchFailure;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

class ConfigSourceFailureTest {
    @ParameterizedTest
    @CsvSource({ "404,Unknown,MISSING", "400,ResourceNotFoundException,MISSING", "400,ParameterNotFound,MISSING",
            "403,Unknown,ACCESS_DENIED", "400,AccessDeniedException,ACCESS_DENIED",
            "400,KMSAccessDeniedException,ACCESS_DENIED",
            "429,ThrottlingException,TRANSIENT", "429,Unknown,TRANSIENT", "503,Unavailable,TRANSIENT",
            "400,ValidationException,OTHER" })
    void classifiesAwsResponses(int status, String code, FetchFailure expected) {
        var failure = AwsServiceException.builder().statusCode(status)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).message("unsafe-secret").build();
        assertEquals(expected, ConfigSourceFailure.classify(failure));
    }

    @Test
    void distinguishesEncodingAndTransportFailures() {
        assertEquals(FetchFailure.TRANSIENT, ConfigSourceFailure.classify(new IOException()));
        assertEquals(FetchFailure.OTHER, ConfigSourceFailure.classify(SdkClientException.create("invalid client settings")));
        assertEquals(FetchFailure.INVALID_CONTENT, ConfigSourceFailure.classify(new IllegalArgumentException("secret")));
        assertEquals(FetchFailure.INVALID_CONTENT,
                ConfigSourceFailure.classify(new UncheckedIOException(new CharacterCodingException())));
        assertEquals(FetchFailure.TRANSIENT, ConfigSourceFailure.classify(ApiCallTimeoutException.create(1)));
        assertEquals(FetchFailure.TRANSIENT,
                ConfigSourceFailure.classify(SdkClientException.create("secret", new IOException())));
        assertEquals(FetchFailure.OTHER, ConfigSourceFailure.classify(new NullPointerException("secret")));
    }

}
