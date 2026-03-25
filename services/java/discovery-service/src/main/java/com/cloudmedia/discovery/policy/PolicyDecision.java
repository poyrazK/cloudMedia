package com.cloudmedia.discovery.policy;

import java.util.List;

public record PolicyDecision(boolean allowed, List<String> reasonCodes) {

	public PolicyDecision {
		reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
	}
}
