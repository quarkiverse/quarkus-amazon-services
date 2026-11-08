package io.quarkiverse.amazon.common.runtime;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

/** Safe diagnostics for config loading; SDK and parser messages can contain configuration values or tokens. */
public enum ConfigSourceFailure {
    MISSING,
    ACCESS_DENIED,
    TRANSIENT,
    INVALID_CONTENT,
    PARTIAL_RESPONSE,
    OTHER;

    public static ConfigSourceFailure classify(Exception failure) {
        if (failure instanceof IllegalArgumentException || failure instanceof CharacterCodingException
                || failure.getCause() instanceof CharacterCodingException) {
            return INVALID_CONTENT;
        }
        if (failure instanceof AwsServiceException service) {
            String code = service.awsErrorDetails() == null ? "" : service.awsErrorDetails().errorCode();
            if (service.statusCode() == 403 || "AccessDeniedException".equals(code) || "AccessDenied".equals(code)
                    || "KMSAccessDeniedException".equals(code)) {
                return ACCESS_DENIED;
            }
            if (service.statusCode() == 404 || "ResourceNotFoundException".equals(code)
                    || "ParameterNotFound".equals(code)) {
                return MISSING;
            }
            if (service.isThrottlingException() || service.statusCode() >= 500) {
                return TRANSIENT;
            }
        }
        if (failure instanceof ApiCallTimeoutException || failure instanceof ApiCallAttemptTimeoutException
                || failure instanceof SdkClientException client && client.retryable()
                || failure.getCause() instanceof IOException || failure.getCause() instanceof TimeoutException) {
            return TRANSIENT;
        }
        return OTHER;
    }

    public void log(Logger logger, String operation) {
        if (this == TRANSIENT || this == MISSING || this == PARTIAL_RESPONSE) {
            logger.warnf("%s [%s]", operation, this);
        } else {
            logger.errorf("%s [%s]", operation, this);
        }
    }
}
