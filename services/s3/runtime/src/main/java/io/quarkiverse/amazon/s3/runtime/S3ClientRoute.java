package io.quarkiverse.amazon.s3.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/** Internal qualifier making S3 client name and implementation an exact CDI match. */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface S3ClientRoute {
    String value();

    boolean crt();

    final class Literal extends AnnotationLiteral<S3ClientRoute> implements S3ClientRoute {
        private final String name;
        private final boolean crt;

        public Literal(String name, boolean crt) {
            this.name = name;
            this.crt = crt;
        }

        public String value() {
            return name;
        }

        public boolean crt() {
            return crt;
        }
    }
}
