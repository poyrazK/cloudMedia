package com.cloudmedia.identity.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setup() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void teardown() {
		validatorFactory.close();
	}

	@Test
	void loginRequestRejectsInvalidEmail() {
		LoginRequest request = new LoginRequest("bad-email", "password123", null);
		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
		assertTrue(violations.stream().anyMatch(v -> "email".equals(v.getPropertyPath().toString())));
	}

	@Test
	void loginRequestRejectsShortPassword() {
		LoginRequest request = new LoginRequest("user@example.com", "short", null);
		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
		assertTrue(violations.stream().anyMatch(v -> "password".equals(v.getPropertyPath().toString())));
	}

	@Test
	void socialLoginRequestAcceptsGoogleProvider() {
		SocialLoginRequest request = new SocialLoginRequest(SocialProvider.GOOGLE, "provider-token", null);
		Set<ConstraintViolation<SocialLoginRequest>> violations = validator.validate(request);
		assertTrue(violations.isEmpty());
	}

	@Test
	void refreshRequestRejectsBlankToken() {
		RefreshRequest request = new RefreshRequest("");
		Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
		assertFalse(violations.isEmpty());
	}
}
