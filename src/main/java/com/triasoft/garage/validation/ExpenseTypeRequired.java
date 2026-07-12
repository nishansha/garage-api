package com.triasoft.garage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ExpenseTypeRequiredValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface ExpenseTypeRequired {

    String message() default "REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
