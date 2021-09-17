/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.runtime;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/** A serialization proxy for singleton objects */
public final class ModuleSerializationProxy implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Class<?> moduleClass;
    private static final ClassValueCompat<Object> instances = new ClassValueCompat<Object>() {
        @Override
        @SuppressWarnings("removal")  // JDK 17 deprecates AccessController
        protected Object computeValue(Class<?> type) {
            try {
                return java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> type.getField("MODULE$").get(null));
            } catch (PrivilegedActionException e) {
                return rethrowRuntime(e.getCause());
            }
        }
    };

    private static Object rethrowRuntime(Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        else throw new RuntimeException(cause);
    }

    public ModuleSerializationProxy(Class<?> moduleClass) {
        this.moduleClass = moduleClass;
    }

    @SuppressWarnings("unused")
    private Object readResolve() {
        return instances.get(moduleClass);
    }

    private static boolean checkClassValueAvailability() {
        try {
            Class.forName("java.lang.ClassValue", false, Object.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private interface ClassValueInterface<T> {
        T get(Class<?> param1Class);

        void remove(Class<?> param1Class);
    }

    private abstract static class ClassValueCompat<T> {

        private final ClassValueInterface<T> instance;

        protected ClassValueCompat() {
            instance = checkClassValueAvailability() ? new JavaClassValue() : new FallbackClassValue();
        }

        private class JavaClassValue extends ClassValue<T> implements ClassValueInterface<T> {
            @Override
            protected T computeValue(Class<?> aClass) {
                return ClassValueCompat.this.computeValue(aClass);
            }
        }

        private class FallbackClassValue implements ClassValueInterface<T> {
            @Override
            public T get(Class<?> type) {
                return ClassValueCompat.this.computeValue(type);
            }

            @Override
            public void remove(Class<?> type) {}
        }

        protected abstract T computeValue(Class<?> type);

        public T get(Class<?> type) {
            return instance.get(type);
        }

        public void remove(Class<?> type) {
            instance.remove(type);
        }

    }
}
