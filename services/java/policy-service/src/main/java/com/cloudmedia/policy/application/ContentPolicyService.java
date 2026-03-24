package com.cloudmedia.policy.application;

import com.cloudmedia.policy.api.policy.dto.ContentPolicyResponse;
import com.cloudmedia.policy.api.policy.dto.ContentPolicyDecisionResponse;
import com.cloudmedia.policy.api.policy.dto.EvaluateContentPolicyRequest;
import com.cloudmedia.policy.api.policy.dto.UpdateContentPolicyRequest;
import com.cloudmedia.policy.api.policy.dto.UpdateModerationStateRequest;
import com.cloudmedia.policy.error.ApiException;
import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import com.cloudmedia.policy.persistence.repository.ContentPolicyRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentPolicyService {

	private static final int GEO_LIST_MAX_LENGTH = 512;

	private final ContentPolicyRepository contentPolicyRepository;

	public ContentPolicyService(ContentPolicyRepository contentPolicyRepository) {
		this.contentPolicyRepository = contentPolicyRepository;
	}

	@Transactional
	public ContentPolicyResponse updateContentPolicy(String contentId, UpdateContentPolicyRequest request) {
		ContentPolicyEntity entity = contentPolicyRepository.findById(contentId).orElseGet(() -> newEntity(contentId));
		List<String> normalizedAllowList = request.geoAllowList() != null
				? normalizeCodes(request.geoAllowList())
				: null;
		List<String> normalizedBlockList = request.geoBlockList() != null
				? normalizeCodes(request.geoBlockList())
				: null;
		List<String> effectiveAllowList = normalizedAllowList != null
				? normalizedAllowList
				: parseCodes(entity.getGeoAllowList());
		List<String> effectiveBlockList = normalizedBlockList != null
				? normalizedBlockList
				: parseCodes(entity.getGeoBlockList());
		validateNoOverlap(effectiveAllowList, effectiveBlockList);

		if (request.ageRestricted() != null) {
			entity.setAgeRestricted(request.ageRestricted());
		}
		if (normalizedAllowList != null) {
			entity.setGeoAllowList(serializeCodesWithGuard(normalizedAllowList));
		}
		if (normalizedBlockList != null) {
			entity.setGeoBlockList(serializeCodesWithGuard(normalizedBlockList));
		}

		return toResponse(contentPolicyRepository.save(entity));
	}

	@Transactional
	public ContentPolicyResponse updateModerationState(String contentId, UpdateModerationStateRequest request) {
		ContentPolicyEntity entity = contentPolicyRepository.findById(contentId).orElseGet(() -> newEntity(contentId));
		entity.setModerationState(request.moderationState());
		return toResponse(contentPolicyRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public ContentPolicyDecisionResponse evaluateContentPolicy(String contentId, EvaluateContentPolicyRequest request) {
		ContentPolicyEntity entity = contentPolicyRepository.findById(contentId).orElseGet(() -> newEntity(contentId));
		List<String> reasonCodes = new ArrayList<>();
		List<String> geoAllowList = parseCodes(entity.getGeoAllowList());
		List<String> geoBlockList = parseCodes(entity.getGeoBlockList());
		String countryCode = normalizeCountryCode(request.countryCode());

		if (entity.getModerationState() == ModerationState.REMOVED) {
			reasonCodes.add("MODERATION_REMOVED");
		} else if (entity.getModerationState() == ModerationState.HIDDEN) {
			reasonCodes.add("MODERATION_HIDDEN");
		}

		if (reasonCodes.isEmpty() && entity.isAgeRestricted() && !Boolean.TRUE.equals(request.ageVerified())) {
			reasonCodes.add("AGE_RESTRICTED");
		}

		if (reasonCodes.isEmpty() && countryCode != null && geoBlockList.contains(countryCode)) {
			reasonCodes.add("GEO_BLOCKED");
		}

		if (reasonCodes.isEmpty() && !geoAllowList.isEmpty()
				&& (countryCode == null || !geoAllowList.contains(countryCode))) {
			reasonCodes.add("GEO_NOT_ALLOWED");
		}

		return new ContentPolicyDecisionResponse(contentId, reasonCodes.isEmpty(), List.copyOf(reasonCodes),
				entity.getModerationState(), entity.isAgeRestricted(), geoAllowList, geoBlockList);
	}

	private ContentPolicyEntity newEntity(String contentId) {
		ContentPolicyEntity entity = new ContentPolicyEntity();
		entity.setContentId(contentId);
		entity.setModerationState(ModerationState.VISIBLE);
		entity.setGeoAllowList("");
		entity.setGeoBlockList("");
		return entity;
	}

	private ContentPolicyResponse toResponse(ContentPolicyEntity entity) {
		return new ContentPolicyResponse(entity.getContentId(), entity.isAgeRestricted(),
				parseCodes(entity.getGeoAllowList()), parseCodes(entity.getGeoBlockList()), entity.getModerationState(),
				entity.getCreatedAt(), entity.getUpdatedAt());
	}

	private List<String> normalizeCodes(List<String> codes) {
		Set<String> normalized = new LinkedHashSet<>();
		for (String code : codes) {
			normalized.add(code.toUpperCase(Locale.ROOT));
		}
		return new ArrayList<>(normalized);
	}

	private String normalizeCountryCode(String countryCode) {
		return countryCode == null || countryCode.isBlank() ? null : countryCode.toUpperCase(Locale.ROOT);
	}

	private void validateNoOverlap(List<String> allowList, List<String> blockList) {
		if (allowList == null || blockList == null) {
			return;
		}
		for (String code : allowList) {
			if (blockList.contains(code)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "POLICY_GEO_CONFLICT",
						"Geo allow and block lists must not overlap", null);
			}
		}
	}

	private String serializeCodes(List<String> codes) {
		return String.join(",", codes);
	}

	private String serializeCodesWithGuard(List<String> codes) {
		String serializedCodes = serializeCodes(codes);
		if (serializedCodes.length() > GEO_LIST_MAX_LENGTH) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "POLICY_GEO_LIST_TOO_LARGE",
					"Geo policy list exceeds maximum supported length", null);
		}
		return serializedCodes;
	}

	private List<String> parseCodes(String serializedCodes) {
		if (serializedCodes == null || serializedCodes.isBlank()) {
			return List.of();
		}
		return List.of(serializedCodes.split(","));
	}
}
