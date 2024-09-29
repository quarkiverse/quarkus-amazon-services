package io.quarkiverse.amazon.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to specify which aws client will be injected.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface AmazonClient {

    String value();

    @SuppressWarnings("all")
    public class AmazonClientLiteral extends AnnotationLiteral<AmazonClient> implements AmazonClient {

        private String name;

        public AmazonClientLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}