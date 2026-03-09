package com.cloudmedia.identity.api.dto;

import jakarta.validation.constraints.Size;

public record DeviceInfo(@Size(max = 64) String deviceId, @Size(max = 64) String platform,
		@Size(max = 128) String appVersion, @Size(max = 255) String userAgent, @Size(max = 64) String ipAddress) {
}
