package com.cloudmedia.discovery.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

	private static final Set<String> ISO_COUNTRY_CODES = Arrays.stream(Locale.getISOCountries())
			.collect(Collectors.toUnmodifiableSet());

	private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return COUNTRY_CODE_PATTERN.matcher(value).matches() && ISO_COUNTRY_CODES.contains(value);
	}
}
