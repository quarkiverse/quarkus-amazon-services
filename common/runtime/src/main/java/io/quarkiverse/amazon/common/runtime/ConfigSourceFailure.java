package io.quarkiverse.amazon.common.runtime;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.concurrent.TimeoutException;

import io.quarkiverse.amazon.common.config.FetchFailure;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Classifies AWS SDK exceptions without exposing their messages, which may contain values or tokens.
 * Kept in common/runtime because it depends on the SDK; common/config uses the SDK-independent FetchFailure.
 */
public final class ConfigSourceFailure {
    private ConfigSourceFailure() {
    }

    public static FetchFailure classify(Exception failure) {
        if (failure instanceof IllegalArgumentException || failure instanceof CharacterCodingException
                || failure.getCause() instanceof CharacterCodingException) {
            return FetchFailure.INVALID_CONTENT;
        }
        if (failure instanceof AwsServiceException service) {
            String code = service.awsErrorDetails() == null ? null : service.awsErrorDetails().errorCode();
            if (code == null || code.isBlank()) {
                code = service.getClass().getSimpleName();
            }
            if (service.statusCode() == 403 || "AccessDeniedException".equals(code) || "AccessDenied".equals(code)
                    || "KMSAccessDeniedException".equals(code)) {
                return FetchFailure.ACCESS_DENIED;
            }
            if (service.statusCode() == 404 || "ResourceNotFoundException".equals(code)
                    || "ParameterNotFound".equals(code)) {
                return FetchFailure.MISSING;
            }
            if (service.isThrottlingException() || service.statusCode() == 429 || service.statusCode() >= 500
                    || "ThrottlingException".equals(code) || "InternalServerException".equals(code)) {
                return FetchFailure.TRANSIENT;
            }
        }
        if (failure instanceof IOException || failure instanceof TimeoutException
                || failure instanceof ApiCallTimeoutException || failure instanceof ApiCallAttemptTimeoutException
                || failure instanceof SdkClientException client && client.retryable()
                || failure.getCause() instanceof IOException || failure.getCause() instanceof TimeoutException) {
            return FetchFailure.TRANSIENT;
        }
        return FetchFailure.OTHER;
    }

}
