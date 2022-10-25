package com.nextlabs.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class ClassUtils {

    public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        return annotatedElement.getAnnotation(annotationType);
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public static boolean isAnnotationDeclaredLocally(Class<?> clazz, Class<? extends Annotation> annotationType) {
        for (Annotation ann : clazz.getDeclaredAnnotations()) {
            if (ann.annotationType() == annotationType) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnnotationPresentLocally(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return clazz.isAnnotationPresent(annotationType) && isAnnotationDeclaredLocally(clazz, annotationType);
    }

    public static boolean isPublicStaticFinal(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }

    public static boolean isVoid(Type type) {
        return void.class.equals(type);
    }

    public static void makeAccessible(final Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    field.setAccessible(true);
                    return null;
                }
            });
        }
    }

    public static void makeAccessible(final Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    method.setAccessible(true);
                    return null;
                }
            });
        }
    }

    public static void setField(Field field, Object target, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        field.set(target, value);
    }

    private ClassUtils() {
    }
}
