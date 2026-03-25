package com.cloudmedia.content.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpPolicyEvaluationClient implements PolicyEvaluationClient {

	private final RestClient restClient;

	public HttpPolicyEvaluationClient(RestClient.Builder restClientBuilder,
			@Value("${policy.service.base-url:http://localhost:8084}") String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	@Override
	public PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified) {
		PolicyEvaluateRequest request = new PolicyEvaluateRequest(countryCode, ageVerified);
		try {
			PolicyApiResponse response = restClient.post().uri("/v1/policy/content/{contentId}/evaluate", contentId)
					.body(request).retrieve().body(PolicyApiResponse.class);
			if (response == null || response.data() == null) {
				throw new PolicyEvaluationException("Policy service returned an empty response");
			}
			return new PolicyDecision(response.data().allowed(), response.data().reasonCodes());
		} catch (RestClientException exception) {
			throw new PolicyEvaluationException("Policy service request failed", exception);
		}
	}

	private record PolicyEvaluateRequest(@Nullable String countryCode, @Nullable Boolean ageVerified) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PolicyApiResponse(PolicyDecisionData data) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PolicyDecisionData(boolean allowed, List<String> reasonCodes) {
	}
}
