package com.cloudmedia.identity.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

	private final Counter loginSuccess;
	private final Counter loginFailure;
	private final Counter socialLoginSuccess;
	private final Counter socialLoginFailure;
	private final Counter refreshSuccess;
	private final Counter refreshReuseDetected;
	private final Counter logoutSuccess;

	public AuthMetrics(MeterRegistry meterRegistry) {
		this.loginSuccess = meterRegistry.counter("identity.login.success");
		this.loginFailure = meterRegistry.counter("identity.login.failure");
		this.socialLoginSuccess = meterRegistry.counter("identity.social_login.success");
		this.socialLoginFailure = meterRegistry.counter("identity.social_login.failure");
		this.refreshSuccess = meterRegistry.counter("identity.refresh.success");
		this.refreshReuseDetected = meterRegistry.counter("identity.refresh.reuse_detected");
		this.logoutSuccess = meterRegistry.counter("identity.logout.success");
	}

	public void onLoginSuccess() {
		loginSuccess.increment();
	}

	public void onLoginFailure() {
		loginFailure.increment();
	}

	public void onSocialLoginSuccess() {
		socialLoginSuccess.increment();
	}

	public void onSocialLoginFailure() {
		socialLoginFailure.increment();
	}

	public void onRefreshSuccess() {
		refreshSuccess.increment();
	}

	public void onRefreshReuseDetected() {
		refreshReuseDetected.increment();
	}

	public void onLogoutSuccess() {
		logoutSuccess.increment();
	}
}
