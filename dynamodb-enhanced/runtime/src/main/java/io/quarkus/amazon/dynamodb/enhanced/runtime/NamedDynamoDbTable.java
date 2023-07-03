package io.quarkus.amazon.dynamodb.enhanced.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Specification of DynamoDb table to be injected.
 */
@Target({ FIELD })
@Retention(RUNTIME)
@Qualifier
@Documented
public @interface NamedDynamoDbTable {

    /**
     * @return name of the table to be injected
     */
    String value();
}
