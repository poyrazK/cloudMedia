package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.SocialProvider;
import com.cloudmedia.identity.events.IdentityEventEnvelope;
import com.cloudmedia.identity.events.IdentityEventPublisher;
import com.cloudmedia.identity.persistence.entity.UserCredentialEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.entity.UserStatus;
import com.cloudmedia.identity.persistence.repository.UserCredentialRepository;
import com.cloudmedia.identity.persistence.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class AuthSocialLoginServiceTest {

	@Autowired
	private AuthSocialLoginUseCase authSocialLoginUseCase;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@MockBean
	private IdentityEventPublisher identityEventPublisher;

	@Test
	void publishesUserCreatedWhenSocialLoginCreatesNewUser() {
		authSocialLoginUseCase.socialLogin(SocialProvider.GOOGLE, "fake-google:sub-new:new-user@example.com", null);

		verify(identityEventPublisher, atLeastOnce()).publish(argThat(event -> "user.created".equals(event.eventType())
				&& "new-user@example.com".equals(extractEmail(event))));
	}

	@Test
	void publishesUserUpdatedWhenLinkingExistingUser() {
		seedCredentialUser("existing-social@example.com", "password123");

		authSocialLoginUseCase.socialLogin(SocialProvider.GOOGLE,
				"fake-google:sub-existing:existing-social@example.com", null);

		verify(identityEventPublisher, atLeastOnce()).publish(argThat(event -> "user.updated".equals(event.eventType())
				&& "existing-social@example.com".equals(extractEmail(event))));
	}

	private String extractEmail(IdentityEventEnvelope event) {
		if (event.payload() instanceof com.cloudmedia.identity.events.UserCreatedPayload payload) {
			return payload.email();
		}
		if (event.payload() instanceof com.cloudmedia.identity.events.UserUpdatedPayload payload) {
			return payload.email();
		}
		return null;
	}

	private void seedCredentialUser(String email, String rawPassword) {
		UserEntity user = new UserEntity();
		user.setId(UUID.randomUUID().toString());
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now().plusMinutes(1));
		user = userRepository.saveAndFlush(user);

		UserCredentialEntity credential = new UserCredentialEntity();
		credential.setUser(user);
		credential.setPasswordHash(rawPassword);
		credential.setUpdatedAt(LocalDateTime.now());
		userCredentialRepository.saveAndFlush(credential);

		UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
		assertEquals(email, reloaded.getEmail());
	}
}
