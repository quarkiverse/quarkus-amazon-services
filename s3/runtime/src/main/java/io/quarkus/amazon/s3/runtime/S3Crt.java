package io.quarkus.amazon.s3.runtime;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Specification of AWS CRT-based S3 client to be injected.
 */
@Retention(RUNTIME)
@Qualifier
@Documented
public @interface S3Crt {

    public static final class Literal extends AnnotationLiteral<S3Crt> implements S3Crt {
        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;
    }
}
