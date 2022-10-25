package com.nextlabs.common.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameter {

    String option() default "";

    String longOption() default "";

    String description();

    boolean mandatory() default false;

    boolean hasArgs() default true;

    String defaultValue() default "";
}
