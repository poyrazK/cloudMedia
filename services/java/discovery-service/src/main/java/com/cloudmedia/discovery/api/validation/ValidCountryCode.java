package com.cloudmedia.discovery.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CountryCodeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCountryCode {

	String message() default "must be an ISO 3166-1 alpha-2 uppercase code";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
