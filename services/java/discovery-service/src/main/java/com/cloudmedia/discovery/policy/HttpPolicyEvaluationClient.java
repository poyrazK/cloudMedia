package com.cloudmedia.discovery.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpPolicyEvaluationClient implements PolicyEvaluationClient {

	private final RestClient restClient;

	private final ExecutorService batchExecutor;

	public HttpPolicyEvaluationClient(RestClient.Builder restClientBuilder,
			@Value("${policy.service.base-url:http://localhost:8084}") String baseUrl) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
		requestFactory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
		this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
		this.batchExecutor = Executors
				.newFixedThreadPool(Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors())));
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

	@Override
	public Map<String, PolicyDecision> evaluateBatch(Collection<String> contentIds, String countryCode,
			Boolean ageVerified) {
		List<String> uniqueIds = contentIds.stream().filter(Objects::nonNull).distinct().toList();
		Map<String, CompletableFuture<PolicyDecision>> futures = new LinkedHashMap<>();
		for (String contentId : uniqueIds) {
			futures.put(contentId,
					CompletableFuture.supplyAsync(() -> evaluate(contentId, countryCode, ageVerified), batchExecutor));
		}

		Map<String, PolicyDecision> decisions = new LinkedHashMap<>();
		for (Map.Entry<String, CompletableFuture<PolicyDecision>> entry : futures.entrySet()) {
			try {
				decisions.put(entry.getKey(), entry.getValue().join());
			} catch (CompletionException exception) {
				Throwable cause = exception.getCause();
				if (cause instanceof PolicyEvaluationException policyEvaluationException) {
					throw policyEvaluationException;
				}
				throw new PolicyEvaluationException("Policy service batch evaluation failed", cause);
			}
		}
		return decisions;
	}

	@PreDestroy
	void shutdownExecutor() {
		batchExecutor.shutdownNow();
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
