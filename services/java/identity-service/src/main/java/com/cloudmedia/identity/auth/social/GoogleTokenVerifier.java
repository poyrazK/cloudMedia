package com.cloudmedia.identity.auth.social;

public interface GoogleTokenVerifier {
	GoogleIdentity verify(String providerToken);
}
