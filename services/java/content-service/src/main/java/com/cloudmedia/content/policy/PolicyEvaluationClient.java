package com.cloudmedia.content.policy;

public interface PolicyEvaluationClient {

	PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified);
}
