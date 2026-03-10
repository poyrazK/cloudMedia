package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.DeviceInfo;
import com.cloudmedia.identity.api.dto.SocialProvider;

public interface AuthSocialLoginUseCase {
	RefreshResult socialLogin(SocialProvider provider, String providerToken, DeviceInfo deviceInfo);
}
