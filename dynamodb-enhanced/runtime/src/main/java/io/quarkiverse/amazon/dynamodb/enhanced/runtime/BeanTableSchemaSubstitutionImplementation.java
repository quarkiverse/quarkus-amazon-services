package io.quarkiverse.amazon.dynamodb.enhanced.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeGetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeSetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectGetterMethod;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticGetterMethod;

/**
 * These are not GraalVM substitutions.
 *
 * They are used to replace runtime generated lambdas with method handles and are the runtime counterparts of the bytecode
 * changes made in DynamodbEnhancedProcessor.
 * Runtime generated lambdas are not supported by native-image and we have class loader problems anyway.
 */
public class BeanTableSchemaSubstitutionImplementation {

    public static <BeanT, GetterT> ObjectGetterMethod<BeanT, GetterT> ObjectGetterMethod_create(Class<BeanT> beanClass,
            Method buildMethod, MethodHandles.Lookup lookup) {
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflect(buildMethod);
            return new FunctionWrapper<BeanT, GetterT>(mh);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert setter to MethodHandle", ex);
        }
    }

    public static <BeanT, GetterT> BeanAttributeGetter<BeanT, GetterT> BeanAttributeGetter_create(Class<BeanT> beanClass,
            Method getter, MethodHandles.Lookup lookup) {
        // change back to MethodHandle after https://github.com/oracle/graal/issues/5672 is resolved
        return new GetterWrapper<BeanT, GetterT>(getter);
    }

    public static <BeanT, SetterT> BeanAttributeSetter<BeanT, SetterT> BeanAttributeSetter_create(Class<BeanT> beanClass,
            Method setter, MethodHandles.Lookup lookup) {
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflect(setter);
            return new BiConsumerWrapper<BeanT, SetterT>(mh);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert setter to MethodHandle", ex);
        }
    }

    public static <BeanT> ObjectConstructor<BeanT> ObjectConstructor_create(Class<BeanT> beanClass,
            Constructor<BeanT> noArgsConstructor, MethodHandles.Lookup lookup) {
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflectConstructor(noArgsConstructor);
            return new SupplierWrapper<>(mh);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert noArgsConstructor to MethodHandle", ex);
        }
    }

    public static <GetterT> StaticGetterMethod<GetterT> StaticGetterMethod_create(Method buildMethod,
            MethodHandles.Lookup lookup) {
        try {
            MethodHandle mh = MethodHandles.publicLookup().unreflect(buildMethod);
            return new SupplierWrapper<>(mh);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "GraalVM Substitution: Unable to convert noArgsConstructor to MethodHandle", ex);
        }
    }

    private static class SupplierWrapper<T> implements ObjectConstructor<T>, StaticGetterMethod<T> {
        private final MethodHandle mh;

        public SupplierWrapper(MethodHandle mh) {
            this.mh = mh;
        }

        @Override
        public T get() {
            try {
                return (T) mh.invoke();
            } catch (Exception ex) {
                throw new IllegalStateException("GraalVM Substitution: Exception invoking method", ex);
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new Error(
                        "GraalVM Substitution: No other direct descendant of Throwable should exist", throwable);
            }
        }
    }

    private static class FunctionWrapper<T, R> implements ObjectGetterMethod<T, R> {
        private final MethodHandle mh;

        public FunctionWrapper(MethodHandle mh) {
            this.mh = mh;
        }

        @Override
        public R apply(T t) {
            try {
                return (R) mh.invoke(t);
            } catch (WrongMethodTypeException | ClassCastException ex) {
                throw new IllegalStateException("GraalVM Substitution: Exception invoking method", ex);
            } catch (Throwable throwable) {
                throw new Error(
                        "GraalVM Substitution: No other direct descendant of Throwable should exist", throwable);
            }
        }
    }

    private static class GetterWrapper<T, R> implements BeanAttributeGetter<T, R> {
        private final Method method;

        public GetterWrapper(Method method) {
            this.method = method;
        }

        @Override
        @SuppressWarnings("unchecked")
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

    private static class BiConsumerWrapper<T, U> implements BeanAttributeSetter<T, U> {
        private final MethodHandle mh;

        public BiConsumerWrapper(MethodHandle mh) {
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