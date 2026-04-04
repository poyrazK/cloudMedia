package com.cloudmedia.discovery.policy;

public interface PolicyEvaluationClient {

	PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified);
}
