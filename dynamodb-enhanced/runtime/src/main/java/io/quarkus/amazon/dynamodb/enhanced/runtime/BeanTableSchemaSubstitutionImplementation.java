package io.quarkus.amazon.dynamodb.enhanced.runtime;

import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * These are not GraalVM substitutions.
 *
 * They are used to replace runtime generated lambdas with method handles and are the runtime counterparts of the bytecode
 * changes made in DynamodbEnhancedProcessor.
 * Runtime generated lambdas are not supported by native-image and we have class loader problems anyway.
 */
public class BeanTableSchemaSubstitutionImplementation {
    public static <T> Supplier<T> newObjectSupplierForClass(Class<T> clazz) {
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflectConstructor(clazz.getConstructor());
            return new ConstructorWrapper<>(mh);
        } catch (IllegalAccessException | NoSuchMethodException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert Constructor to MethodHandle", ex);
        }
    }

    public static <T, R> Function<T, R> getterForProperty(
            PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
        // change back to MethodHandle after https://github.com/oracle/graal/issues/5672 is resolved
        Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod == null) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert Getter-Method to Method");
        }
        return new GetterWrapper<>(readMethod);
    }

    public static <T, U> BiConsumer<T, U> setterForProperty(
            PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
        Method writeMethod = propertyDescriptor.getWriteMethod();
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflect(writeMethod);
            return new SetterWrapper<>(mh);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert Setter-Method to MethodHandle", ex);
        }
    }

    private static class ConstructorWrapper<T> implements Supplier<T> {
        private final MethodHandle mh;

        public ConstructorWrapper(MethodHandle mh) {
            this.mh = mh;
        }

        @Override
        public T get() {
            try {
                return (T) mh.invoke();
            } catch (Exception ex) {
                throw new IllegalStateException("GraalVM Substitution: Exception invoking getter", ex);
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new Error(
                        "GraalVM Substitution: No other direct descendant of Throwable should exist", throwable);
            }
        }
    }

    private static class GetterWrapper<T, R> implements Function<T, R> {
        private final Method method;

        public GetterWrapper(Method method) {
            this.method = method;
        }

        @Override
        public R apply(T t) {
            try {
                return (R) method.invoke(t);
            } catch (Exception ex) {
                throw new IllegalStateException("GraalVM Substitution: Exception invoking getter", ex);
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new Error(
                        "GraalVM Substitution: No other direct descendant of Throwable should exist", throwable);
            }
        }
    }

    private static class SetterWrapper<T, U> implements BiConsumer<T, U> {
        private final MethodHandle mh;

        public SetterWrapper(MethodHandle mh) {
            this.mh = mh;
        }

        @Override
        public void accept(T object, U value) {
            try {
                mh.invoke(object, value);
            } catch (Exception ex) {
                throw new IllegalStateException("GraalVM Substitution: Exception invoking getter", ex);
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new Error(
                        "GraalVM Substitution: No other direct descendant of Throwable should exist", throwable);
            }
        }
    }
}