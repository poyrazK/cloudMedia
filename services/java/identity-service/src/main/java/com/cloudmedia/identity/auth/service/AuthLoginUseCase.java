package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.DeviceInfo;

public interface AuthLoginUseCase {
	RefreshResult login(String email, String password, DeviceInfo deviceInfo);
}
