package com.cloudmedia.discovery.policy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface PolicyEvaluationClient {

	PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified);

	default Map<String, PolicyDecision> evaluateBatch(Collection<String> contentIds, String countryCode,
			Boolean ageVerified) {
		Map<String, PolicyDecision> decisions = new LinkedHashMap<>();
		for (String contentId : contentIds) {
			if (contentId != null && !decisions.containsKey(contentId)) {
				decisions.put(contentId, evaluate(contentId, countryCode, ageVerified));
			}
		}
		return decisions;
	}
}
