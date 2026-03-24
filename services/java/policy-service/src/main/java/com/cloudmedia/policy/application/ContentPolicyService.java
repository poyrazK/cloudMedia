package com.cloudmedia.policy.application;

import com.cloudmedia.policy.api.policy.dto.ContentPolicyResponse;
import com.cloudmedia.policy.api.policy.dto.UpdateContentPolicyRequest;
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

	private final ContentPolicyRepository contentPolicyRepository;

	public ContentPolicyService(ContentPolicyRepository contentPolicyRepository) {
		this.contentPolicyRepository = contentPolicyRepository;
	}

	@Transactional
	public ContentPolicyResponse updateContentPolicy(String contentId, UpdateContentPolicyRequest request) {
		List<String> normalizedAllowList = request.geoAllowList() != null
				? normalizeCodes(request.geoAllowList())
				: null;
		List<String> normalizedBlockList = request.geoBlockList() != null
				? normalizeCodes(request.geoBlockList())
				: null;
		validateNoOverlap(normalizedAllowList, normalizedBlockList);

		ContentPolicyEntity entity = contentPolicyRepository.findById(contentId).orElseGet(() -> newEntity(contentId));
		if (request.ageRestricted() != null) {
			entity.setAgeRestricted(request.ageRestricted());
		}
		if (normalizedAllowList != null) {
			entity.setGeoAllowList(serializeCodes(normalizedAllowList));
		}
		if (normalizedBlockList != null) {
			entity.setGeoBlockList(serializeCodes(normalizedBlockList));
		}

		return toResponse(contentPolicyRepository.save(entity));
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

	private List<String> parseCodes(String serializedCodes) {
		if (serializedCodes == null || serializedCodes.isBlank()) {
			return List.of();
		}
		return List.of(serializedCodes.split(","));
	}
}
