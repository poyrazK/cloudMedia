package com.cloudmedia.identity.api;

import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.service.AuthRefreshService;
import com.cloudmedia.identity.auth.service.RefreshResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthRefreshService authRefreshService;

	@MockBean
	private AuthProperties authProperties;

	@Test
	void loginReturnsNotImplementedEnvelopeWhenPayloadValid() throws Exception {
		mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_login_1").content("""
						{
						  "email": "user@example.com",
						  "password": "password123"
						}
						""")).andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.error.code").value("AUTH_NOT_IMPLEMENTED"))
				.andExpect(jsonPath("$.meta.requestId").value("req_login_1"))
				.andExpect(jsonPath("$.meta.timestamp").exists());
	}

	@Test
	void loginReturnsValidationErrorWhenEmailInvalid() throws Exception {
		mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "email": "bad-email",
				  "password": "password123"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.email").exists());
	}

	@Test
	void socialLoginReturnsNotImplementedEnvelopeWhenGooglePayloadValid() throws Exception {
		mockMvc.perform(post("/v1/auth/social-login").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_social_1").content("""
						{
						  "provider": "GOOGLE",
						  "providerToken": "token-123"
						}
						""")).andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.error.code").value("AUTH_NOT_IMPLEMENTED"))
				.andExpect(jsonPath("$.meta.requestId").value("req_social_1"));
	}

	@Test
	void refreshReturnsTokenEnvelopeWhenRefreshTokenValid() throws Exception {
		given(authRefreshService.rotateRefreshToken(anyString()))
				.willReturn(new RefreshResult("access-token", "new-refresh-token", "sess_123"));
		given(authProperties.getAccessTokenTtl()).willReturn(Duration.ofMinutes(15));
		given(authProperties.getRefreshTokenTtl()).willReturn(Duration.ofDays(30));

		mockMvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.header("X-Request-Id", "req_refresh_1").content("""
								{
								  "refreshToken": "refresh-123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
				.andExpect(jsonPath("$.data.sessionId").value("sess_123"))
				.andExpect(jsonPath("$.meta.requestId").value("req_refresh_1"));
	}

	@Test
	void refreshReturnsValidationErrorWhenRefreshTokenBlank() throws Exception {
		mockMvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "refreshToken": ""
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.refreshToken").exists());
	}

	@Test
	void logoutReturnsNotImplementedEnvelopeWhenPayloadValid() throws Exception {
		mockMvc.perform(post("/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_logout_1").content("""
						{
						  "sessionId": "sess_123",
						  "allSessions": false
						}
						""")).andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.error.code").value("AUTH_NOT_IMPLEMENTED"))
				.andExpect(jsonPath("$.meta.requestId").value("req_logout_1"));
	}
}
