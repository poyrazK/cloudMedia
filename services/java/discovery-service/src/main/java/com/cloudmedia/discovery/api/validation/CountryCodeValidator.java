package com.cloudmedia.discovery.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

	private static final Set<String> ISO_COUNTRY_CODES = Arrays.stream(java.util.Locale.getISOCountries())
			.collect(Collectors.toUnmodifiableSet());

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return value.matches("^[A-Z]{2}$") && ISO_COUNTRY_CODES.contains(value);
	}
}
