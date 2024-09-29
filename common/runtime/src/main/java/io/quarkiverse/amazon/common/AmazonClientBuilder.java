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
 * Qualifier used to specify which aws client builder will be injected.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface AmazonClientBuilder {

    String value();

    @SuppressWarnings("all")
    public class AmazonClientBuilderLiteral extends AnnotationLiteral<AmazonClientBuilder> implements AmazonClientBuilder {

        private String name;

        public AmazonClientBuilderLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}