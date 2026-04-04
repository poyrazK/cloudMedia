package com.cloudmedia.policy.api.policy.dto;

import jakarta.validation.constraints.Pattern;

public record EvaluateContentPolicyRequest(
		@Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "must be a 2-letter country code") String countryCode,
		Boolean ageVerified) {
}
