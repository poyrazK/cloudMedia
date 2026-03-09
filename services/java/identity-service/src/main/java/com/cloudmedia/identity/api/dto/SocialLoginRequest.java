package com.cloudmedia.identity.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(@NotNull SocialProvider provider, @NotBlank String providerToken,
		@Valid DeviceInfo deviceInfo) {
}
