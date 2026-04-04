package com.cloudmedia.policy.api.policy.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record UpdateContentPolicyRequest(Boolean ageRestricted,
		List<@Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter country code") String> geoAllowList,
		List<@Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter country code") String> geoBlockList) {

	@AssertTrue(message = "At least one policy field must be provided")
	public boolean hasUpdatableField() {
		return ageRestricted != null || geoAllowList != null || geoBlockList != null;
	}
}
