package com.cloudmedia.identity.auth.social;

import com.cloudmedia.identity.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FakeGoogleTokenVerifier implements GoogleTokenVerifier {

	private static final String PREFIX = "fake-google:";

	@Override
	public GoogleIdentity verify(String providerToken) {
		if (providerToken == null || !providerToken.startsWith(PREFIX)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "SOCIAL_TOKEN_INVALID", "Invalid Google provider token",
					null);
		}

		String payload = providerToken.substring(PREFIX.length());
		String[] parts = payload.split(":", 2);
		if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "SOCIAL_TOKEN_INVALID", "Invalid Google provider token",
					null);
		}

		return new GoogleIdentity(parts[0], parts[1]);
	}
}
